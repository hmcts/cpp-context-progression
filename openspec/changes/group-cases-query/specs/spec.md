# Spec: Group Cases

## Capability

Given a case group ID, return a flat, display-ready summary of the lead
(master) case and every case in the group — for consumers that need to show
"this case is part of a group of N" without calling multiple queries or
parsing the full `ProsecutionCase` payload.

**Query**: `progression.query.group-cases`
**Response type**: `application/vnd.progression.query.group-cases+json`
**Resource**: `GET /prosecutioncases/group/{groupId}` (third `(mapping)` on
the resource already shared with `group-member-cases` and
`mastercase-details`)

---

## Data Lineage

```
PostgreSQL view store
  prosecution_case.group_id              (uuid)
  prosecution_case.payload                (jsonb — embeds isGroupMaster,
                                            isGroupMember, prosecutionCaseIdentifier,
                                            defendants[], etc.)
    ↓ loaded into domain model
  uk.gov.justice.core.courts.ProsecutionCase
    .getIsGroupMaster()        → Boolean
    .getProsecutionCaseIdentifier().getCaseURN() / .getProsecutionAuthorityReference()
    .getDefendants()[0].getPersonDefendant().getPersonDetails()
        .getFirstName() / .getMiddleName() / .getLastName()
        .getDateOfBirth()
        .getAddress()          → already flat (address1-5, postcode)
      ↓ flattened by GroupCaseConverter
  { groupId, leadCase: {caseSummary}, cases: [{caseSummary}, ...] }
```

There is no dedicated `Group` entity or table. Group membership and
master/member status are derived entirely from the `group_id` column and the
`isGroupMaster`/`isGroupMember` flags already embedded in each
`prosecution_case` row's payload — this spec does not change that model, only
adds a new read projection over it.

---

## Field Contract

| Field | Type | Nullable | Description |
|---|---|---|---|
| `groupId` | UUID string | No | The group ID requested |
| `leadCase` | object (`caseSummary`) | Yes | The case where `isGroupMaster == true`. Absent if no such case exists in the group (including an empty/unknown group). |
| `cases` | array of `caseSummary` | No (may be empty) | **Every** case in the group, including the lead case. |

### `caseSummary` object

| Field | Type | Nullable | Description |
|---|---|---|---|
| `caseId` | UUID string | No | `ProsecutionCase.id` |
| `caseUrn` | string | Yes | `caseURN` if present on `prosecutionCaseIdentifier`, else `prosecutionAuthorityReference` |
| `defendantName` | string | Yes | Name of `defendants[0]` only — see rule below |
| `defendantDoB` | date (yyyy-MM-dd) | Yes | Date of birth of `defendants[0]` |
| `address` | object (`address1`-`address5`, `postcode`) | Yes | Address of `defendants[0]` |

### Rules

- `cases[]` MUST include the lead/master case. This distinguishes
  `group-cases` from `group-member-cases`, which excludes it.
- `caseUrn` precedence: `caseURN` wins when present and non-null; otherwise
  `prosecutionAuthorityReference` is used. Mirrors the existing precedence in
  `CaseLsmInfoQuery.extractCaseUrn` — the two fields are never concatenated.
- When a case has more than one defendant, only `defendants[0]` is used for
  `defendantName`/`defendantDoB`/`address`. There is no "primary defendant"
  selection rule beyond first-in-list — this codebase has no other concept
  (e.g. a `masterDefendantId`-style flag) that applies to this query.
- `leadCase` is omitted (not sent as `null`) when no case in the group has
  `isGroupMaster == true`.
- If `groupId` matches no cases, the response is
  `{ "groupId": "<id>", "cases": [] }` with no `leadCase`. No 404 is raised —
  consistent with existing `group-member-cases`/`mastercase-details` behaviour
  on an unknown/empty group.

---

## Acceptance Criteria

### AC1 — Group with lead case and members
**Given** a group with one master case and two member cases
**When** `progression.query.group-cases` is called with that `groupId`
**Then** `cases` contains all 3 cases (lead + both members)
**And** `leadCase` matches the master case's flattened summary

### AC2 — `caseUrn` precedence
**Given** a case whose `prosecutionCaseIdentifier` has both `caseURN` and
`prosecutionAuthorityReference` populated
**When** that case is flattened into a `caseSummary`
**Then** `caseUrn` equals the `caseURN` value, not `prosecutionAuthorityReference`

### AC3 — Multiple defendants
**Given** a case with two defendants
**When** that case is flattened into a `caseSummary`
**Then** `defendantName`, `defendantDoB`, and `address` reflect only the first
defendant in the list (`defendants[0]`)

### AC4 — Empty/unknown group
**Given** a `groupId` with no matching `prosecution_case` rows
**When** `progression.query.group-cases` is called
**Then** the response is `{ "groupId": "<id>", "cases": [] }`
**And** `leadCase` is absent (not present as `null`)
**And** no error/404 is raised

### AC5 — No master case in group (data-integrity edge case)
**Given** a group where no row has `isGroupMaster == true`
**When** `progression.query.group-cases` is called
**Then** `cases` still contains all cases in the group
**And** `leadCase` is absent

---

## Related Changes

- [group-cases-query: Add progression.query.group-cases](../changes/group-cases-query/proposal.md) — the change that implements this spec
- Sibling, unmodified queries: `progression.query.group-member-cases` (member
  cases only, excludes lead, full `ProsecutionCase` payload),
  `progression.query.mastercase-details` (lead case only, full
  `ProsecutionCase` payload)
