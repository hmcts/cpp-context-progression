# Design — Default hearing type for standalone applications

**Tickets:** CHD-2305 (PCB → *Pre-charge bail*), CHD-2306 (WOFD → *Warrant of Further Detention*)
**Contexts:** `cpp-context-users-groups` (config + query), `cpp-context-progression` (validation), CPP UI

---

## 1. Problem

When a user creates a standalone application, they must manually pick a *hearing type*. Wrong
choices route the application to the wrong hearing and can expose it on the list / media register.
For specific application types the hearing type is fixed, so it should be **defaulted and locked**.

Mappings required:

| Application type | Default hearing type |
|---|---|
| Application for an initial extension of pre-charge bail period | Pre-charge bail |
| Application for the subsequent extension of pre-charge bail period | Pre-charge bail |
| Application to withhold sensitive information during pre-charge bail application | Pre-charge bail |
| Application for a warrant for further detention | Warrant of Further Detention |
| Application for an extension of warrant for further detention | Warrant of Further Detention |

---

## 2. Solution overview

Model each mapping as a **permission record** in users-groups `cpp_permission`, then have both the UI
and the progression backend read it through the existing permissions query.

Record shape (one row per application type):

| Column | Value |
|---|---|
| `object` | `HearingType` |
| `source` | applicationType id |
| `target` | hearingType id (the default) |
| `action` | `Locked` |
| `active` | `true` |

**UI** — on application-type selection, query
`usersgroups.permissions` (`application/vnd.usersgroups.permissions+json`) with
`object=HearingType&source=<applicationTypeId>`.
- `target` → pre-select that hearing type.
- `action=Locked` → disable/lock the field.
- No record → current manual behaviour (no default, editable).

**Backend (progression)** — validate on application creation so the rule can't be bypassed by a
direct API call. In the `initiate-court-proceedings-for-application` command API, call the same
permissions query and reject if the submitted hearing type contradicts a `Locked` mapping.

```
UI ──select applicationType──► usersgroups.permissions (object=HearingType, source=appTypeId)
                                     │ target=hearingTypeId, action=Locked
   ◄──default + lock hearingType─────┘

UI ──initiate-court-proceedings-for-application──► progression command API
                                                     │ re-query usersgroups.permissions
                                                     │ assert submitted hearingType == target
                                                     ▼ pass → send command / fail → 400
```

---

## 3. Backend validation detail

- **Seam:** `InitiateCourtApplicationProceedingsCommandApi`
  (`@Handles("progression.initiate-court-proceedings-for-application")`), before `sender.send(...)`.
  This class already runs authorisation + input validation and already calls users-groups via the
  injected `Requester` — including `usersgroups.permissions` — so this is an incremental addition,
  not a new integration.
- **Inputs:** `courtApplication.type.id` (applicationType), `courtHearing.hearingType.id`.
- **Rule:** query `usersgroups.permissions` with `object=HearingType`, `source=applicationType.id`.
  If an `active`, `Locked` record exists and `courtHearing.hearingType.id != target` →
  throw `BadRequestException`. No record → no constraint.
- **Applies to standalone applications** (`linkType=STANDALONE`).

---

## 4. Key decisions / corrections to the original approach

1. **`cpp_permission` is event-sourced — do not seed by direct SQL/Liquibase insert.**
   The table is a CQRS read projection, populated **only** via users-groups commands → events →
   event listeners (`create-permission-with-details` → `permission-created`). Direct DB/Liquibase
   inserts would be out-of-band and would not survive a projection rebuild. The five mapping rows
   must be created through the command path (one-off admin/seed script hitting the users-groups
   command API), not a changelog `<insert>`.

2. **`active` is not a query parameter.** `GET /permissions` filters on `object`, `action`,
   `source`, `target` only. Query by `object`+`source`, then read `active` and `action` from the
   response and honour only `active=true` rows.

3. **UUID typing.** `source`/`target` are UUIDs in the table but exposed as JSON `string`; the PK
   is `id` in the DB but `permissionId` in the response. Keep applicationType/hearingType ids as
   canonical UUIDs.

4. **Semantic fit (accept with eyes open).** `cpp_permission` is an access-control table; using it
   for application→hearing default *configuration* is a mild overload. It's justified here because
   the `Locked` action + `object/source/target` triple map cleanly onto the need and avoid a new
   store/schema. Worth a one-line ADR to record the choice.

---

## 5. Impact & effort

- **users-groups:** no code change — reuse existing command + query. New reference data (5 rows)
  created via the command API. Confirm the seeding mechanism / environment promotion for these rows.
- **progression:** one validation addition in `InitiateCourtApplicationProceedingsCommandApi` +
  integration test (per HMCTS rule: ≥1 IT for the changed command entry point).
- **UI:** query on application-type change; pre-select + lock the hearing-type field; graceful
  fallback when no mapping.

## 6. Open questions

- How are the mapping rows seeded and promoted across environments (script vs. migration-via-command)?
- Are the five applicationType ids and the two hearingType ids confirmed/stable across environments?
- Behaviour if a mapping exists but is `active=false` — treat as no default (assumed yes).
- Does the UI need the display *description* of the hearing type, or only the id (id → resolve name elsewhere)?
