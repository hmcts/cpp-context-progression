# Spec: Hearing amendment — proceedings-concluded reset

## Purpose

Defines the expected behaviour of `proceedingsConcluded` and `caseStatus` when a
judicial result is removed from an offence via a hearing amendment and results are
re-shared.

## Definitions

| Term | Meaning |
|------|---------|
| **proceedingsConcluded (offence)** | `true` iff the offence has at least one judicial result with `category = FINAL` |
| **proceedingsConcluded (defendant)** | `true` iff ALL of the defendant's offences have `proceedingsConcluded = true` |
| **caseStatus = INACTIVE** | All defendants on the case have `proceedingsConcluded = true` |
| **caseStatus = ACTIVE** | At least one defendant has `proceedingsConcluded = false` |

These definitions govern the initial result-sharing path. This spec asserts they
**must also hold after a hearing amendment** that removes a judicial result.

## Acceptance Criteria

### AC-1: Offence with deleted result becomes active

Given an offence that had a FINAL judicial result (`proceedingsConcluded: true`),
When that result is deleted via a hearing amendment and results are re-shared,
Then `offence.proceedingsConcluded` MUST be `false`.

### AC-2: Defendant status follows its offences

Given a defendant whose all offences were concluded (`proceedingsConcluded: true`),
When one of that defendant's offence results is deleted and results are re-shared,
Then `defendant.proceedingsConcluded` MUST be `false`.

### AC-3: Case status reverts to ACTIVE

Given a prosecution case with `caseStatus = INACTIVE` (all defendants concluded),
When at least one defendant's `proceedingsConcluded` becomes `false`,
Then `prosecutionCase.caseStatus` MUST NOT remain `INACTIVE`.
(It reverts to the status held prior to becoming INACTIVE, per the existing
`previousNotInactiveCaseStatus` revert logic in `CaseAggregate.getUpdatedCaseStatus()`.)

### AC-4: Unchanged offences are unaffected

Given other offences on the same defendant/case that still have their FINAL results,
When a separate offence's result is deleted and results are re-shared,
Then each unchanged offence's `proceedingsConcluded` MUST remain `true`.

### AC-5: Offences not in scope of the current hearing are unaffected

Given an offence that was concluded in a previous hearing but is not included
in the prosecution case of the current hearing being resulted,
When that hearing's results are processed,
Then that offence's `proceedingsConcluded` MUST remain `true`.

(This distinguishes "result deleted" from "offence not in this hearing".)

### AC-6: Aggregate internal state is consistent with viewstore

After the amendment event chain completes, querying the prosecution case via
`progression.query.prosecutioncase` MUST return values consistent with the recalculated
`CaseAggregate` state. The `hearing-resulted-case-updated` event MUST carry the
recalculated `proceedingsConcluded` values, not the stale ones.

## Mechanism

The fix operates at `CaseAggregate.checkIfDefendantProceedingsConcluded()`. No changes
are required to the event chain, command flow, or viewstore listeners — the existing
`hearing-resulted-case-updated` path already propagates the recalculated values once
the aggregate computes them correctly.

## Data Contract

`proceedingsConcluded` and `caseStatus` are already present in:

- `application/vnd.progression.query.prosecutioncase+json`
- `application/vnd.progression.query.application.aaag+json`

No schema changes required.

## Out of Scope

- Recalculation when an offence is added to a hearing (separate concern).
- Cross-day amendments where the result was entered on a different hearing day than
  the amendment — this is a related but separate bug in `HearingResultEventListener.
  getUpdatedJudicialResults()` (lines 500–515).
- Data fix for cases already stale in production (3rd-line BDF task).
