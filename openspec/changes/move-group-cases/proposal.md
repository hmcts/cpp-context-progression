# Add `progression.move-group-cases` command

## Problem

Case-management needs to split a subset of cases out of an existing civil
case group into a brand new group — e.g. separating defendants who need to
proceed on a different track from the rest of the group. Today progression
has no endpoint for this. The two existing group-mutation endpoints don't
fit:

- `progression.initiate-court-proceedings-for-group-cases` creates brand new
  `ProsecutionCase`/`GroupCaseAggregate` streams from a `CourtReferral` — it
  assumes the cases don't exist yet.
- `progression.remove-case-from-group-cases` removes exactly one case from a
  group and disbands its group membership entirely (forking it a private copy
  of the group's civil fees). It has no notion of "moving to a different,
  still-live group," and no bulk/multi-case vari∂ant.

A case split needs both: bulk removal from the old group, **and** genesis of
a new group aggregate for cases that already exist.

## Scope

Backend only, in `cpp-context-progression`. A new command,
`progression.move-group-cases`, exposed as:

- `POST /prosecutioncases`
- `application/vnd.progression.move-group-cases+json`
- Permissions: System User
- Response: `200 OK` / `4xx` / `5xx`

Payload (as specified by the API definition this change implements):

```json
{
  "currentGroup": {
    "groupId": "uuid",
    "newLeadCaseId": "uuid | absent"
  },
  "newGroup": {
    "groupId": "uuid",
    "caseIds": ["uuid", "..."],
    "leadCaseId": "uuid"
  }
}
```

(`newLeadCaseId` on the current group is required only if the current lead
case is one of the cases being moved; `leadCaseId` on the new group must be
one of `caseIds`.)

The moved cases already exist as `CaseAggregate` streams — this command only
changes group **association**, in the aggregate and the viewstore. It does
not create `ProsecutionCase`s.

## Non-goals

- No change to `remove-case-from-group-cases` or
  `initiate-court-proceedings-for-group-cases` — both kept as-is.
- No new viewstore table. Group membership stays a `group_id` column plus
  `isGroupMaster`/`isGroupMember` flags embedded in each `prosecution_case`
  row's JSON payload (same model `group-cases-query` documents).
- Not scoped here: any change to how case-management itself decides which
  cases to move or who the new lead is — that logic lives upstream; this
  command only executes the split progression-side.

## Approach

1. Validate the payload: `leadCaseId` must be a member of `newGroup.caseIds`;
   `currentGroup.newLeadCaseId` must be present if the current lead is in the
   moved set, absent otherwise (exact 4xx shape: open question, see below).
2. Load the old group's `GroupCaseAggregate` (by `currentGroup.groupId`).
   Remove the moved `caseIds` from its membership in bulk, reassigning
   `groupMaster` to `currentGroup.newLeadCaseId` if given.
3. Create a new `GroupCaseAggregate` stream (by `newGroup.groupId`), seeded
   with `newGroup.caseIds` as members and `newGroup.leadCaseId` as master —
   without creating any new `ProsecutionCase`/`CaseAggregate` streams, since
   the cases already exist.
4. For each moved case, update its own `CaseAggregate`'s group association
   (flags **and** `groupId` — see open question 1) to point at the new group.
5. Resolve civil fees for the new group (see open question 2).
6. Create/list a new hearing for the new group, mirroring the old group's
   *current* session info — resolved via the viewstore (see Open Question 5,
   now resolved) subject to the sync-timing decision in open question 4.
7. Raise `CaseRemovedFromGroupCases`-equivalent event(s) for the old group and
   a genesis event for the new group (see open question 1), so the viewstore
   projection updates both sides.

## Open Questions — Decisions Needed

These surfaced during design exploration and are **not** resolved by this
proposal. Implementation should not proceed past design.md's affected
sections until they're confirmed with whoever owns the API definition and/or
the wider team.

1. **New-group genesis event — DECIDED.** The only event that seeds a
   `GroupCaseAggregate`'s membership today is `CourtProceedingsInitiated`,
   which semantically means "case(s) created." Reusing it here would
   misrepresent already-existing cases as newly created.
   **Confirmed:** a dedicated new event, `GroupCreatedFromMovedCases`,
   separate from `CourtProceedingsInitiated` — carrying `groupId`,
   `caseIds` (every moved case id), and `leadCase` (the full
   `ProsecutionCase` for the new master, so `GroupCaseAggregate.masterCase`
   can be populated the same way `CourtProceedingsInitiated`'s handler
   populates it today). See design.md for the concrete aggregate method and
   `apply()` wiring.
2. **Civil fees — DECIDED: fork-once-per-new-group, not per-case, not a
   no-op inherit.** All members of a group currently share identical
   `feeId`s. Leaving moved cases pointed at the old group's shared fees
   entangles the two groups financially after the split (a fee update on
   one silently affects the other). Cloning a fee per *case* (literally
   reusing `RemoveCaseFromGroupCasesHandler`'s per-case fork) breaks the new
   group's own "all members share identical fee data" invariant.
   **Confirmed:** clone each fee type exactly once at the point of the
   split — sourced from the new lead case's current civil fees — and have
   every moved case reference that one new shared `feeId` set, mirroring
   how `initiate-court-proceedings-for-group-cases` seeds
   `commonCivilFeesForGroup` once for all members. Residual wrinkle, not
   blocking the decision but worth flagging separately to whoever owns
   payment reconciliation: the clone copies `feeStatus`/`paymentReference`
   as-is, so a fee already marked paid before the split would show the same
   payment reference on both the old and new group's records afterward.
   This already happens in the existing remove flow; a group split is a
   more likely trigger for it than a single removal.
3. **Can the old group go empty? — DECIDED: no, guard against it.**
   `GroupCaseAggregateTest` confirms the existing floor for single-case
   removal is "≥1 member," not "≥2": a group of 2 shrinking to 1 is allowed
   (`shouldSetNumberOfGroupCasesToOneWhenMemberRemovedFromGroupOfTwo`), but
   the drop from 1 to 0 is rejected
   (`shouldNotRaiseCaseRemovedEventWhenLastCaseOfGroupIsRejected`) — and that
   rejection isn't just an internal guard, it's republished cross-context as
   `public.progression.remove-last-case-in-group-cases-rejected`
   (`CaseRemoveFromGroupCaseProcessor.java:40`), which case-management is
   evidently built to receive and react to.
   **Confirmed:** `move-group-cases` mirrors this floor-of-1 pattern for the
   old group — a move that would leave `currentGroup` with zero members is
   rejected, not allowed through. See design.md for the concrete guard
   method and rejection event. (The earlier-noted rationale gap — that
   moved cases relocate rather than being orphaned, so this guard's
   original justification doesn't fully transfer — is accepted as a
   deliberate simplicity/consistency choice rather than a gap to resolve
   further: one guard shape, reused, is preferable to two different
   philosophies for "can a case leave a group" depending on where it's
   going.)
4. **Hearing-sync timing — DECIDED: immediate consistency.** Member-case-
   to-hearing association today is a *consequence* of `GroupCaseAggregate`
   membership (the external scheduling/listing system only ever references
   a group's master case; progression fans listing updates out to all
   `memberCases`) — it is not a table that gets edited directly. That
   fan-out only fires on the **next** real `update-defendant-listing-status`
   event, so without proactive work a moved case's `case_defendant_hearing`
   row would stay pointed at the old hearing until something else happens
   to trigger a refresh.
   **Confirmed:** make it immediately consistent — this costs nothing
   extra to design, because it's the exact same mechanism bulk case
   creation already uses on itself: build a `Hearing` for the new group
   (moved cases as `prosecutionCases`, the new `hearingId`, status
   `SENT_FOR_LISTING`) and drive it through
   `HearingAggregate.updateDefendantListingStatus(...)` (the same call
   `CourtProceedingsInitiatedProcessor` makes for brand-new bulk cases),
   which raises `ProsecutionCaseDefendantListingStatusChangedV2/V3` and
   lets `ProsecutionCaseDefendantListingStatusChangedListener` write the
   `case_defendant_hearing` rows immediately — no waiting on the next real
   listing event. (An earlier version of this design pointed at
   `CaseAggregate.updateDefendantListingStatus` instead — a same-named but
   unrelated method that only updates a private, never-queried in-memory
   map on `CaseAggregate`; corrected in design.md.)
5. **Resolving the "current" hearing to mirror — RESOLVED, no payload change
   needed.** Earlier framing of this as "no `hearingId` in the payload, and
   no `groupId → hearingId` index exists" was wrong — that was true only of
   the event-sourced aggregate layer; the viewstore has a usable index. The
   `case_defendant_hearing` table (composite key: `caseId` + `defendantId` +
   `hearingId`, joined to `hearing` for the current `payload`/`listing_status`)
   is populated for **every** case in a group hearing, not just the master —
   confirmed via `ProsecutionCaseDefendantListingStatusChangedListener`
   (writes one row per case/defendant in `hearing.getProsecutionCases()`,
   which is already fanned out to every group member before this listener
   runs). So `CaseDefendantHearingRepository.findByCaseId(caseId)` /
   `HearingAtAGlanceService.getCaseHearings(caseId)`, called with
   `newGroup.leadCaseId` (or any case in `newGroup.caseIds` — they should all
   currently resolve to the same hearing pre-split), returns every hearing
   that case has ever been attached to. The one remaining implementation
   detail (not a blocker): that list includes history, so a selection rule
   is needed to pick the *current* one — filter out `HEARING_RESULTED` (and
   confirm whether any other `HearingListingStatus` values should also be
   excluded) and take the remaining candidate, verifying against real data
   that at most one non-resulted hearing exists per case at a time rather
   than assuming it.
6. **Audit requirement — DECIDED. Existing-`groupId` rejection — still
   open, with a refined recommendation (revised from a simpler earlier
   version — see below).**
   **Confirmed: `Audit Required: No` stands as specified**, no additional
   audit-trail mechanism is needed. Consistent with the only audit
   precedent in this codebase (`AuditCourtDocument`, which requires a human
   `userId` and records user-attributed access to sensitive material) —
   `move-group-cases` is `Permissions: System User`, machine-to-machine,
   with no human actor to attribute an access-audit entry to, and the
   event-sourcing history every command gets already provides the
   underlying trail.
   The API definition's own **`4xx Bad Request (Bad payload / Existing
   Group Id?)`** (written with a question mark by the API's author) is
   **not yet decided**. `InitiateCourtProceedingsForGroupCasesHandler` (the
   sibling group-creation endpoint) has **no check at all** against its
   `groupId` already having stream history — `GroupCaseAggregate.apply()`'s
   `CourtProceedingsInitiated` handler is purely additive, so a colliding
   `groupId` there silently merges cases into whatever group already owns
   it, with no error. That gap already exists in shipped code; the API
   author's `?` is pointing at something real, not a hypothetical.
   **An earlier version of this recommendation was too blunt**: "reject
   `4xx` whenever `newGroup.groupId` already has stream history" would
   also reject a legitimate *retry* of the same request — this command is
   `Permissions: System User`, machine-to-machine, and every other command
   endpoint in this codebase's RAML responds `202` (fire-and-forget), the
   exact context where a caller retrying after a timeout is routine. A
   blanket reject would make a retry of a request that *already succeeded*
   fail forever, every time, since the retry would always find the group
   it itself created.
   **Recommended (not yet confirmed), revised:** distinguish an exact-match
   retry from a genuine collision when `newGroup.groupId` already has
   stream history:
   - If the existing `GroupCaseAggregate`'s `memberCases` already equals
     `newGroup.caseIds` and `groupMaster` already equals
     `newGroup.leadCaseId` — this is the same request landing twice.
     Treat as idempotent: succeed, raise no new events. (Mirrors what
     `CivilCaseExists` already does for case-level resubmission in the
     sibling endpoint — detect "this already happened," don't error.)
   - Otherwise — the existing stream's content differs from what this
     request would produce — reject `4xx`. This is the genuine collision
     case the API author's `?` is actually worried about.
   **Separately worth flagging to the API's author, found while checking
   this:** the API definition specifies `200 OK`, but every one of the
   other 63 command endpoints in `progression_command_api.raml` responds
   `202` (the fire-and-forget pattern this whole command API is built on).
   That's one more sign — alongside the `?` on "Existing Group Id" and the
   `Audit Required: No` — that this API definition may not have been
   drafted with full visibility into this codebase's established command
   conventions.

## Spec

[move-group-cases](../../specs/move-group-cases/spec.md)

## Dependencies

None identified. No downstream UI change is tracked as part of this
proposal, and (per the resolved Open Question 5) no API definition change is
needed either — the current hearing is resolved from the viewstore, not from
a new payload field.
