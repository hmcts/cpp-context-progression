# Tasks: CHD-2474 — Amendment proceedings-concluded reset

- [x] Fix `HearingAggregate.processHearingResults()` — emit `prosecution-cases-resulted-v2` even when `judicialResults` is empty
- [x] Fix `CaseAggregate` — extract `updateCurrentOffencesWithProceedingsConcludedStatus()` with result-deleted condition; simplify `checkIfDefendantProceedingsConcluded()` to read pre-computed flags
- [x] Unit test: amendment scenario in `CaseAggregateTest`
- [x] Unit test: unchanged offences unaffected
- [x] Unit test: offence not in current hearing scope (null guard — previously concluded offence absent from current hearing stays true)
- [x] Unit test: offence absent from current hearing and never previously resulted stays false
- [x] Unit test: `HearingAggregateTest` updated to assert `prosecution-cases-resulted-v2` is emitted when judicialResults is empty
- [x] Integration test: `HearingResultedCaseUpdatedIT` — single defendant, amendment deletes result

---

## Task detail

### Fix `CaseAggregate` — extract and fix `updateCurrentOffencesWithProceedingsConcludedStatus()`

**File:**
`progression-domain/progression-domain-aggregate/src/main/java/uk/gov/moj/cpp/progression/aggregate/CaseAggregate.java`

The offence-level `proceedingsConcluded` computation was extracted into a new method
`updateCurrentOffencesWithProceedingsConcludedStatus()`. The fix adds the result-deleted
condition inside this method. `checkIfDefendantProceedingsConcluded()` was simplified to
just read the pre-computed flags.

**`updateCurrentOffencesWithProceedingsConcludedStatus()` logic:**
```java
if (hasNewAmendment(offence)) {
    isOffenceConcluded = isConcluded(offence);
} else {
    final boolean isOffencePreviouslyConcluded = isOffencePreviouslyConcluded(defendantId, offence.getId());
    if (isOffencePreviouslyConcluded && isEmpty(offence.getJudicialResults())) {
        //result was explicitly deleted via amendment → reset to false
        isOffenceConcluded = false;
    } else {
        //fallback to isConcluded(offence) handles first-time results without isNewAmendment flag
        isOffenceConcluded = isOffencePreviouslyConcluded || isConcluded(offence);
    }
}
getUpdatedOffence(updatedOffences, offence, isOffenceConcluded);
```

**`checkIfDefendantProceedingsConcluded()` after simplification (with null guard):**
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

The null guard is required for multi-hearing scenarios: when a defendant's offence was resulted
in a prior hearing and is absent from the current hearing payload, `getOffenceById` returns null.
Without the guard, `null.getProceedingsConcluded()` throws an NPE.

Using `isEmpty(judicialResults)` not `!isConcluded()`: the latter would incorrectly reset
offences with a non-FINAL result that appear unchanged in the current hearing while their
FINAL result from a different hearing is preserved in `defendantOffencesResultedOffenceLevel`.

---

### Unit tests in `CaseAggregateTest`

**File:**
`progression-domain/progression-domain-aggregate/src/test/java/uk/gov/moj/cpp/progression/aggregate/CaseAggregateTest.java`

Add the following test scenarios (follow existing test patterns in the file):

**Test 1 — amendment deletes result, proceedingsConcluded resets:**
- Given: aggregate with a defendant whose single offence had a FINAL result
  (initial share already set `proceedingsConcluded = true`, `caseStatus = INACTIVE`)
- When: `updateCase()` is called with the same prosecution case where that offence now
  has an empty `judicialResults` list (result deleted) and no `isNewAmendment` flag
- Then:
  - `proceedingsConcluded = false` on the offence
  - `proceedingsConcluded = false` on the defendant
  - `caseStatus` reverts to the previous non-INACTIVE status (ACTIVE or equivalent)

**Test 2 — unchanged offences are unaffected:**
- Given: aggregate with two offences, both previously concluded
- When: `updateCase()` is called with one offence having empty `judicialResults` and the
  other still having a FINAL result
- Then:
  - Amended offence: `proceedingsConcluded = false`
  - Unchanged offence: `proceedingsConcluded = true`
  - `caseStatus = ACTIVE` (not all concluded)

**Test 3 — previously concluded offence absent from current hearing stays true (`shouldPreserveProceedingsConcludedForOffenceAbsentFromCurrentHearing`):**
- Given: aggregate with two offences, both previously concluded; the current hearing payload
  contains only one of them (offence2 is absent — belonged to a different hearing day)
- When: `updateCase()` is called with offence1 having empty `judicialResults` (deleted)
- Then:
  - Offence1: `proceedingsConcluded = false` (result deleted)
  - Offence2: `proceedingsConcluded = true` preserved via null guard → `isOffencePreviouslyConcluded = true`
  - Defendant: `proceedingsConcluded = false` (not all concluded) → `caseStatus = ACTIVE`

**Test 4 — offence absent and never previously resulted stays false (`shouldKeepOffenceNotConcludedWhenAbsentFromCurrentHearingAndNeverPreviouslyResulted`):**
- Given: aggregate with two offences, neither previously resulted; the current hearing
  payload contains only offence1 (FINAL + `isNewAmendment=true`); offence2 is absent
- When: `updateCase()` is called
- Then:
  - Offence1: concluded (FINAL result in current hearing)
  - Offence2: `isOffencePreviouslyConcluded = false` (never in `defendantOffencesResultedOffenceLevel`) → `proceedingsConcluded = false`
  - Defendant: `proceedingsConcluded = false` → `caseStatus = ACTIVE`

---

### Integration test: `HearingResultedCaseUpdatedIT`

**File:**
`progression-integration-test/src/test/java/uk/gov/moj/cpp/progression/HearingResultedCaseUpdatedIT.java`

**`shouldUpdateHearingResultedCaseUpdatedV2_ThenAmendWithResultDeleted`:**
1. Create 1 case with 1 defendant (1 offence) via `addProsecutionCaseToCrownCourt`
2. Send `hearing-resulted` with FINAL result → assert `caseStatus = INACTIVE`, `defendant.proceedingsConcluded = true`, `offence.proceedingsConcluded = true`
3. Send `hearing-resulted` (amendment) with `judicialResults` deleted → assert `caseStatus = ACTIVE`, `defendant.proceedingsConcluded = false`

Fixtures: `public.events.hearing.hearing-resulted-case-updated.json` (initial), `public.events.hearing.hearing-resulted-amendment-deleted.json` (amendment)

---

## Verification

```bash
# Domain aggregate unit tests (includes CaseAggregateTest)
mvn test -pl progression-domain/progression-domain-aggregate \
         -Dtest=CaseAggregateTest

# Full integration test suite
CPP_DOCKER_DIR=/path/to/cpp-developers-docker ./runIntegrationTests.sh
```

Spot-check regression: existing tests for initial result sharing must still pass.
