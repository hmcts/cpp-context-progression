# Add `progression.query.group-cases` query

## Problem

Consumers that need a quick summary of a case group — the lead/master case plus
every member case, in a flat, display-ready shape (URN, defendant name, DoB,
address) — currently have no single query that returns this.

The two existing group queries don't fit:

- `progression.query.group-member-cases` returns only non-master member cases
  (`isGroupMember && !isGroupMaster`), excluding the lead case, and each item is
  the full heavyweight `ProsecutionCase` payload (nested
  `prosecutionCaseIdentifier`, `defendants[].personDefendant.personDetails`,
  `caseMarkers`, `civilFees`, `statementOfFacts`, etc.) — not a flat summary.
- `progression.query.mastercase-details` returns only the master case, again as
  a full `ProsecutionCase`.

A consumer that wants "the whole group, lead case highlighted, in a flat shape"
today has to call both queries and flatten the result client-side.

## Scope

Backend only, in `cpp-context-progression`. A new read-only query,
`progression.query.group-cases`, added as a third `(mapping)` on the existing
`GET /prosecutioncases/group/{groupId}` RAML resource, alongside
`group-member-cases` and `mastercase-details`.

Response shape:

```json
{
  "groupId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "leadCase": {
    "caseId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "caseUrn": "string",
    "defendantName": "string",
    "defendantDoB": "2026-10-30",
    "address": { "address1": "string", "address2": "string", "address3": "string", "address4": "string", "address5": "string", "postcode": "KE8 1PG" }
  },
  "cases": [
    { "caseId": "...", "caseUrn": "...", "defendantName": "...", "defendantDoB": "...", "address": { "...": "..." } }
  ]
}
```

`cases[]` contains **every** case in the group, including the lead case
(unlike `group-member-cases`, which excludes it).

## Non-goals

- No change to `group-member-cases` or `mastercase-details` — both are kept
  as-is for existing consumers.
- No new viewstore table, no new event subscriptions. The group concept
  remains what it is today: a `group_id` column plus `isGroupMaster` /
  `isGroupMember` flags embedded in each `prosecution_case` row's JSON
  payload.
- No group-level/aggregate fields (prosecuting authority, hearing
  location/date, offence type, result intent, case count, created date).
  These were in an earlier draft of this request but were dropped — they
  belong to other bounded contexts (hearing, resulting) that
  `prosecution_case` does not currently carry, and pulling them in would
  require new event subscriptions and a dedicated group projection. Out of
  scope for this change.
- No multi-defendant selection logic. This query always takes
  `defendants[0]` for `defendantName`/`defendantDoB`/`address` — there is no
  "primary defendant" concept on `ProsecutionCase.defendants[]` anywhere in
  this codebase today (confirmed: only `Defendant.masterDefendantId`, used for
  case merge/split linkage, and a separate `MasterDefendant` type scoped to
  court applications — neither applies here).

## Approach

1. Add a third `(mapping)` entry to the `/prosecutioncases/group/{groupId}`
   GET resource in `progression-query-api.raml`, plus the example/schema JSON
   pair, mirroring the existing two mappings.
2. Add `@Handles("progression.query.group-cases")` to `ProsecutionCaseQueryApi`
   (pass-through, same style as the two existing handlers on this resource).
3. Add `getGroupCases()` to `ProsecutionCaseQuery` (`QUERY_VIEW`), reusing
   `prosecutionCaseRepository.findByGroupId(groupId)` — the same repository
   call `group-member-cases` and `mastercase-details` already use, just with
   no post-fetch filter removing the lead case.
4. Add a new `GroupCaseConverter` (plain, non-DI class, following the
   `CaseLsmInfoQuery` + `CaseLsmInfoConverter` pairing — the established
   pattern in this codebase for flat per-case query responses; no MapStruct is
   used anywhere here) to flatten each `ProsecutionCase` row to
   `{ caseId, caseUrn, defendantName, defendantDoB, address }` and to pick out
   the `leadCase` via the existing `isGroupMaster` filter style already used
   privately in `ProsecutionCaseQuery.getMasterProsecutionCase`.
5. Add an access-control Drools rule for `progression.query.group-cases`,
   reusing the same role list as `group-member-cases`.

## Field mapping rules

| Field | Source | Rule |
|---|---|---|
| `caseId` | `ProsecutionCase.getId()` | direct |
| `caseUrn` | `ProsecutionCase.getProsecutionCaseIdentifier()` | `caseURN` if present, else `prosecutionAuthorityReference` — same precedence as `CaseLsmInfoQuery.extractCaseUrn` |
| `defendantName` | `defendants[0]` | `HearingAtAGlanceService.getDefendantName(personDefendant, legalEntityDefendant)` (reused, not reimplemented) |
| `defendantDoB` | `defendants[0].getPersonDefendant().getPersonDetails().getDateOfBirth()` | direct |
| `address` | `defendants[0].getPersonDefendant().getPersonDetails().getAddress()` | pass-through — the generated `Address` type is already `address1`-`address5` + `postcode`, no flattening needed |
| `leadCase` | row where `isGroupMaster() == true` within the `findByGroupId` result | same filter `ProsecutionCaseQuery.getMasterProsecutionCase` already uses privately |
| `cases[]` | every row in the `findByGroupId` result, including the lead case | — |

If a case has multiple defendants, only `defendants[0]` is used — see
Non-goals.

## Spec

[group-cases](../../specs/group-cases/spec.md)

## Dependencies

None. No downstream UI change is tracked as part of this proposal.
