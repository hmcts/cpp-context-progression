# CHD-2474: Reset proceedingsConcluded and caseStatus after hearing amendment removes judicial results

## Problem

When a hearing is amended and a judicial result is removed from an offence, the system
fails to recalculate `proceedingsConcluded` and `caseStatus`. As a result:

- The affected offence keeps `proceedingsConcluded: true` even though it has no result.
- Its parent defendant keeps `proceedingsConcluded: true`.
- The prosecution case keeps `caseStatus: INACTIVE`.

The UI consumes these stale values from `progression.query.prosecutioncase` and displays
every offence as "INACTIVE", breaking the offence-selection screen in application creation
(where offences are filtered by active/inactive status via `offence_active_order=OFFENCE`).

## Jira

[CHD-2474](https://tools.hmcts.net/jira/browse/CHD-2474)
Caused by: [CHD-1929](https://tools.hmcts.net/jira/browse/CHD-1929)

## Root Cause

There were **two bugs** that together prevented `proceedingsConcluded` from being reset.

### Bug 1 — `HearingAggregate.processHearingResults()` (event not emitted)

`prosecution-cases-resulted-v2` was only emitted when the collected `judicialResults`
list was non-empty:

```java
// BEFORE (buggy):
if (isNotEmpty(hearing.getProsecutionCases()) && isNotEmpty(judicialResults)) {
    streamBuilder.add(createProsecutionCasesResultedV2Event(...));
}
```

When all results are deleted, `judicialResults` is empty, so the event is never emitted.
The entire downstream chain — `HearingResultedEventProcessor` → `ProgressionService.updateCase()`
→ `CaseAggregate.updateCase()` — is never reached. Both `proceedingsConcluded` and
`caseStatus` remain stale.

### Bug 2 — `CaseAggregate.updateCurrentOffencesWithProceedingsConcludedStatus()` (wrong value computed)

Even if Bug 1 were fixed alone (event emitted), `CaseAggregate` would still compute the
wrong answer. Offence-level `proceedingsConcluded` is computed in
`updateCurrentOffencesWithProceedingsConcludedStatus()`. For a re-share where the result
was deleted, the offence is present with `judicialResults = null/[]`, but `hasNewAmendment()`
returns `false` (no results → no `isNewAmendment` flag). Without the fix the code falls
through to `isOffenceConcluded = isOffencePreviouslyConcluded` (= `true`), making the
deletion invisible:

```java
if (hasNewAmendment(offence)) {
    isOffenceConcluded = isConcluded(offence);
} else {
    isOffenceConcluded = isOffencePreviouslyConcluded(defendantId, offence.getId()); // ← never resets — BUG
}
```

## Scope

Two targeted fixes: one line in `HearingAggregate.java`, one new branch in
`CaseAggregate.java`. No new events, no new commands, no new components required.

## Non-Goals

- No change to the event chain structure or component wiring.
- No BDF / data fix for existing stale production data (separate 3rd-line concern).
- No changes to query-side schemas or response shapes.

## Secondary Risk (out of scope for this change)

`HearingResultEventListener.getUpdatedJudicialResults()` (lines 507–511) merges results
from different hearing days. For cross-day amendments (result entered Day 1, deleted
Day 2), the Day 1 result is preserved in the merge, causing `isConcluded()` to return
`true` before the aggregate is even reached. This is a separate but related bug to
investigate after this fix is confirmed.

## Approach

1. **`HearingAggregate.processHearingResults()`** — remove the `isNotEmpty(judicialResults)`
   guard so `prosecution-cases-resulted-v2` is always emitted when prosecution cases are
   present, regardless of whether any judicial results exist.

2. **`CaseAggregate.updateCurrentOffencesWithProceedingsConcludedStatus()`** — extracted
   from the old inline logic; adds a condition that detects when a previously concluded
   offence has **empty/absent `judicialResults`** (result deleted) and resets
   `proceedingsConcluded` to `false`. `checkIfDefendantProceedingsConcluded()` is
   simplified to read the pre-computed flags.

## Spec

[hearing-amendment-proceedings-concluded](../../specs/hearing-amendment-proceedings-concluded/spec.md)

## Dependencies

No downstream changes required. The corrected `proceedingsConcluded` and `caseStatus`
values propagate through the existing `hearing-resulted-case-updated` event chain to the
viewstore and the public event.
