# `progression.event.laa-defendant-proceeding-concluded-changed` — Test Coverage & Results

**Event:** `progression.event.laa-defendant-proceeding-concluded-changed`
**Raised by:** `CaseAggregate.updateCase(...)` (module `progression-domain-aggregate`) while `UpdateCaseHandler` processes `progression.command.hearing-resulted-update-case`.
**Consumed by:** `LaaDefendantProceedingConcludedEventProcessor` → Azure APIM `concludeDefendantProceeding` → LAA.
**Behaviour change under investigation:** commit `230c68409` ("dd-42137 laa defect fix", PR #496) added the *whole-case* suppression guard `isAllDefendantProceedingConcludedLaa(...)`. Before this the event fired as soon as one defendant concluded; now it is withheld until every LAA‑referenced offence of every defendant on the case has `proceedingsConcluded == true` **and** a judicial result of category `FINAL` **on the offence itself**.

## 2026-09-09 fix: reopened cases were never re-notified to LAA

Investigation (prompted by the "proceedings concluded" conditions table, in particular the
"Amend & Reshare → Yes-Interim" rows where a fully-concluded case is amended back to not-all-final)
found that `230c68409`'s whole-case gate over-corrected: `isAllDefendantProceedingConcludedLaa(...)` only
evaluates the *current* state, so once a case had been reported to LAA as fully concluded, a later
amendment that un-concludes one offence (e.g. FINAL → ANCILLARY) flipped the case status back to
`ACTIVE` correctly, but **no `...-changed` event was raised to tell LAA the case is no longer concluded**.
This was locked in by a regression test (`CaseAggregateTest.shouldNotUpdateProceedingConcludedWithLAAWhenCaseIsUpdatedWithReshare`,
added by `230c68409` itself) that asserted no event fires on reopen — the inverse of what the same test
asserted before that commit.

**Fix:** `CaseAggregate` now tracks, per case, whether the last LAA notification reported the case as
fully concluded (`laaCaseProceedingConcluded` map, updated when `LaaDefendantProceedingConcludedChanged`
is applied). `updateCase` raises the event whenever the *whole-case-concluded-for-LAA* state actually
**transitions** (false→true, newly concludes; or true→false, reopened) instead of only when it becomes
true. Partial/never-fully-concluded hearings still raise nothing, matching S3–S5 below.

The pre-`230c68409` test was restored (renamed back to `shouldUpdateProceedingConcludedWithLAAWhenCaseIsUpdatedWithReshare`
with its original assertions), and a new aggregate-level test
(`CaseAggregateLaaTest.shouldSendLaaConcludedEventWhenPreviouslyConcludedCaseIsReopenedByAmendment`) covers the reopen path directly.

## 2026-09-09 second fix: the gate could never open for a genuine first-time conclusion (found via `remotelog.txt`)

A production/remote debug log (`remotelog.txt`, single defendant, single LAA-referenced offence,
resulted `FINAL` for the first time) showed `isAllDefendantProceedingConcludedLaa(...) -- false` even
though the offence plainly carried a `FINAL` judicial result. Root cause: that call evaluated
`isConcludedForLaa(offence)` — `TRUE.equals(offence.getProceedingsConcluded()) && isConcluded(offence)` —
against `prosecutionCase.getDefendants()`, i.e. the **raw, unprocessed argument** passed into
`updateCase(...)`. On a first-time result, `offence.getProceedingsConcluded()` on that raw object is
`null` (it is only computed later, into `updatedProsecutionCase`), so `isConcludedForLaa` was false for
every offence on every case, and the whole-case gate could in practice never open from a single hearing —
only a prior reshare that happened to carry a pre-set `proceedingsConcluded=true` on the raw payload could
satisfy it, which is not how `public.hearing.resulted` payloads are shaped. This was masked in the
existing test suite because those tests hand-set `.withProceedingsConcluded(true)` directly on the input
`ProsecutionCase`/`Offence`, which a real hearing-resulted payload does not do.

**Fix:** the LAA gate (`isAllDefendantProceedingConcludedLaa`) and the `updatedCaseStatus`/`updatedProsecutionCase`
computation were reordered so the gate evaluates against `updatedProsecutionCase` — the case with
`proceedingsConcluded` already correctly recomputed per offence via `updateDefendantWithProceedingsConcludedStatusAndOriginalListingNumbers` —
instead of the raw `prosecutionCase` argument.

Covered by `CaseAggregateLaaTest.shouldSendLaaConcludedEventOnFirstTimeFinalResultEvenWhenRawOffenceProceedingsConcludedIsNull`,
built directly from the `remotelog.txt` data (same defendant/offence IDs).

Date of run: 2026-09-06 (original), 2026-09-09 (both fixes) · Branch: `team/dd-42137-laa-code-changes` · JDK 17.0.16 · `mvn -o test`

---

## 1. Are there integration tests covering these scenarios?

**Yes — one JVM integration test class**, plus substantial component/unit coverage:

| Layer | Test class | Module |
|---|---|---|
| Integration (JMS + WireMock, containerised WildFly) | `PublicHearingResultedWithFeatureToggleEnabledIT` | `progression-integration-test` |
| Component (command handler + event stream) | `UpdateCaseHandlerTest`, `PatchAndResendLaaCaseOutcomeHandlerTest`, `ResendLaaCaseOutcomeHandlerTest` | `progression-command-handler` |
| Aggregate behaviour | `CaseAggregateLaaTest`, `CaseAggregateTest` (reshare) | `progression-domain-aggregate` |
| Validation helpers | `DefendantHelperTest` | `progression-domain-aggregate` |
| Downstream processor → LAA | `LaaDefendantProceedingConcludedEventProcessorTest`, `LaaDefendantProceedingConcludedResendEventProcessorTest` | `progression-event-processor` |

The integration test **requires the CPP docker stack** (`CPP_DOCKER_DIR`, WildFly deployments, Liquibase, private registry `crmdvrepo01`) via `runIntegrationTests.sh` and was **not executed in this environment**. Its assertions are documented in §4. All other layers were executed — see §3.

---

## 2. Scenario coverage matrix

Scenarios are those from the event-flow analysis. "Event raised?" is the expected outcome on the current branch.

| # | Scenario | Event raised? | Covered by | Result |
|---|---|---|---|---|
| S1 | Single defendant, single LAA offence, offence has `FINAL` result **on the offence** + `proceedingsConcluded=true` | **Yes** | `CaseAggregateLaaTest.shouldUpdateProceedingConcludedWithLAAWhenCaseIsUpdatedWithReshareWhenResultIsInDefendantLevelAndOffenceLevel` (final step); IT `shouldMakeCaseStatusInactiveWhenAllOffencesAreResultedFinal` (indirect) | PASS |
| S2 | Defendant's offence concluded, but `FINAL` result present **only at defendant-judicial-result level, not on the offence** | No (suppressed by Gate B) | `CaseAggregateLaaTest.shouldSendLaaDefendantProceedingConcludedChangedWhenHearingIsResulted` *(name now stale — asserts no event)* | PASS |
| S3 | Multi-offence defendant, only some offences concluded this hearing (partial) | No (suppressed) | `CaseAggregateLaaTest.shouldSendLaaConcludedEventWithCurrentOffencesWhenCurrentHearingIsResulted`, `...WithPrevResultedOffencesWhenCurrentHearingIsNotResulted`, `...AndThereAreNoPrevResultedOffences` | PASS |
| S4 | Offence has a non‑`FINAL` result only (`ANCILLARY` / `INTERMEDIARY`) + `proceedingsConcluded=true` | No | `DefendantHelperTest.shouldAllDefendantsProceedingsConcludedBeFalseWhenOneDefendantWithAncillaryJudicialResultAndOffenceProceedingsConcluded`, `...WithIntermediaryJudicialResult...` | PASS |
| S5 | Offence has a `FINAL` result but `proceedingsConcluded=false` | No | `DefendantHelperTest.shouldAllDefendantsProceedingsConcludedBeFalseWhenOneDefendantWithFinalJudicialResultAndOffenceProceedingsNotConcluded` | PASS |
| S6 | Case has **no LAA reference** on any offence | Not in scope (Gate A) | IT `shouldInvokeProcessFlowsWhenHearingResultsArePublishedOnDifferentOrderDays` — `verifyLaaProceedingsConcludedCommandInvoked(0, ...)`; `CaseAggregateTest.shouldNotUpdateProceedingConcludedWithLAAWhenCaseIsUpdatedWithReshare` | PASS (unit); IT not run |
| S7 | Hearing re-resulted / replayed with no change to any concluded flag | No (Gate A — no state change) | `CaseAggregateLaaTest` reshare tests (`...WhenResultIsInDefendantLevel`) | PASS |
| S8 | All offences of all defendants `FINAL` + concluded across multiple hearings → also flips case `INACTIVE` | Yes | **precondition only**: `UpdateCaseHandlerTest.shouldMarkCaseInactive_whenAllOffencesOfAllDefendantsHaveFinalCategory_inMultipleHearings`, `shouldMarkCaseInactive_inMultipleHearings_whenAllOffencesOfDefendantHaveFinalCategory` assert `proceedingsConcluded`/`caseStatus=INACTIVE` but **do not assert the LAA event itself** | PASS |
| S9 | Offence amended with `FINAL` result on a later hearing | Yes (case also goes INACTIVE) | **precondition only**: `UpdateCaseHandlerTest.shouldProcessHearingResultedUpdateCaseCommand_whenOneOfOffenceIsAmendedWithFinalResult_expectProceedingConcludedAsTrueAndCaseInActive` | PASS |
| S10 | `resend-laa-outcome-concluded` command re-emits stored `...-changed` payloads as `...-resent` | Yes (`...-resent`) | `ResendLaaCaseOutcomeHandlerTest.testResendDefendantProceedingConludedToLaa`; `CaseAggregateLaaTest.testResendLaaOutcomeConcluded`; `LaaDefendantProceedingConcludedResendEventProcessorTest` | PASS |
| S11 | `patch-and-resend` fills a missing `hearingId` on a stored `...-changed` event | Yes (`...-resent` with patched hearingId) | `PatchAndResendLaaCaseOutcomeHandlerTest.shouldHandleCommandSuccessfullyWhenLAAEventFoundForResultingDateWithEmptyHearingID`; `CaseAggregateLaaTest.shouldSuccessfullyPatchAndResendLaaOutcomeConcluded` | PASS |
| S12 | patch-and-resend with 0 or >1 matching stored events → error | n/a | `PatchAndResendLaaCaseOutcomeHandlerTest.shouldHandleCommandWhenNoMatchingEventFoundWithResultingDate`, `...WhenMoreThanOneMatchingEventsFound`, `...WithHearingID` | PASS |
| S13 | Downstream: `...-changed` event → Azure APIM call, with retry/exception on failure | Yes | `LaaDefendantProceedingConcludedEventProcessorTest.shouldHandleDefendantProceedingConcludedEventMessage`, `...ThrowDefendantProceedingConcludedExceptionAfterAllRetries` | PASS |
| S14 | `caseStatus == EJECTED` → whole `updateCase` block skipped | No event | *Not directly asserted for the LAA event* — see §5 gap G1 |
| S15 | SJP hearing dropped before any command is sent | No event | *Covered upstream in hearing/SJP flow, not in this repo's LAA tests* — see §5 gap G2 |
| S16 | Case previously reported to LAA as fully concluded (all offences `FINAL`+concluded) is **reopened** by a later amendment that un-concludes one offence (e.g. `FINAL` → `ANCILLARY`) | **Yes** (`proceedingsConcluded=false` at defendant level) — fixed 2026-09-09, was suppressed before | `CaseAggregateTest.shouldUpdateProceedingConcludedWithLAAWhenCaseIsUpdatedWithReshare`; `CaseAggregateLaaTest.shouldSendLaaConcludedEventWhenPreviouslyConcludedCaseIsReopenedByAmendment` | PASS |

---

## 3. Executed test results

All commands run with `mvn -o test` (offline) on branch `team/dd-42137-laa-code-changes`.

| Test class | Tests | Failures | Errors | Skipped | Time |
|---|---:|---:|---:|---:|---:|
| `CaseAggregateLaaTest` | 9 | 0 | 0 | 0 | 0.711 s |
| `DefendantHelperTest` | 44 | 0 | 0 | 0 | 0.099 s |
| `CaseAggregateTest#shouldNotUpdateProceedingConcludedWithLAAWhenCaseIsUpdatedWithReshare` | 1 | 0 | 0 | 0 | 1.017 s |
| `UpdateCaseHandlerTest` *(covers `proceedingsConcluded` / case-status preconditions; does not assert the LAA event)* | 12 | 0 | 0 | 0 | 1.480 s |
| `PatchAndResendLaaCaseOutcomeHandlerTest` | 4 | 0 | 0 | 0 | 0.098 s |
| `ResendLaaCaseOutcomeHandlerTest` | 2 | 0 | 0 | 0 | 1.353 s |
| `LaaDefendantProceedingConcludedEventProcessorTest` | 2 | 0 | 0 | 0 | 2.148 s |
| `LaaDefendantProceedingConcludedResendEventProcessorTest` | 1 | 0 | 0 | 0 | 0.702 s |
| **Total** | **75** | **0** | **0** | **0** | |

> Note: the working tree currently contains debug `System.out.println` statements in `CaseAggregate.java` and `DefendantHelper.java` (uncommitted, from defect investigation). They do not affect test outcomes.

### `CaseAggregateLaaTest` — method-level outcomes

| Method | Asserts | Result |
|---|---|---|
| `testResendLaaOutcomeConcluded` | resend produces `...-resent` per input | PASS |
| `shouldSuccessfullyPatchAndResendLaaOutcomeConcluded` | null hearingId → patched `...-resent` | PASS |
| `shouldIgnorePatchAndResendLaaOutcomeConcludedWhenEventHasHearingID` | non-null hearingId → empty stream | PASS |
| `shouldSendLaaDefendantProceedingConcludedChangedWhenHearingIsResulted` | *(stale name)* `FINAL` only at defendant level → **no** LAA event, only `HearingResultedCaseUpdated` | PASS |
| `shouldNotSendLaaConcludedEventWithPrevResultedOffencesWhenCurrentHearingIsNotResulted` | partial conclusion → no LAA event | PASS |
| `shouldNotSendLaaConcludedEventWithCurrentOffencesWhenCurrentHearingIsNotResultedAndThereAreNoPrevResultedOffences` | partial conclusion → no LAA event | PASS |
| `shouldSendLaaConcludedEventWithCurrentOffencesWhenCurrentHearingIsResulted` | *(stale name)* offence1 has no `FINAL` on-offence → no LAA event | PASS |
| `shouldUpdateProceedingConcludedWithLAAWhenCaseIsUpdatedWithReshareWhenResultIsInDefendantLevel` | defendant-level only → no LAA event | PASS |
| `shouldUpdateProceedingConcludedWithLAAWhenCaseIsUpdatedWithReshareWhenResultIsInDefendantLevelAndOffenceLevel` | final step: both offences `FINAL` on-offence + concluded → **LAA event raised**, all `proceedingsConcluded = true` | PASS |

### `DefendantHelperTest` — new methods from commit `230c68409`

| Method | Result |
|---|---|
| `shouldAllDefendantsProceedingsConcludedBeTrueWhenEmptyDefendants` | PASS |
| `shouldAllDefendantsProceedingsConcludedBeFalseWhenOneDefendantWithFinalJudicialResultAndOffenceProceedingsNotConcluded` | PASS |
| `shouldAllDefendantsProceedingsConcludedBeTrueWhenOneDefendantWithFinalJudicialResultAndOffenceProceedingsConcluded` | PASS |
| `shouldAllDefendantsProceedingsConcludedBeFalseWhenOneDefendantWithAncillaryJudicialResultAndOffenceProceedingsConcluded` | PASS |
| `shouldAllDefendantsProceedingsConcludedBeFalseWhenOneDefendantWithIntermediaryJudicialResultOffenceAndProceedingsConcluded` | PASS |

---

## 4. Integration test: `PublicHearingResultedWithFeatureToggleEnabledIT`

**Not run here** (needs the CPP docker environment). Relevant assertions on the current branch:

| Test | What it drives | LAA assertion |
|---|---|---|
| `shouldInvokeProcessFlowsWhenHearingResultsArePublishedOnDifferentOrderDays` | `public.events.hearing.hearing-resulted` for a case **without representation**, then `resend-laa-outcome-concluded` command, then a 2nd result | `verifyLaaProceedingsConcludedCommandInvoked(0, [hearingId, caseId, defendantId])` — LAA APIM **must not** be called (case has no LAA reference). The post-resend `verifyLaaProceedingsConcludedCommandInvoked(2, ...)` line is **commented out** by commit `230c68409` with the note *"LAA proceedings concluded should now be filtered"*. |
| `whenDefendantJudicialResultWithFinalCategoryIsPresentAtDefendantLevel` | hearing resulted with a defendant-level `FINAL` result | asserts offence `proceedingsConcluded = true` in the read model; does not assert the LAA APIM call |
| `shouldMakeCaseStatusInactiveWhenAllOffencesAreResultedFinal` | one defendant, two offences, all resulted `FINAL` | case status → `INACTIVE` (this is the "whole case concluded" happy path that also satisfies Gate B) |
| `whenResultedBeforeLAAGrantAndLAAGrantIsProvidedLaterAndResulted` | result without LAA, then record LAA reference | verifies defendant-offences / legal-aid-status updates; no LAA APIM assertion |
| `shouldSendLAAConcludedEventWithOffencesWhenConsecutiveHearingResultedForSingleOffenceWithNoJudiciaryResults` | *(disabled — whole method commented out by commit `230c68409`)* previously asserted `verifyLaaProceedingsConcludedCommandInvoked(1, ...)` per consecutive single-offence result | n/a — removed by the fix |

**Observation:** after commit `230c68409` there is **no active integration test that asserts the `...-changed` event / LAA APIM call actually fires** for a valid fully-concluded LAA case. The positive path is only covered at aggregate/handler level.

---

## 5. Coverage gaps & recommended new tests

| ID | Gap | Recommended test |
|---|---|---|
| G1 | No test asserts `caseStatus == EJECTED` suppresses the LAA event (S14). | `CaseAggregateLaaTest.shouldNotSendLaaConcludedEventWhenCaseIsEjected` — apply `ProsecutionCaseCreated` with status `EJECTED`, call `updateCase` with a fully-concluded LAA defendant, assert no `LaaDefendantProceedingConcludedChanged`. |
| G2 | No local test for the SJP-hearing short-circuit (S15) — it lives in `HearingResultEventProcessor.handleHearingResultedPublicEvent`. | `HearingResultEventProcessorTest.shouldIgnoreSjpHearingResulted` — send `public.hearing.resulted` with `isSJPHearing=true`, verify no `progression.command.hearing-result` sent. |
| G3 | **Multi-defendant** partial conclusion is only covered as single-defendant multi-offence. Add an explicit 2-defendant case. | `CaseAggregateLaaTest.shouldNotSendLaaConcludedEventWhenOnlyOneOfTwoDefendantsConcluded` — case with defendant A (all `FINAL` + concluded) and defendant B (open offence); assert no event. Then re-result B fully and assert the event now carries **both** defendants. |
| G4 | No **active integration test** asserts the LAA APIM call fires for a valid fully-concluded LAA case (post-fix). | New IT `shouldInvokeLaaProceedingsConcludedWhenWholeCaseConcludedWithLaaReference` in `PublicHearingResultedWithFeatureToggleEnabledIT` — LAA reference recorded, single defendant, all offences resulted `FINAL` on-offence; `verifyLaaProceedingsConcludedCommandInvoked(1, [caseId, defendantId])`. |
| G6 | `UpdateCaseHandlerTest` registers `LaaDefendantProceedingConcludedChanged` in the enveloper but no method asserts it is present or absent in the appended stream. | Add `eventStreamAppendedWith(... streamContaining/notContaining LaaDefendantProceedingConcludedChanged ...)` assertions to the multi-hearing INACTIVE tests. |
| G5 | The stale test names `shouldSendLaaDefendantProceedingConcludedChangedWhenHearingIsResulted` and `shouldSendLaaConcludedEventWithCurrentOffencesWhenCurrentHearingIsResulted` now assert the opposite of what they say. | Rename to `shouldNotSend...` to match behaviour, or restructure so they build a genuine on-offence `FINAL` result and assert the event is raised. |

---

## 6. How to run

```bash
# Unit / component (fast, offline)
mvn -o test -pl progression-domain/progression-domain-aggregate \
  -Dtest='CaseAggregateLaaTest,DefendantHelperTest'
mvn -o test -pl progression-command/progression-command-handler \
  -Dtest='UpdateCaseHandlerTest,PatchAndResendLaaCaseOutcomeHandlerTest,ResendLaaCaseOutcomeHandlerTest'
mvn -o test -pl progression-event/progression-event-processor \
  -Dtest='LaaDefendantProceedingConcludedEventProcessorTest,LaaDefendantProceedingConcludedResendEventProcessorTest'

# Integration (requires cpp-developers-docker checked out + CPP_DOCKER_DIR exported)
export CPP_DOCKER_DIR=/path/to/cpp-developers-docker
./runIntegrationTests.sh
# or, against an already-running stack:
mvn -o verify -pl progression-integration-test \
  -Dtest='PublicHearingResultedWithFeatureToggleEnabledIT'
```
