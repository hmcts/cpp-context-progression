# Design: `progression.move-group-cases`

Status: **draft — several sections below depend on the Open Questions in
`proposal.md` and are sketches, not final designs.** Each section says which
open question(s) it is contingent on.

## Current shape (for reference)

```
GroupCaseAggregate (stream keyed by groupId)
 ├─ memberCases: Set<UUID>
 ├─ groupMaster: UUID
 └─ masterCase: ProsecutionCase

 born via:   CourtProceedingsInitiated   (only event that seeds membership —
                                           assumes case creation)
 shrinks via: CaseRemovedFromGroupCases   (ONE case at a time; auto-picks new
                                           master via getNewGroupMaster() if
                                           the removed case was master;
                                           canBeRemoved() rejects removing the
                                           last case)
```

```
RemoveCaseFromGroupCasesHandler.handle() [progression-command-handler]
  1. CaseAggregate(removedCaseId).updateCaseGroupInfo(false, false)
       → CaseGroupInfoUpdated (flags only — groupId field is carried
         forward UNCHANGED from the existing ProsecutionCase)
  2. for each CivilFees in groupAggregate.getMasterCase().getCivilFees():
       - load existing shared FeeAggregate(feeId)
       - clone it under a NEW feeId → FeeAggregate(newFeeId).addCivilFee(...)
  3. CaseAggregate(removedCaseId).createProsecutionCase(caseWithClonedFees)
  4. if removed case was master: new master's CaseAggregate
       .updateCaseGroupInfo(true, true)   (new master keeps ORIGINAL feeId —
                                            not forked)
  5. GroupCaseAggregate.removeCaseFromGroupCases(...) → CaseRemovedFromGroupCases
```

The viewstore is already `groupId`-aware and needs no change for this
command: `CaseGroupInfoUpdatedEventListener` does
`caseEntity.setGroupId(prosecutionCase.getGroupId())` — it just has never
been asked to write a *different* `groupId`, because `updateCaseGroupInfo`
today only flips the booleans.

## Proposed shape

**Architecture correction, found during implementation**: hearing
resolution (step 6 below) does not belong inside `MoveGroupCasesHandler`.
No command handler in this codebase depends on viewstore-persistence today
— `progression-command-handler`'s `pom.xml` has no such dependency, and
injecting `CaseDefendantHearingRepository` directly into a command handler
would be a genuine CQRS boundary violation (write side reaching into the
read side), something this codebase has never done. The existing precedent
for this exact kind of post-group-creation hearing orchestration is
`CourtProceedingsInitiatedProcessor` — an **event processor**, not the
command handler (`InitiateCourtProceedingsForGroupCasesHandler`) — which is
where `ListingService`/`ReferenceDataService`-style cross-store reads
already live in this codebase. So: `MoveGroupCasesHandler` does steps 1-5
and 7 only (pure aggregate mutation); a new event processor,
`GroupCreatedFromMovedCasesProcessor`, reacting to `GroupCreatedFromMovedCases`,
does step 6. This does not weaken the Q4 "immediate consistency" decision —
bulk creation's own "immediate" `SENT_FOR_LISTING` update already happens via
an event processor (`CourtProceedingsInitiatedProcessor`), not inside its
command handler, so this mirrors existing precedent rather than retreating
from it.

```
MoveGroupCasesHandler  [NEW]  @Handles("progression.command.move-group-cases")

  1. Load old group: GroupCaseAggregate(currentGroup.groupId)
  2. Load new lead case's CaseAggregate — source of fee clone (Q2 —
     decided, see below) and of masterCase for the new group's genesis
  3. OLD GROUP: canMoveCasesOut() guard, then bulk-remove newGroup.caseIds
     from memberCases, reassign groupMaster to currentGroup.newLeadCaseId
     if given                                                   [Q3 — decided]
  4. NEW GROUP: createGroupFromMovedCases(newGroup.groupId,
     newGroup.caseIds, leadCase) → GroupCreatedFromMovedCases   [Q1 — decided]
     (guarded by the "Existing Group Id" check — Open Q6 part 2 — before
     this is called)
  5. FEES: clone each fee type ONCE from the new lead's current CivilFees,
     apply the resulting shared feeId set to every case in newGroup.caseIds
                                                                  [Q2 — decided]
  6. HEARING — moved out of this handler, see
     GroupCreatedFromMovedCasesProcessor below              [Q4/Q5 — decided/resolved]
  7. For each moved case: CaseAggregate.updateCaseGroupInfo(isMaster,
     isMember, newGroupId)  — extended signature, see below      [Q1 — decided]
```

## Aggregate changes

### `GroupCaseAggregate` — bulk removal (old group side)

**File**: `progression-domain/progression-domain-aggregate/.../aggregate/GroupCaseAggregate.java`

Today's `removeCaseFromGroupCases(groupId, removedCase, newGroupMaster)`
(lines 89–106) takes one case and recomputes `numberOfGroupCasesAfterRemoving`
for exactly that one removal. This command needs a bulk variant — a new
method, not a loop over the existing one, because:

- the existing method's `canBeRemoved`/last-case guard is single-case
  shaped and needs a bulk equivalent — `canMoveCasesOut`, see "Old-group-
  empty handling" below (Open Question 3 — now decided: guard against it);
- `newGroupMaster` here is explicitly supplied by case-management
  (`currentGroup.newLeadCaseId`), not auto-picked by `getNewGroupMaster()` —
  only needed if the auto-picked master itself is also among the moved
  cases.

Sketch (name tentative), with retry-tolerance mirroring
`removeCaseFromGroupCases`'s existing per-case idempotency check
(`!this.memberCases.contains(removedCase.getId())` → no-op) — a retried
`move-group-cases` request must not fail just because a prior attempt
already removed some or all of `movedCaseIds` from `memberCases` (Open
Question 6, part 2 — see the refined discussion in proposal.md):

```java
public Stream<Object> moveCasesToNewGroup(final UUID groupId,
                                           final Set<UUID> movedCaseIds,
                                           final ProsecutionCase newGroupMaster) {
    final Set<UUID> stillMembers = movedCaseIds.stream()
            .filter(this.memberCases::contains)
            .collect(toSet());
    if (stillMembers.isEmpty()) {
        // every moved case was already removed by a prior attempt — no-op,
        // not an error, same as removeCaseFromGroupCases's own idempotency
        return empty();
    }
    // bulk analogue of removeCaseFromGroupCases — raises one event per
    // moved case, or a single bulk event, for stillMembers only; TBD at
    // implementation time.
}
```

### `GroupCaseAggregate` — new group genesis (Open Question 1 — DECIDED)

No existing method seeds membership without `CourtProceedingsInitiated`.
Decided: a new event, `GroupCreatedFromMovedCases`, and a new aggregate
method, mirroring exactly what `CourtProceedingsInitiated`'s handler
populates today (`groupCases`, `memberCases`, `groupMaster`, `masterCase`)
but without implying case creation:

```java
public Stream<Object> createGroupFromMovedCases(final UUID groupId,
                                                  final List<UUID> caseIds,
                                                  final ProsecutionCase leadCase) {
    return apply(Stream.of(GroupCreatedFromMovedCases
            .groupCreatedFromMovedCases()
            .withGroupId(groupId)
            .withCaseIds(caseIds)
            .withLeadCase(leadCase)
            .build()));
}
```

`apply()` wiring, added alongside the existing `CourtProceedingsInitiated`/
`CaseRemovedFromGroupCases` cases in `GroupCaseAggregate.apply()`:

```java
when(GroupCreatedFromMovedCases.class).apply(e -> {
    this.groupCases.addAll(e.getCaseIds());
    this.memberCases.addAll(e.getCaseIds());
    this.groupMaster = e.getLeadCase().getId();
    this.masterCase = e.getLeadCase();
}),
```

`leadCase` carries the full `ProsecutionCase` (fetched by `MoveGroupCasesHandler`
from the lead's own `CaseAggregate`, same as how the handler already needs
to load it for the fee-cloning step) rather than just an id, so `masterCase`
is populated consistently with how `CourtProceedingsInitiated`'s handler
populates it — this matters because `removeCaseFromGroupCases`'s fee-fork
logic reads `groupAggregate.getMasterCase().getCivilFees()` elsewhere in
this codebase, so leaving `masterCase` null on a freshly-created group would
be a latent trap for any future feature that assumes it's always populated,
even though this move-group-cases flow itself sources fees from the lead
case's `CaseAggregate` directly rather than through `masterCase`.

This event needs no new event-processor or viewstore listener. It's
intra-aggregate bookkeeping only — same as `CaseRemovedFromGroupCases`
today, which doesn't drive any viewstore write itself either; the viewstore
already gets the correct `groupId` per case from each moved case's own
`CaseGroupInfoUpdated` event (see the `CaseAggregate.updateCaseGroupInfo`
change below), not from the group-level event.

The "Existing Group Id" guard (Open Question 6, part 2 — recommended, not
yet confirmed; revised from a simpler earlier version) belongs in
`MoveGroupCasesHandler`, before calling this method — not inside
`createGroupFromMovedCases` itself, consistent with how
`verifyCaseCreationEvent`'s equivalent check lives in the handler for the
sibling endpoint, not in `GroupCaseAggregate`. A blanket "stream already
has history → reject" was rejected as too blunt: this command is
`Permissions: System User` over a fire-and-forget-style command API (every
other command endpoint in this RAML responds `202`), so a caller retrying
after a timeout is routine — and a blanket reject would make a retry of an
already-succeeded request fail forever, every time, since the retry always
finds the group it itself created.

```java
final EventStream newGroupStream = eventSource.getStreamById(newGroup.getGroupId());
if (newGroupStream.size() > 0) {
    final GroupCaseAggregate existing = aggregateService.get(newGroupStream, GroupCaseAggregate.class);
    final boolean isExactRetry = existing.getMemberCases().equals(new HashSet<>(newGroup.getCaseIds()))
            && Objects.equals(existing.getGroupMaster(), newGroup.getLeadCaseId());
    if (isExactRetry) {
        // same request landing twice — succeed, raise no new events,
        // mirroring how CivilCaseExists handles case-level resubmission
        // in the sibling endpoint
        return;
    }
    // genuine collision: an unrelated, already-live group owns this id
    throw new BadRequestException("groupId already in use by a different group");
}
```

### `CaseAggregate.updateCaseGroupInfo` — needs a `groupId` parameter

**File**: `progression-domain/progression-domain-aggregate/.../aggregate/CaseAggregate.java:4194`

Today:

```java
public Stream<Object> updateCaseGroupInfo(final Boolean isGroupMaster, final Boolean isGroupMember) {
    return apply(Stream.of(CaseGroupInfoUpdated.caseGroupInfoUpdated()
            .withProsecutionCase(ProsecutionCase.prosecutionCase()
                    .withValuesFrom(this.prosecutionCase)
                    .withIsGroupMember(isGroupMember)
                    .withIsGroupMaster(isGroupMaster)
                    .build())
            .build()));
}
```

`withValuesFrom(this.prosecutionCase)` carries the *existing* `groupId`
forward unchanged — correct for "leaving a group" (where `groupId` becoming
stale doesn't matter because `isGroupMember` is false), wrong for "moving to
a different group" (where the viewstore must show the *new* `groupId`).
Proposed: overload or extend the method to accept the target `groupId` and
pass it through via `.withGroupId(groupId)`. For the `prosecution_case`
row itself, no further downstream change is needed —
`CaseGroupInfoUpdatedEventListener.getProsecutionCaseEntity` writes both
the dedicated `group_id` column and the full JSON `payload` from the same
`ProsecutionCase` object the event carries, so passing the new `groupId`
through at the aggregate level fixes both representations in one place.
(There is a separate, unrelated side effect of this same listener that
does need attention — see "Risk: stale case data in a hearing's cached
payload," below.)

**Backward compatibility**: `RemoveCaseFromGroupCasesHandler`'s two existing
call sites (lines 62, 94 of `RemoveCaseFromGroupCasesHandler.java`) must
keep compiling/behaving identically — either add an overload that defaults
to the existing groupId-preserving behaviour, or pass the existing
`groupId` explicitly at both call sites. Judgment call at implementation
time, not a design blocker.

### Risk: stale case data in a hearing's cached payload (new, not one of the six numbered Open Questions)

Found while confirming the `group_id` fix above is sufficient —
`CaseGroupInfoUpdatedEventListener` does a second thing beyond writing the
`prosecution_case` row: it also patches the moved case's entry inside its
*hearing's own cached JSON* (`HearingEntity.payload`), so a hearing's
stored `prosecutionCases[]` list stays in sync with each case's latest
group flags:

```java
final List<CaseDefendantHearingEntity> caseDefendantHearingEntities =
        caseDefendantHearingRepository.findByCaseId(caseGroupInfoUpdated.getProsecutionCase().getId());
caseDefendantHearingEntities.stream().forEach(caseDefendantHearingEntity ->
        hearingEntity.set(caseDefendantHearingEntity.getHearing())
);
updateHearingWithCaseGroupInfo(hearingEntity.get(), caseGroupInfoUpdated);
```

This picks "the" hearing for the case with **no filtering or ordering at
all** — a plain `forEach` that keeps overwriting `hearingEntity`, so
whichever row `findByCaseId` happens to return last wins, arbitrarily.
That's harmless today because, outside this proposal, a case normally has
only one *live* `case_defendant_hearing` row at a time. This proposal
breaks that assumption: by the time `CaseGroupInfoUpdated` fires for a
moved case, it has **at least two** rows — the old hearing's (nothing in
this design deletes it) and the brand-new hearing's (created earlier in
the same command, per "Hearing mirroring" above). Which one this listener
picks is not controlled by this design at all; it depends on whatever
order `findByCaseId` happens to return rows in.

Two ways this can go wrong, depending on that arbitrary pick:
- It patches the **old** hearing's cached payload with the case's *new*
  group data — leaving a case that just left the group still appearing,
  with the wrong group's data, inside a hearing it's no longer part of.
- It patches the **new** hearing correctly, but the **old** hearing's
  cached payload keeps showing the case with its *pre-move* group data
  forever — nothing in this flow ever re-patches the old hearing once the
  case has moved on.

**Not yet resolved.** Candidate directions, none chosen: (a) have
`updateHearingWithCaseGroupInfo` patch *every* hearing `findByCaseId`
returns, not just the last one iterated; (b) make the pick
content-aware — only patch the hearing matching the case's *current*
`groupId`/listing status, mirroring the `HEARING_RESULTED`-exclusion
filter already used elsewhere in this design; (c) decide whether stale
`case_defendant_hearing` rows for a hearing a case has moved away from
should be removed entirely once the move completes, rather than left to
accumulate (note: `CaseDefendantHearingRepository.removeByHearingIdAndCaseId`
already exists and does exactly this kind of removal, just not called from
this flow). This is a pre-existing fragility in `CaseGroupInfoUpdatedEventListener`
that move-group-cases is the first feature to actually expose, not
something introduced by this proposal's own code.

## RAML

**File**: `progression-command/progression-command-api/src/raml/progression_command_api.raml`

No bare `/prosecutioncases` resource exists today (only
`/prosecutioncases/{prosecutionCaseId}` and other path-parameterised
variants). New resource:

```raml
/prosecutioncases:
  post:
      description: |
        Cases are moved from one group into a new group.
        ...
        (mapping):
            requestType: application/vnd.progression.move-group-cases+json
            name: progression.move-group-cases
      body:
        application/vnd.progression.move-group-cases+json:
            example:
             !include json/progression.move-group-cases.json
            schema:
             !include json/schema/progression.move-group-cases.json
      responses:
            200:
                description: OK
```

New schema/example pair, `progression.move-group-cases.json`, matching the
payload shape in `proposal.md` — `currentGroup.newLeadCaseId` optional,
`newGroup.leadCaseId` required and constrained (informally, since RAML/JSON
Schema draft-04 has no cross-field "`leadCaseId` must be in `caseIds`"
constraint) to be one of `newGroup.caseIds` — that constraint is enforced in
the handler, not the schema, same pattern as other cross-field validations
in this codebase.

**Note on the `200`**: kept here matching the source API definition exactly,
but flagged (Open Question 6, part 2 in proposal.md) as worth raising with
the API's author — every other command endpoint in this RAML (all 63 of
them) responds `202`, the fire-and-forget pattern this command API is built
on. Not changed unilaterally here since it's the API definition's call, not
an implementation detail to silently correct.

## Civil fees — decided flow (Open Question 2 — DECIDED)

```
Old group                    New group (B, C moved; lead = B)
┌─────────┐                  ┌─────────┐   ┌─────────┐
│ Case A  │                  │ Case B  │   │ Case C  │
│ (stays) │                  │ (lead)  │   │(member) │
└────┬────┘                  └────┬────┘   └────┬────┘
     │ feeId: F1 (unchanged)      │ feeId: F1' (new, shared)
     ▼                            └───────┬───────┘
FeeAggregate(F1)                          ▼
  (untouched)                    FeeAggregate(F1') — ONE clone per fee
                                  type, sourced from B's (the new lead's)
                                  current CivilFees at split time, applied
                                  identically to B and C.
```

Mirrors `InitiateCourtProceedingsForGroupCasesHandler.raiseCaseCreationEvent`'s
`commonCivilFeesForGroup` pattern (fee list computed once, applied to every
member) rather than `RemoveCaseFromGroupCasesHandler`'s per-case fork.

This is now the confirmed implementation approach, not a sketch to validate.
The residual `feeStatus`/`paymentReference` carry-over wrinkle (same payment
reference ending up on both the old and new group's cloned fee records if a
fee was already marked paid pre-split) is noted in proposal.md as worth
flagging to whoever owns payment reconciliation, but does not block this
design.

## Hearing mirroring — decided flow (Open Question 4 — DECIDED; Open Question 5 resolved)

**Runs in `GroupCreatedFromMovedCasesProcessor` (new `EVENT_PROCESSOR`),
reacting to `GroupCreatedFromMovedCases` — not inside `MoveGroupCasesHandler`.**
See "Architecture correction" at the top of this document: no command
handler in this codebase reads the viewstore, and this is exactly the kind
of orchestration `CourtProceedingsInitiatedProcessor` already does for
bulk case creation's own hearing handling. This processor injects
`HearingAtAGlanceService`/`CaseDefendantHearingRepository` the same way
`CourtProceedingsInitiatedProcessor` injects `ListingService`/`ReferenceDataService`
— a normal event-processor dependency, not a new architectural pattern.

```
1. Resolve old group's current hearing          [Open Question 5 — RESOLVED]
     caseDefendantHearingRepository.findByCaseId(newGroup.leadCaseId)
       — or HearingAtAGlanceService.getCaseHearings(leadCaseId); both
         already live in progression-query-view / progression-viewstore-
         persistence, the normal home for a query-side dependency used from
         an event processor
       → returns every HearingEntity ever linked to that case (one row per
         case+defendant+hearing — already includes every member case of a
         group, not just the master: confirmed via
         ProsecutionCaseDefendantListingStatusChangedListener, which writes
         a row for every case in hearing.getProsecutionCases(), already
         fanned out to all group members before this listener runs)
     filter out listingStatus == HEARING_RESULTED (confirm no other
       terminal HearingListingStatus values need excluding too), take the
       remaining hearing
     — verify against real data that at most one non-resulted hearing
       exists per case at a time; this sketch assumes so but it is not
       independently confirmed

     Nuance: SENT_FOR_LISTING is a valid "current" status here, not a
     terminal one to exclude. The hearingId is minted by progression itself,
     not by the listing context — CourtProceedingsInitiatedProcessor.
     generateHearingListing() (progression-event-processor/.../processor/
     CourtProceedingsInitiatedProcessor.java:150-201) assigns a fresh
     randomUUID() per hearing grouping at bulk-creation time, threads it
     through HearingListingNeeds.withId(...), and progression persists the
     case_defendant_hearing/hearing viewstore rows for it immediately
     (status SENT_FOR_LISTING, via the same private
     progression.command.update-defendant-listing-status ->
     ProsecutionCaseDefendantListingStatusChangedListener path) — all of
     this happens before listing.command.list-court-hearing is even sent
     out, and well before any public.listing.hearing-confirmed comes back.
     So a move-group-cases split that happens between bulk creation and
     listing's eventual confirmation will resolve a hearing that is itself
     still SENT_FOR_LISTING, not yet HEARING_INITIATED/HEARING_RESULTED —
     the session info being mirrored in that case is progression's own
     as-yet-unconfirmed submission, not something listing has agreed to
     yet. That's consistent with the rest of this design (mirror whatever
     progression currently holds, not case-management's stale copy), but
     worth being explicit that "current" can legitimately mean "pending,"
     not just "confirmed."
2. Read that Hearing's CURRENT courtCentre/hearingDays
     — the resolved HearingEntity.payload (viewstore) should already be
       in sync with HearingAggregate.hearing (both kept current via
       ProsecutionCaseDefendantListingStatusChangedV2/V3 events) — NOT
       whatever CourtReferral/hearing payload this command might carry.
3. Create a new Hearing aggregate for the new group via the same mechanism
   ListNewHearingHandler uses (HearingAggregate.listNewHearing), seeded with
   the cloned session info — a new, independent hearingId (the "only one
   group per hearing" constraint means this cannot be the same hearing).
4. Proactively sync each moved case's own current-hearing record — decided
   (Open Question 4), not left to eventual consistency; see below for the
   mechanism, and for the correction to an earlier mistake in this sketch.
```

### Correction: the earlier "proactive sync" sketch pointed at the wrong method

This design previously named `CaseAggregate.updateDefendantListingStatus(hearingId,
courtCentreId, hearingDays)` as the proactive-sync call. That was wrong —
tracing what it actually does:

```java
// CaseAggregate.java:461-465
when(HearingRequestStatusUpdated.class).apply(e ->
        this.hearingStatusByKey.put(
                buildHearingKey(e.getCourtCentreId(), e.getHearingDateTime()),
                ImmutablePair.of(e.getHearingId(), e.getHearingRequestStatus())
        )
),
```

`hearingStatusByKey` is a private, in-memory map on `CaseAggregate`. No event
listener anywhere subscribes to `HearingRequestStatusUpdated` (confirmed —
zero matches outside this one `apply()` block) — it is never projected to
the viewstore and never queryable. It looks like internal dedup bookkeeping,
not "what hearing does this case currently show as attached to." Calling it
would sync a value nobody can see.

The method that actually matters is a same-named-but-unrelated method on a
**different aggregate**: `HearingAggregate.updateDefendantListingStatus(hearing,
hearingListingStatus, notifyNCES, listHearingRequests)` — the one
`UpdateDefendantListingStatusHandler` calls, which raises
`ProsecutionCaseDefendantListingStatusChangedV2/V3`, which is the *only*
thing `ProsecutionCaseDefendantListingStatusChangedListener` reacts to when
writing `case_defendant_hearing` rows (one per case/defendant in
`hearing.getProsecutionCases()`). That is the real source of truth queries
read from — not `CaseAggregate`'s internal map.

```
WRONG (previous sketch):
  CaseAggregate.updateDefendantListingStatus(...)
        → HearingRequestStatusUpdated → hearingStatusByKey (private, unqueried)

RIGHT (what actually populates case_defendant_hearing):
  HearingAggregate.updateDefendantListingStatus(hearing, status, ..., listHearingRequests)
        → ProsecutionCaseDefendantListingStatusChangedV2/V3
        → ProsecutionCaseDefendantListingStatusChangedListener
        → case_defendant_hearing rows written, one per (case, defendant)
          in hearing.getProsecutionCases()
```

Decided step 4: `GroupCreatedFromMovedCasesProcessor` builds a `Hearing`
object for the new group (the moved cases as `prosecutionCases`, the new
`hearingId`, `hearingListingStatus = SENT_FOR_LISTING`) and sends the
private `progression.command.update-defendant-listing-status` command — the
exact same command `CourtProceedingsInitiatedProcessor` sends for brand-new
bulk case creation, handled by the existing `UpdateDefendantListingStatusHandler`,
which drives `HearingAggregate.updateDefendantListingStatus(...)`. This is
not new capability to build; it is reusing an already-established command
with different inputs (moved cases and a new hearingId instead of
newly-created cases) — which is exactly why immediate consistency was
chosen over eventual consistency: it costs nothing extra to implement, and
it keeps the command-handler/event-processor boundary exactly where this
codebase already draws it.

**Gap found and fixed while writing the integration test**: the command's
`listHearingRequests` field is not cosmetic. It flows through
`HearingAggregate.updateDefendantListingStatus(...)` into
`ProsecutionCaseDefendantListingStatusChangedV2`, which
`ProsecutionCaseDefendantListingStatusChangedProcessor` only forwards to
`ListingService.listCourtHearing(...)` (the actual outbound call that tells
the listing context a hearing exists) when
`!CollectionUtils.isEmpty(listHearingRequests)`. The processor originally
built its command with only `hearing` and `hearingListingStatus` — leaving
`listHearingRequests` absent — so the new hearing would have been created
*only* inside progression's own store, with listing never told about it.
Fixed by deriving a single `ListHearingRequest` from the mirrored hearing's
own session info (court centre, hearing type, the first `HearingDay`'s
sitting time/duration) and one `ListDefendantRequest` per moved defendant,
and including it in the command. One known, accepted imprecision from this:
the new hearing's `numberOfGroupCases` is carried over as-is from the old
hearing via `withValuesFrom(oldHearing)` (the old group's total, not the new
group's), the same staleness-until-listing-confirms pattern the existing
`shouldInitiateHearingWithConfirmedNumberOfGroupCasesWhenCaseRemovedBeforeAllocation`
test already documents for brand-new groups — listing's own confirmation is
what corrects it, not progression's initial guess.

## Old-group-empty handling (Open Question 3 — DECIDED: guard against it)

**The existing precedent** (`GroupCaseAggregateTest`): single-case removal's
floor is "≥1 member," not "≥2" — shrinking a group of 2 to 1 is allowed, but
removing the last remaining case is rejected via `canBeRemoved()` returning
`false`, raising `LastCaseToBeRemovedFromGroupCasesRejected`, republished
cross-context as `public.progression.remove-last-case-in-group-cases-rejected`
(`CaseRemoveFromGroupCaseProcessor.java:40`) for case-management to react to.

**Decided design, mirroring that precedent:**

```java
// GroupCaseAggregate — bulk analogue of canBeRemoved()
public boolean canMoveCasesOut(final Set<UUID> caseIdsToMove) {
    final long remaining = this.memberCases.stream()
            .filter(id -> !caseIdsToMove.contains(id))
            .count();
    return remaining > 0;
}
```

`MoveGroupCasesHandler` checks this before mutating anything. If `false`:

```java
public Stream<Object> rejectLastCasesToBeMovedFromGroup(final UUID groupId, final Set<UUID> caseIds) {
    return apply(Stream.of(LastCasesToBeMovedFromGroupRejected
            .lastCasesToBeMovedFromGroupRejected()
            .withGroupId(groupId)
            .withCaseIds(caseIds)
            .build()));
}
```

— a new event, bulk-shaped analogue of `LastCaseToBeRemovedFromGroupCasesRejected`.
It needs a processor method (alongside, or added to,
`CaseRemoveFromGroupCaseProcessor`) republishing it cross-context as
`public.progression.move-last-cases-from-group-rejected`, so
case-management's existing pattern for reacting to the single-case
rejection can be reused rather than inventing a new integration shape.

If `canMoveCasesOut` returns `true`, the bulk removal proceeds as already
designed (`moveCasesToNewGroup`, above) — and because the guard makes an
empty `memberCases` result impossible, the bulk removal method never needs
to handle clearing a dangling `groupMaster`; that's simply an unreachable
state by construction, not something defended against defensively.

## Audit (Open Question 6, part 1 — DECIDED)

`Audit Required: No` stands as specified — no `AuditCourtDocument`-style
mechanism is needed. That mechanism requires a human `userId` from the
envelope metadata and records user-attributed access to sensitive material;
`move-group-cases` is `Permissions: System User`, machine-to-machine, with
no human actor to attribute an entry to. The event-sourcing history every
command in this aggregate model gets already provides the underlying trail.
No implementation task needed for this.

## Validation / 4xx cases

- `leadCaseId` not present in `caseIds` → 4xx (clear-cut, not an open
  question).
- `currentGroup.newLeadCaseId` absent while the current lead is among the
  moved cases → 4xx (clear-cut).
- Move would leave `currentGroup` with zero members →
  `canMoveCasesOut()` returns `false`, rejected (Q3 — decided; see
  "Old-group-empty handling" above).
- `newGroup.groupId` already has stream history, with *different* content
  than this request would produce → genuine collision, `4xx`. If it has
  stream history with the *same* `memberCases`/`groupMaster` this request
  would produce → treat as an idempotent retry, succeed, raise nothing new
  (see "Existing Group Id" guard, above). Still open (Open Question 6,
  part 2) pending team confirmation, but no longer a bare unknown — see
  proposal.md for why the earlier simpler "reject on any collision"
  version was revised.

## Test coverage to add (once open questions are resolved)

Following the precedent of `RemoveCaseFromGroupCasesHandlerTest` /
`GroupCaseAggregateTest` / `CourtProceedingsInitiatedForGroupCasesIT`:

- `GroupCaseAggregateTest`: bulk removal leaves correct `memberCases`;
  correct `groupMaster` reassignment when given explicitly; genesis of a new
  aggregate from already-existing cases (no `ProsecutionCaseCreated`-style
  side effects); `canMoveCasesOut()` returns `false` and
  `rejectLastCasesToBeMovedFromGroup` raises `LastCasesToBeMovedFromGroupRejected`
  when every current member is in the moved set — mirroring
  `shouldNotRaiseCaseRemovedEventWhenLastCaseOfGroupIsRejected`.
- `MoveGroupCasesHandlerTest` (new): fee cloning happens once and is shared
  across all moved cases, not per-case; old group's untouched members keep
  their original `feeId`; hearing creation delegates to the right aggregate
  method; viewstore-bound event carries the new `groupId`; a retried request
  against a `newGroup.groupId` that already matches the exact
  `memberCases`/`groupMaster` this request would produce succeeds with no
  new events raised; the same retried request against a `newGroup.groupId`
  whose existing content *differs* is rejected `4xx`; a retry where the old
  group's `movedCaseIds` were already removed by a prior attempt is a no-op,
  not a rejection.
- `MoveGroupCasesIT` (new, or extend `CourtProceedingsInitiatedForGroupCasesIT`-
  style): end-to-end split, asserting both groups' `group-cases` query
  results (via the existing `progression.query.group-cases` from
  `group-cases-query`) are correct and mutually exclusive.
