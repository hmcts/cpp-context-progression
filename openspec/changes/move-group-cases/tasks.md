# Tasks: Add `progression.move-group-cases`

## Decisions needed before implementation starts

Resolved during design exploration (see `proposal.md`'s Open Questions for
full context/reasoning on each):

- [x] New-group genesis approach: dedicated new event, `GroupCreatedFromMovedCases`
      (Open Question 1 — DECIDED)
- [x] Civil fee sourcing for moved cases: fork-once-per-new-group (Open
      Question 2 — DECIDED)
- [x] Old group may not be left with zero members — guarded (Open Question
      3 — DECIDED)
- [x] Moved cases' hearing association must be immediately consistent (Open
      Question 4 — DECIDED)
- [x] Resolving the "current" hearing to mirror — via the viewstore, no API
      change needed (Open Question 5 — RESOLVED)
- [x] `Audit Required: No` stands (Open Question 6, part 1 — DECIDED)
- [ ] Exact rejection/idempotency behaviour for an already-existing
      `newGroup.groupId` — recommendation given (distinguish exact-match
      retry from genuine collision), **not yet formally confirmed with the
      team**. Implementing against the recommendation below; revisit if the
      team decides differently.

Also flagged, not yet resolved, not blocking implementation start: the
"stale case data in a hearing's cached payload" risk in `design.md`
(`CaseGroupInfoUpdatedEventListener`'s unordered hearing-pick). Tracked as
its own task below.

## Implementation

- [x] Add `GroupCreatedFromMovedCases` and
      `LastCasesToBeMovedFromGroupRejected` event schemas
      (`progression-domain-message`), registered in
      `subscriptions-descriptor.yaml`
- [x] Add `/prosecutioncases` POST resource + `move-group-cases` `(mapping)`,
      schema, and example JSON to the command RAML (both command-api and
      command-handler layers)
- [x] Add `MoveGroupCasesApi` (`COMMAND_API`) handler, mirroring
      `RemoveCaseFromGroupCasesApi`
- [x] Add `MoveGroupCasesHandler` (`COMMAND_HANDLER`) — validation, both
      guards (empty-group, existing-groupId retry/collision), old-group
      bulk removal, new-group genesis, per-case group association, fee
      cloning. Hearing mirroring deliberately NOT here — see next item.
- [x] Add bulk-removal method (`moveCasesToNewGroup` + `canMoveCasesOut` +
      `rejectLastCasesToBeMovedFromGroup`) to `GroupCaseAggregate` for the
      old group side
- [x] Add new-group genesis method (`createGroupFromMovedCases`) +
      `apply()` wiring to `GroupCaseAggregate`
- [x] Extend `CaseAggregate.updateCaseGroupInfo` to carry a target `groupId`
      through to `CaseGroupInfoUpdated`; 2-arg overload now delegates to the
      3-arg one passing the existing `groupId` — `RemoveCaseFromGroupCasesHandler`'s
      two call sites need no changes
- [x] Implement civil fee resolution for the new group (clone once, shared
      across all moved cases) — in `MoveGroupCasesHandler`
- [x] **Architecture correction during implementation**: hearing
      creation/mirroring moved OUT of `MoveGroupCasesHandler` into a new
      event processor, `GroupCreatedFromMovedCasesProcessor`, reacting to
      `GroupCreatedFromMovedCases` — no command handler in this codebase
      reads the viewstore, and `CaseDefendantHearingRepository` access
      belongs at the same layer `CourtProceedingsInitiatedProcessor`
      already uses for the equivalent bulk-creation orchestration. See
      design.md's "Architecture correction" note.
- [x] Add validation for `leadCaseId ∈ caseIds` and
      `currentGroup.newLeadCaseId` presence/absence rules → thrown as
      `IllegalArgumentException` in `MoveGroupCasesHandler` (framework's
      exact HTTP status mapping for this not independently verified in this
      session — no network/build access to confirm; flagged for review)
- [x] Add access-control Drools rule for `progression.move-group-cases`
      (System User only)
- [x] Republish `LastCasesToBeMovedFromGroupRejected` cross-context as
      `public.progression.move-last-cases-from-group-rejected` — added to
      `CaseRemoveFromGroupCaseProcessor`
- [x] Republish `GroupCreatedFromMovedCases` cross-context, unchanged, as
      `public.progression.group-created-from-moved-cases` — the success
      confirmation a caller (e.g. case-management UI) waits for before
      navigating to a confirmation screen. Added to
      `GroupCreatedFromMovedCasesProcessor` (not a new class — mirrors
      `CaseRemovedFromGroupCasesProcessor`'s bare republish pattern for the
      analogous single-case-remove success event), sent *before*
      `resolveCurrentHearing(...)`'s early-return branch so the confirmation
      never depends on whether there was a hearing to mirror. New schema
      `public.progression.group-created-from-moved-cases.json`, registered
      in `public-publications-descriptor.yaml`. `MoveGroupCasesIT`'s
      happy-path test updated to assert it's received with the correct
      `groupId`/`caseIds`/`leadCase`.
- [ ] Address (or explicitly defer with a tracked follow-up) the stale
      hearing-cached-payload risk from `design.md` before this flow can
      produce a case with 2+ `case_defendant_hearing` rows in practice —
      **not done this session**
- [x] **Coverage audit + gap-fill**: audited every production file touched
      by this change for test coverage (`CaseAggregate`, `GroupCaseAggregate`,
      `CaseRemoveFromGroupCaseProcessor`, `MoveGroupCasesApi`,
      `command-move-group-cases.drl`) and closed what was missing:
      - `CaseAggregateTest`: added
        `shouldUpdateCaseGroupInfoPreservingExistingGroupId` (2-arg overload)
        and `shouldUpdateCaseGroupInfoCarryingCaseToTheProvidedNewGroupId`
        (3-arg overload) — `updateCaseGroupInfo` had no direct aggregate-level
        test before, only indirect coverage via handler tests.
      - New `CaseRemoveFromGroupCaseProcessorTest` — this processor had no
        test file at all. Covers both `@Handles` methods: the pre-existing
        `processEvent` (→ `public.progression.remove-last-case-in-group-cases-rejected`)
        and the new `processMoveRejectedEvent` added by this change
        (→ `public.progression.move-last-cases-from-group-rejected`).
      - New `MoveGroupCasesApiTest`, modeled 1:1 on sibling
        `RemoveCaseFromGroupCasesApiTest` — `MoveGroupCasesApi` had no test;
        verifies `progression.move-group-cases` is re-dispatched unchanged as
        `progression.command.move-group-cases`.
      - `ProgressionCommandRuleExecutorTest.ProgressionRules`: added
        `MoveGroupCases("progression.move-group-cases", "System Users")` —
        `command-move-group-cases.drl` is covered by this hand-maintained
        enum, not by any generic/automatic directory scan, so without this
        entry the rule had no RBAC test coverage at all.
      - `GroupCaseAggregate`'s new methods (`moveCasesToNewGroup`,
        `canMoveCasesOut`, `rejectLastCasesToBeMovedFromGroup`,
        `createGroupFromMovedCases`) were already fully covered by the
        existing `GroupCaseAggregateTest` updates — no gap found there.
      - All four affected modules re-run together: 0 failures (
        `progression-domain-aggregate` 230 tests/+2,
        `progression-command-api` +2 new test files,
        `progression-event-processor` 1328 tests/+2,
        `progression-command-handler` unchanged/still green).
- [x] Add/extend unit tests: `GroupCaseAggregateTest`,
      `MoveGroupCasesHandlerTest` — 7 cases: handler-matcher check, happy-path
      move (old master unmoved, event content + civil-fee single-clone +
      per-case fee reassignment all verified), old-master-moved path, the
      zero-members rejection, and all three `IllegalArgumentException`
      validation branches.
- [x] **Bug found and fixed**: when the old group's master is itself one of
      the moved cases, `MoveGroupCasesHandler` was only reading the provided
      `currentGroup.newLeadCaseId`'s `CaseAggregate` to embed a stale
      snapshot in `CaseRemovedFromGroupCases.newGroupMaster` — it never
      promoted that case (`isGroupMaster`/`isGroupMember` never flipped, no
      event raised on its own stream), unlike `RemoveCaseFromGroupCasesHandler`'s
      equivalent new-master promotion. Fixed by calling
      `updateCaseGroupInfo(true, true)` on it (and appending the resulting
      event) before taking the snapshot. `MoveGroupCasesHandlerTest`'s
      `shouldPromoteProvidedOldGroupNewMaster_AndCarryItOntoRemovalEvent`
      updated to assert the fix.
- [x] **Second bug found and fixed, while writing the integration test**:
      `GroupCreatedFromMovedCasesProcessor`'s command never populated
      `listHearingRequests`, so `ProsecutionCaseDefendantListingStatusChangedProcessor`'s
      `!CollectionUtils.isEmpty(listHearingRequests)` guard silently skipped
      the outbound `listingService.listCourtHearing(...)` call — the new
      group's hearing would have existed only inside progression's own
      store, with the listing context never told about it. Fixed by
      deriving a `ListHearingRequest` (court centre/hearing type/session
      time from the mirrored hearing, one `ListDefendantRequest` per moved
      defendant) and including it. See design.md's new note under "Decided
      step 4" for the accepted `numberOfGroupCases`-staleness caveat that
      comes with it. Compiles clean; `progression-event-processor`'s
      existing 1323 tests still pass (no processor-level unit test exists
      yet for this class specifically — see below).
- [x] Add `GroupCreatedFromMovedCasesProcessorTest` — 3 cases:
      `shouldPublishPublicConfirmation_AndNothingElse_WhenNoCurrentHearingFound`
      (public confirmation sent, no `update-defendant-listing-status`
      command when `caseDefendantHearingRepository.findByCaseId` returns
      empty), `shouldNotMirrorAHearingThatHasAlreadyBeenResulted` (a
      `HEARING_RESULTED` link is filtered out by `resolveCurrentHearing`, so
      again only the confirmation is sent), and
      `shouldMirrorCurrentHearing_AndNotifyListingWithAListHearingRequestPerMovedDefendant`
      (happy path: both the public confirmation and the
      `update-defendant-listing-status` command are sent in order; the new
      hearing gets a fresh id distinct from the mirrored one; the derived
      `listHearingRequests` contains exactly one request carrying the old
      hearing's court centre and one `listDefendantRequest` per moved
      defendant, correctly attributing `prosecutionCaseId`/`defendantId`
      across both moved cases). Follows this module's established
      instance-`Enveloper` mocking convention (`@Spy Enveloper enveloper =
      EnveloperFactory.createEnveloper()` + `@InjectMocks`, per
      `UpdateDefendantsWithMatchedRequestedProcessorTest`), not the
      static-`Enveloper.envelop(...)` convention used in
      `MoveGroupCasesHandlerTest`. `progression-event-processor`'s full
      suite: 1326 tests, 0 failures, 8 pre-existing skips (1323 + these 3).
- [x] Add integration test: `MoveGroupCasesIT` — two tests: the happy-path
      split (old group shrinks correctly, new group's `group-cases` query
      is correct, each moved case's own progression view shows the new
      `groupId`, a *new* hearing is created and distinguishable from the
      old one, and — proving the `listHearingRequests` fix — Listing
      actually receives a POST for the new hearing with the moved cases
      attached), and the zero-members rejection (public event + old group
      unchanged). Added a `moveGroupCases(...)` REST helper to
      `PreAndPostConditionHelper`, mirroring `removeCaseFromGroupCases`.
      Compiles clean (`mvn test-compile`) but **not run** — this
      environment has no Docker/`CPP_DOCKER_DIR`, so it has never actually
      executed against a live stack. Run it for real before merging.
- [x] **Ran `MoveGroupCasesIT` for real against a live Docker stack** — found
      and fixed a bug in the test itself (not production code): the
      group-cases query view (`GroupCaseConverter`) serializes the lead
      case's identifier as `leadCase.caseId`, not `leadCase.id` — that shape
      is custom to this query (see `groupCasesQueryResponse.json`), distinct
      from the public confirmation event's `leadCase`, which is a full
      `ProsecutionCase` POJO and correctly uses `id`. The real run's actual
      payload showed the correct master case id under `leadCase.caseId`,
      confirming the move itself worked — only the test's JSON path
      (`$.leadCase.id`) was wrong for the two `pollGroupCases(...)`
      assertions. Fixed both to `$.leadCase.caseId`; compiles clean.
      The remaining 18 failures / 3 other errors in that same real run
      (ApplicationReferredToBoxworkIngesterIT, CourtApplicationCreatedIngesterIT,
      ProsecutorUpdatedIT, SearchInactiveMigratedCasesIT,
      HearingConfirmedForCourtApplicationsIngestIT "Connection refused", etc.)
      are the same pre-existing, unrelated set already investigated earlier
      in this change's work (indexer/WireMock timing and test-ordering issues
      in this Docker environment, not touching group-cases functionality) —
      none reference group/case-move logic. Re-run the full IT suite once
      more after this fix to confirm `MoveGroupCasesIT` goes green.

**Verified**: artifactory access became available later in the session.
`mvn clean verify` on the full reactor (all 32 modules, including
`progression-event-processor` which hosts `GroupCreatedFromMovedCasesProcessor`)
passes with zero failures. One real bug was found and fixed in the process: a
malformed YAML `...` terminator in `progression_command_api.raml` (wrong
indentation broke the RAML parser) — now matches the working
`/initiate-application` pattern. Generated POJO names/packages
(`MoveGroupCases`, `CurrentGroup`, `NewGroup` in `uk.gov.moj.cpp.progression.command`;
`GroupCreatedFromMovedCases`, `LastCasesToBeMovedFromGroupRejected` in
`uk.gov.moj.cpp.progression.events`) match what the handler/aggregate code
assumed throughout.

## Verification

```bash
mvn test -pl progression-command/progression-command-api,progression-command/progression-command-handler,progression-domain/progression-domain-aggregate
```

For the integration test:
```bash
CPP_DOCKER_DIR=/path/to/cpp-developers-docker ./runIntegrationTests.sh
```
