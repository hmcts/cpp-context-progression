# Spec: Move Group Cases

Status: **Proposed.** Several rules and acceptance criteria below are marked
provisional pending the Open Questions in `../proposal.md` — this spec
captures what's settled and flags what isn't, rather than asserting resolved
behaviour that hasn't actually been agreed.

## Capability

Given an existing civil case group and a subset of its cases, split that
subset into a new, independent group — reassigning group membership and
master/lead status for the moved cases, without creating any new
`ProsecutionCase`.

**Command**: `progression.move-group-cases`
**Request type**: `application/vnd.progression.move-group-cases+json`
**Resource**: `POST /prosecutioncases` (new resource)
**Permissions**: System User
**Response**: `200 OK` / `4xx` / `5xx`

---

## Command Flow

```
case-management
  │ POST /prosecutioncases  (application/vnd.progression.move-group-cases+json)
  ▼
MoveGroupCasesHandler
  ├─ OLD GroupCaseAggregate(currentGroup.groupId)
  │     memberCases -= newGroup.caseIds
  │     groupMaster  = currentGroup.newLeadCaseId   (if given)
  │
  ├─ NEW GroupCaseAggregate(newGroup.groupId)        [created]
  │     memberCases  = newGroup.caseIds
  │     groupMaster  = newGroup.leadCaseId
  │
  ├─ for each moved case: CaseAggregate — group association updated
  │     (isGroupMaster / isGroupMember / groupId → newGroup.groupId)
  │
  ├─ civil fees resolved for the new group                        [decided]
  └─ new hearing created/listed for the new group,
     mirroring current session info, immediately (no wait for the
     next real listing event)                                     [decided]
```

No new viewstore table. Group membership stays a `group_id` column plus
`isGroupMaster`/`isGroupMember` flags embedded in each `prosecution_case`
row's JSON payload — same model `group-cases-query` documents. This spec
does not change that model, only adds a new way of mutating it.

---

## Field Contract

### Request: `currentGroup`

| Field | Type | Required | Description |
|---|---|---|---|
| `groupId` | UUID | Yes | The group cases are being moved out of |
| `newLeadCaseId` | UUID | Conditional | Required if the current lead case is among the cases being moved; must not be supplied otherwise (exact validation: see Rules) |

### Request: `newGroup`

| Field | Type | Required | Description |
|---|---|---|---|
| `groupId` | UUID | Yes | The new group being created |
| `caseIds` | array of UUID | Yes | Cases being moved into the new group |
| `leadCaseId` | UUID | Yes | Must be one of `caseIds` |

---

## Rules

- `newGroup.leadCaseId` MUST be present in `newGroup.caseIds` — otherwise
  `4xx`.
- `currentGroup.newLeadCaseId` MUST be present if the old group's current
  master case is among `newGroup.caseIds`, and MUST be absent otherwise —
  otherwise `4xx`.
- The move MUST NOT leave `currentGroup` with zero members. If every
  current member of `currentGroup` is in `newGroup.caseIds`, the command is
  rejected rather than dissolving the old group — mirroring the existing
  floor of "≥1 member" enforced for single-case removal.
- The moved cases are not re-created. This command only changes group
  association — on the `GroupCaseAggregate` (old and new) and on each moved
  case's own `CaseAggregate` — never `ProsecutionCaseCreated` or equivalent.
- If `newGroup.groupId` already has stream history: if its current
  `memberCases`/`groupMaster` already exactly match what this request would
  produce, the request is treated as an idempotent retry (succeeds, raises
  no new events); otherwise it is a genuine collision and is rejected
  `4xx`. **Exact rejection behaviour is provisional, see Open Questions** —
  this distinction itself is a recommendation, not yet confirmed with the
  team.
- All cases within the new group must end up sharing identical civil fee
  data (`feeId`s), consistent with the existing "all group members share
  identical fee data" rule enforced at group creation
  (`initiate-court-proceedings-for-group-cases`, DD-35774 AC-4). **Decided**
  sourcing mechanism: each fee type is cloned exactly once at the point of
  the split, sourced from the new lead case's current civil fees, and every
  moved case references that one new shared `feeId` set — never cloned per
  case, never left pointing at the old group's shared fees.
- Every moved case's hearing association MUST be correct immediately after
  the command completes — **decided**: not eventually consistent. Each
  moved case's `case_defendant_hearing` record must point at the new
  group's hearing by the time the command returns, not only after a
  subsequent real listing-status update arrives.

---

## Acceptance Criteria

### AC1 — Basic split, lead case stays behind
**Given** group G1 has members {A (master), B, C}
**And** the move request specifies `currentGroup.groupId = G1` with no
`newLeadCaseId`, and `newGroup = {groupId: G2, caseIds: [B, C], leadCaseId: B}`
**When** `progression.move-group-cases` is processed
**Then** G1's membership becomes {A (still master)}
**And** G2 is created with membership {B (master), C (member)}
**And** no new `ProsecutionCase` is created for A, B, or C

### AC2 — Current lead is moved, replacement supplied
**Given** group G1 has members {A (master), B, C}
**And** the move request specifies `currentGroup = {groupId: G1,
newLeadCaseId: B}` (B is one of the moved cases — contrived for this AC to
exercise reassignment; see note) and `newGroup = {groupId: G2, caseIds: [A],
leadCaseId: A}`, i.e. A is moved and B stays
**When** `progression.move-group-cases` is processed
**Then** G1's new master is B
**And** G2 is created with A as master

*(Note: this AC exists to prove old-group master reassignment works when the
master is the one moved out — the exact case/group assignment used for the
real test fixture is an implementation detail, not fixed by this AC.)*

### AC3 — Validation: lead case not in moved set
**Given** a move request where `newGroup.leadCaseId` is not present in
`newGroup.caseIds`
**When** `progression.move-group-cases` is processed
**Then** the response is `4xx`
**And** neither group's membership is changed

### AC4 — Validation: missing required `newLeadCaseId`
**Given** a move request where the current group's master case is among the
moved cases, but `currentGroup.newLeadCaseId` is absent
**When** `progression.move-group-cases` is processed
**Then** the response is `4xx`
**And** neither group's membership is changed

### AC5 — Civil fees are shared once across the new group, not per case
**Given** group G1 has members {A (master), B, C}, all currently sharing
civil fee `feeId F1`
**And** the move request moves B and C into new group G2 with B as lead
**When** `progression.move-group-cases` is processed
**Then** B and C both reference the same new `feeId` (`F1'`), cloned once
from B's (the new lead's) civil fees at the point of the split
**And** A (who stayed in G1) continues to reference the original `feeId F1`,
unchanged
**And** B and C do **not** each get their own independently-cloned `feeId`

### AC6 — Rejected when the move would empty the old group
**Given** group G1 has members {A (master), B, C}
**And** the move request's `newGroup.caseIds` is `[A, B, C]` — every current
member of G1
**When** `progression.move-group-cases` is processed
**Then** the response is `4xx`
**And** G1's membership is unchanged
**And** no `GroupCreatedFromMovedCases` event is raised for `newGroup.groupId`

### AC7 — Moved cases' hearing association is correct immediately, not eventually
**Given** group G1 has members {A (master), B, C}, currently listed under
hearing H1
**And** the move request moves B and C into new group G2 with B as lead
**When** `progression.move-group-cases` is processed
**Then** a new hearing H2 exists, seeded with H1's current session info
(court centre, room, hearing day(s))
**And** B's and C's `case_defendant_hearing` records point at H2
immediately after the command completes
**And** this holds without any subsequent `update-defendant-listing-status`
event having been received from the listing context

### AC8 — Provisional: retried request against the same `newGroup.groupId` succeeds (not yet confirmed)
**Given** a move request has already succeeded, creating G2 with
`memberCases = {B, C}` and `groupMaster = B`
**And** the identical request is submitted again (same `currentGroup`,
same `newGroup`)
**When** `progression.move-group-cases` is processed the second time
**Then** the response is success, not `4xx`
**And** no new `GroupCreatedFromMovedCases` event is raised for G2
**And** G1's and G2's membership are unchanged from the first, successful
attempt

### AC9 — Provisional: colliding `newGroup.groupId` with different content is rejected (not yet confirmed)
**Given** `newGroup.groupId` already exists as a live group G2 with
`memberCases = {X, Y}` — unrelated to this request's `newGroup.caseIds`
**And** a move request specifies `newGroup = {groupId: G2, caseIds: [B, C],
leadCaseId: B}`
**When** `progression.move-group-cases` is processed
**Then** the response is `4xx`
**And** G2's membership remains `{X, Y}`, unchanged — B and C are not
merged into it

---

## Open Questions / Provisional Behaviour

Not asserted as resolved by this spec — see `../proposal.md` for full
context on each:

1. ~~New-group genesis mechanism~~ — **DECIDED**: a dedicated new event,
   `GroupCreatedFromMovedCases`, not a reuse of `CourtProceedingsInitiated`.
   Affects implementation, not the command's external contract — see
   proposal.md Open Question 1 and design.md for the concrete shape.
2. ~~Civil fee sourcing for the new group~~ — **DECIDED**, see the Rules
   section and AC5 above.
3. ~~Whether the old group can be left with zero members~~ — **DECIDED:
   no.** See the new Rule and AC6 below.
4. ~~Timing of hearing-association consistency~~ — **DECIDED: immediate**,
   not eventual. See the new Rule and AC7 below.
5. ~~Resolving the "current" hearing to mirror~~ — **RESOLVED**, no API
   definition change needed; see proposal.md Open Question 5.
6. **Audit requirement** — **DECIDED**: `Audit Required: No` stands, no
   additional audit mechanism needed. **"Existing `newGroup.groupId`"
   rejection behaviour** — still open, with a refined recommendation (see
   proposal.md Open Question 6): distinguish an idempotent retry (identical
   `memberCases`/`groupMaster` already present — succeed, no-op) from a
   genuine collision (different content — reject `4xx`), rather than a
   blanket reject, because this command's `Permissions: System User` +
   this RAML's universal `202` fire-and-forget convention (this command's
   own `200` is itself flagged as worth raising with the API's author)
   makes caller retries routine. See AC8/AC9 below for the provisional
   shape of this behaviour.

---

## Related Changes

- [move-group-cases: Add progression.move-group-cases](../proposal.md) — the
  change that implements this spec
- [group-cases-query](../../group-cases-query/proposal.md) — read-only query
  over the same group model, confirms the "no dedicated Group table" model
  this spec relies on
- Sibling, unmodified commands: `progression.initiate-court-proceedings-for-group-cases`
  (group creation from new cases), `progression.remove-case-from-group-cases`
  (single-case removal with fee forking)
