# Design: CHD-2474 — Amendment proceedings-concluded reset

## Event Chain (confirmed working — not changed)

When results are re-shared after a hearing amendment, the following events fire in sequence:

```
progression.event.prosecutionCase-defendant-listing-status-changed-v2
progression.event.hearing-resulted
progression.event.prosecution-cases-resulted-v2
  └─▶ HearingResultedEventProcessor.handleProsecutionCasesResultedV2()
        └─▶ ProgressionService.updateCase()
              └─▶ progression.command.hearing-resulted-update-case
                    └─▶ UpdateCaseHandler
                          └─▶ CaseAggregate.updateCase()
                                └─▶ progression.event.hearing-resulted-case-updated
                                      └─▶ ProsecutionCaseDefendantUpdatedEventListener
                                            └─▶ viewstore updated
progression.event.defendant-trial-record-sheet-requested
```

Two bugs exist in this chain (both fixed):

1. **`HearingAggregate`** — `prosecution-cases-resulted-v2` was not emitted when
   `judicialResults` was empty, so when a result is deleted the entire chain from
   `HearingResultedEventProcessor` onward was never triggered.
2. **`CaseAggregate`** — `checkIfDefendantProceedingsConcluded()` hardcoded `TRUE` for
   previously concluded offences without checking if their result was deleted.

---

## Data Flow into `CaseAggregate.updateCase()`

When `prosecution-cases-resulted-v2` is processed, `ProgressionService.updateCase()`
dispatches the `hearing-resulted-update-case` command carrying:

| Field | Value after amendment |
|-------|-----------------------|
| `prosecutionCase` | Full prosecution case from hearing event — includes ALL offences. The amended offence is present but with **empty `judicialResults`** (result was deleted). |
| `defendantJudicialResults` | Defendant-level judicial results from the hearing — does NOT include a result for the amended offence. |

`UpdateCaseHandler` loads the `CaseAggregate` by case ID, replays its event stream
(restoring internal state including `defendantOffencesResultedOffenceLevel` map), and
calls `updateCase()` with the above data.

---

## Root Cause: `updateCurrentOffencesWithProceedingsConcludedStatus()`

**File:** `progression-domain/progression-domain-aggregate/src/main/java/uk/gov/moj/cpp/progression/aggregate/CaseAggregate.java`

The offence-level `proceedingsConcluded` computation lives in
`updateCurrentOffencesWithProceedingsConcludedStatus()`, called from `getUpdatedDefendant()`.
The pre-computed offences are then passed to `checkIfDefendantProceedingsConcluded()` which
simply reads the `.getProceedingsConcluded()` field on each known offence.

```java
// Call chain:
// getUpdatedDefendant(defendant)
//   └─▶ updateCurrentOffencesWithProceedingsConcludedStatus(defendantId, offences, updatedOffences)
//   └─▶ checkIfDefendantProceedingsConcluded(defendantId, updatedOffences)

private void updateCurrentOffencesWithProceedingsConcludedStatus(...) {
    if (nonNull(defendantOffences)) {
        defendantOffences.forEach(offence -> {
            boolean isOffenceConcluded;
            if (hasNewAmendment(offence)) {
                isOffenceConcluded = isConcluded(offence);
            } else {
                final boolean isOffencePreviouslyConcluded =
                    isOffencePreviouslyConcluded(defendantId, offence.getId());
                if (isOffencePreviouslyConcluded && isEmpty(offence.getJudicialResults())) {
                    // ← BUG was here: previously this branch did not exist
                    isOffenceConcluded = false;
                } else {
                    isOffenceConcluded = isOffencePreviouslyConcluded;
                }
            }
            getUpdatedOffence(updatedOffences, offence, isOffenceConcluded);
        });
    }
}
```

**Why the bug triggers on amendment:**

1. After deletion, the offence is present in the hearing payload but has
   `judicialResults = null / []` (absent or empty).
2. `hasNewAmendment(offence)` → `false` — there are no results, so none carry
   `isNewAmendment: true`.
3. `isOffencePreviouslyConcluded(defendantId, offenceId)` → `true` — the map
   `defendantOffencesResultedOffenceLevel` remembers the result from the previous share.
4. Without the fix, the code falls through to `isOffenceConcluded = isOffencePreviouslyConcluded`
   (= true), making the deletion invisible to the aggregate.

---

## The Fix

### Fix 1 — `HearingAggregate.processHearingResults()`

**File:** `progression-domain/progression-domain-aggregate/src/main/java/uk/gov/moj/cpp/progression/aggregate/HearingAggregate.java`

Remove the `isNotEmpty(judicialResults)` guard so `prosecution-cases-resulted-v2` fires
whenever prosecution cases are present, even when all judicial results have been deleted.

```java
// BEFORE (buggy):
if (isNotEmpty(hearing.getProsecutionCases()) && isNotEmpty(judicialResults)) {
    streamBuilder.add(createProsecutionCasesResultedV2Event(updatedHearing, shadowListedOffences, hearingDay));
}

// AFTER (fixed):
if (isNotEmpty(hearing.getProsecutionCases())) {
    streamBuilder.add(createProsecutionCasesResultedV2Event(updatedHearing, shadowListedOffences, hearingDay));
}
```

### Fix 2 — `CaseAggregate.updateCurrentOffencesWithProceedingsConcludedStatus()`

The fix is implemented in `updateCurrentOffencesWithProceedingsConcludedStatus()`, a method
extracted to pre-compute each offence's `proceedingsConcluded` flag before
`checkIfDefendantProceedingsConcluded()` reads them.

```java
// BEFORE (buggy — logic was inline in checkIfDefendantProceedingsConcluded):
} else {
    if (isOffencePreviouslyConcluded(defendantId, previousOffence.getId())) {
        offenceProceedingsConcludedMap.put(previousOffence.getId(), TRUE); // ← never reset
    } else {
        offenceProceedingsConcludedMap.put(previousOffence.getId(),
            isConcluded(previousOffence));
    }
}

// AFTER (fixed — computation extracted to updateCurrentOffencesWithProceedingsConcludedStatus):
if (hasNewAmendment(offence)) {
    isOffenceConcluded = isConcluded(offence);
} else {
    final boolean isOffencePreviouslyConcluded = isOffencePreviouslyConcluded(defendantId, offence.getId());
    if (isOffencePreviouslyConcluded && isEmpty(offence.getJudicialResults())) {
        //result was explicitly deleted via amendment (empty/absent judicialResults) → reset to false
        isOffenceConcluded = false;
    } else {
        //previously concluded and result not deleted, or not yet resulted
        //fallback to isConcluded(offence) handles first-time results without isNewAmendment flag
        isOffenceConcluded = isOffencePreviouslyConcluded || isConcluded(offence);
    }
}
getUpdatedOffence(updatedOffences, offence, isOffenceConcluded);
```

`checkIfDefendantProceedingsConcluded()` was simplified: it now iterates `defendantCaseOffences`
(all known offences) and reads `.getProceedingsConcluded()` from the pre-computed
`currentUpdatedOffences` list. A null guard handles offences absent from the current hearing
(e.g., resulted in a prior hearing) by falling back to `isOffencePreviouslyConcluded`:

```java
defendantAllOffences.forEach(previousOffence -> {
    final Offence currentOffence = getOffenceById(previousOffence.getId(), currentUpdatedOffences);
    if (nonNull(currentOffence)) {
        offenceProceedingsConcludedMap.put(previousOffence.getId(), currentOffence.getProceedingsConcluded());
    } else {
        // Offence not in current hearing — preserve previously known concluded state
        offenceProceedingsConcludedMap.put(previousOffence.getId(),
            isOffencePreviouslyConcluded(defendantId, previousOffence.getId()));
    }
});
```

**Why `isEmpty(judicialResults)` and not `!isConcluded()`:**
`!isConcluded()` is true for any non-FINAL result (e.g. INTERMEDIARY). In multi-hearing
cases, an offence may appear in the current hearing with INTERMEDIARY+`isNewAmendment=false`
(unchanged result from a prior hearing day) while its FINAL result from a *different*
hearing is preserved in `defendantOffencesResultedOffenceLevel`. Using `!isConcluded()`
would incorrectly reset that cross-hearing concluded state. Empty/absent `judicialResults`
is the precise signal that a result was deliberately removed.

---

## Effect of the fix on `getUpdatedCaseStatus()`

After `checkIfDefendantProceedingsConcluded()` returns `false` for the affected
defendant, the existing logic in `getUpdatedCaseStatus()` (~line 1731) handles the revert:

```java
if (nonNull(currentProsecutionCaseStatus)
        && !currentHearingProceedingsConcluded          // ← now false
        && INACTIVE.getDescription().equalsIgnoreCase(currentProsecutionCaseStatus)
        && nonNull(previousNotInactiveCaseStatus)) {
    updatedCaseStatus = previousNotInactiveCaseStatus;  // reverts to ACTIVE
}
```

No changes required to `getUpdatedCaseStatus()`.

---

## Outcome after fix

| Flag | Before (stale) | After fix |
|------|----------------|-----------|
| `offence.proceedingsConcluded` (result deleted) | `true` | `false` |
| `defendant.proceedingsConcluded` (not all concluded) | `true` | `false` |
| `prosecutionCase.caseStatus` | `INACTIVE` | `ACTIVE` |
| Unchanged offences' `proceedingsConcluded` | `true` | `true` (unaffected) |
| Offence in a different hearing (not in scope) | `true` | `true` (unaffected — not in current hearing's `defendantOffences`) |

---

## Key files

| Role | Path |
|------|------|
| **Fix 1 (event gate)** | `progression-domain/progression-domain-aggregate/src/main/java/uk/gov/moj/cpp/progression/aggregate/HearingAggregate.java` |
| **Fix 2 (wrong value)** | `progression-domain/progression-domain-aggregate/src/main/java/uk/gov/moj/cpp/progression/aggregate/CaseAggregate.java` |
| Method with fix logic | `CaseAggregate.updateCurrentOffencesWithProceedingsConcludedStatus()` |
| Simplified by fix | `CaseAggregate.checkIfDefendantProceedingsConcluded()` (now reads pre-computed flags) |
| Triggers caseStatus revert | `CaseAggregate.getUpdatedCaseStatus()` (unchanged, already handles revert) |
| Unit tests | `CaseAggregateTest.java`, `HearingAggregateTest.java` |
| Integration tests | `HearingAmendmentProceedingsConcludedIT.java` (2 test methods) |
| Related secondary risk | `HearingResultEventListener.getUpdatedJudicialResults()` lines 500–515 (cross-day amendment — separate bug) |
