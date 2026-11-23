package uk.gov.moj.cpp.progression.aggregate;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Stream.empty;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.core.courts.CourtProceedingsInitiated;
import uk.gov.justice.core.courts.CourtReferral;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.moj.cpp.progression.events.CaseRemovalFromGroupCasesRejected;
import uk.gov.moj.cpp.progression.events.CaseRemovedFromGroupCases;
import uk.gov.moj.cpp.progression.events.CivilCaseExists;
import uk.gov.moj.cpp.progression.events.GroupCreatedFromMovedCases;
import uk.gov.moj.cpp.progression.events.LastCaseToBeRemovedFromGroupCasesRejected;
import uk.gov.moj.cpp.progression.events.LastCasesToBeMovedFromGroupRejected;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
public class GroupCaseAggregate implements Aggregate {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupCaseAggregate.class);
    private static final long serialVersionUID = 101L;

    private final Set<UUID> groupCases = new HashSet<>();
    private final Set<UUID> memberCases = new HashSet<>();
    private UUID groupMaster;
    private ProsecutionCase masterCase;

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(CourtProceedingsInitiated.class).apply(e ->
                        e.getCourtReferral().getProsecutionCases().forEach(pCase -> {
                            if (nonNull(pCase.getGroupId())) {
                                final UUID caseId = pCase.getId();
                                this.groupCases.add(caseId);
                                this.memberCases.add(caseId);
                                if (nonNull(pCase.getIsGroupMaster()) && pCase.getIsGroupMaster()) {
                                    this.groupMaster = caseId;
                                    this.masterCase = pCase;
                                }
                            }
                        })
                ),
                when(CaseRemovedFromGroupCases.class).apply(e -> {
                    this.memberCases.remove(e.getRemovedCase().getId());
                    if (nonNull(e.getNewGroupMaster())) {
                        this.groupMaster = e.getNewGroupMaster().getId();
                        this.masterCase = e.getNewGroupMaster();
                    }
                }),
                when(GroupCreatedFromMovedCases.class).apply(e -> {
                    this.groupCases.addAll(e.getCaseIds());
                    this.memberCases.addAll(e.getCaseIds());
                    this.groupMaster = e.getLeadCase().getId();
                    this.masterCase = e.getLeadCase();
                }),
                otherwiseDoNothing()
        );
    }

    public Stream<Object> initiateCourtProceedings(final CourtReferral courtReferral) {
        LOGGER.info("Court Proceedings being initiated");
        return apply(Stream.of(CourtProceedingsInitiated.courtProceedingsInitiated().withCourtReferral(courtReferral).build()));
    }

    public Stream<Object> raiseGroupCaseExists(final UUID groupId, final ProsecutionCase prosecutionCase) {

        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCase.getProsecutionCaseIdentifier();
        LOGGER.info("Raise Civil Case Exists event for URN = {} with prosecutionCaseId = {}", prosecutionCaseIdentifier.getCaseURN(), prosecutionCase.getId());
        return apply(
                Stream.of(
                        CivilCaseExists.civilCaseExists()
                                .withGroupId(groupId)
                                .withCaseUrn(prosecutionCaseIdentifier.getCaseURN())
                                .withProsecutionCaseId(prosecutionCase.getId())
                                .build()
                )
        );

    }

    public Stream<Object> removeCaseFromGroupCases(final UUID groupId, final ProsecutionCase removedCase, final ProsecutionCase newGroupMaster) {
        if (isNull(removedCase)) {
            LOGGER.info("Null cannot be removed from group.");
            return empty();
        } else if (!this.memberCases.contains(removedCase.getId())) {
            LOGGER.info("Case with id {} already removed from group.", removedCase.getId());
            return empty();
        } else {
            return apply(Stream.of(CaseRemovedFromGroupCases
                    .caseRemovedFromGroupCases()
                    .withGroupId(groupId)
                    .withMasterCaseId(groupMaster)
                    .withRemovedCase(removedCase)
                    .withNewGroupMaster(newGroupMaster)
                    .withNumberOfGroupCases(numberOfGroupCasesAfterRemoving(removedCase.getId()))
                    .build()));
        }
    }

    /**
     * Number of cases left in the group once {@code removedCaseId} is removed. {@code memberCases} holds
     * every case of the group, the master included, and a new master is already a member.
     */
    private int numberOfGroupCasesAfterRemoving(final UUID removedCaseId) {
        return (int) this.memberCases.stream()
                .filter(caseId -> !caseId.equals(removedCaseId))
                .count();
    }

    /**
     * Whether {@code caseIdsToMove} can be moved out of this group without leaving it with zero
     * members — the bulk analogue of {@link #canBeRemoved}, generalised from "not the last case" to
     * "not every remaining case".
     */
    public boolean canMoveCasesOut(final Set<UUID> caseIdsToMove) {
        final long remaining = this.memberCases.stream()
                .filter(caseId -> !caseIdsToMove.contains(caseId))
                .count();
        return remaining > 0;
    }

    /**
     * Moves {@code movedCases} out of this group, one {@link CaseRemovedFromGroupCases} event per
     * case still actually a member — reusing {@link #removeCaseFromGroupCases}'s own idempotency
     * check, so a case already moved by a prior attempt is silently skipped rather than re-raising
     * an event for it. {@code newGroupMaster} is only attached to the event for whichever moved case
     * is the current master at the point it's processed; other moved cases don't reassign it.
     * Callers must check {@link #canMoveCasesOut} first — this method does not guard against leaving
     * the group empty.
     */
    public Stream<Object> moveCasesToNewGroup(final UUID groupId, final List<ProsecutionCase> movedCases, final ProsecutionCase newGroupMaster) {
        final Stream.Builder<Object> events = Stream.builder();
        for (final ProsecutionCase movedCase : movedCases) {
            final boolean isCurrentMaster = movedCase.getId().equals(this.groupMaster);
            removeCaseFromGroupCases(groupId, movedCase, isCurrentMaster ? newGroupMaster : null)
                    .forEach(events::add);
        }
        return events.build();
    }

    public Stream<Object> rejectLastCaseToBeRemovedFromGroup(final UUID groupId, final UUID removedCaseId) {
        return apply(Stream.of(LastCaseToBeRemovedFromGroupCasesRejected
                .lastCaseToBeRemovedFromGroupCasesRejected()
                .withGroupId(groupId)
                .withCaseId(removedCaseId)
                .build()));
    }

    /**
     * Bulk analogue of {@link #rejectLastCaseToBeRemovedFromGroup} — raised when
     * {@link #canMoveCasesOut} returns {@code false}, i.e. the requested move would leave this group
     * with zero members.
     */
    public Stream<Object> rejectLastCasesToBeMovedFromGroup(final UUID groupId, final Set<UUID> caseIds) {
        return apply(Stream.of(LastCasesToBeMovedFromGroupRejected
                .lastCasesToBeMovedFromGroupRejected()
                .withGroupId(groupId)
                .withCaseIds(new ArrayList<>(caseIds))
                .build()));
    }

    /**
     * Seeds a brand-new group from cases that already exist (as {@code CaseAggregate} streams) —
     * unlike {@link #initiateCourtProceedings}, this does not imply case creation. Callers must
     * guard against {@code groupId} already having stream history before calling this (see
     * {@code MoveGroupCasesHandler} — the "Existing Group Id" check).
     */
    public Stream<Object> createGroupFromMovedCases(final UUID groupId, final List<UUID> caseIds, final ProsecutionCase leadCase) {
        return apply(Stream.of(GroupCreatedFromMovedCases
                .groupCreatedFromMovedCases()
                .withGroupId(groupId)
                .withCaseIds(caseIds)
                .withLeadCase(leadCase)
                .build()));
    }

    public Stream<Object> rejectCaseRemovalFromGroup(final UUID groupId, final UUID removedCaseId,
                                                     final UUID requestedNewGroupMaster, final String reason) {
        return apply(Stream.of(CaseRemovalFromGroupCasesRejected
                .caseRemovalFromGroupCasesRejected()
                .withGroupId(groupId)
                .withCaseId(removedCaseId)
                .withRequestedNewLeadCaseId(requestedNewGroupMaster)
                .withReason(reason)
                .build()));
    }

    /**
     * True when the caller asked for a new group master that cannot be used: the group master is being
     * removed and the requested case is the removed case itself or is not a member of the group.
     * A requested master is ignored when the removed case is not the group master.
     */
    public boolean isInvalidNewGroupMaster(final UUID removedCaseId, final UUID requestedNewGroupMaster) {
        return removedCaseId.equals(this.groupMaster)
                && nonNull(requestedNewGroupMaster)
                && (requestedNewGroupMaster.equals(removedCaseId) || !this.memberCases.contains(requestedNewGroupMaster));
    }

    /**
     * Returns the case that becomes the group master when {@code removedCaseId} is removed, or null
     * when the removed case is not the current master.
     *
     * @param removedCaseId the case being removed from the group
     * @param requestedNewGroupMaster the new master chosen by the caller (case management), may be null.
     *                                Check it with {@link #isInvalidNewGroupMaster} first. When null, the
     *                                first remaining member is used.
     */
    public UUID getNewGroupMaster(final UUID removedCaseId, final UUID requestedNewGroupMaster) {
        if (!removedCaseId.equals(this.groupMaster)) {
            return null;
        }
        if (nonNull(requestedNewGroupMaster)) {
            return requestedNewGroupMaster;
        }
        return this.memberCases.stream()
                .filter(member -> !(member.equals(this.groupMaster)))
                .findFirst()
                .orElse(null);
    }

    public boolean canBeRemoved(final UUID caseId) {
        return this.memberCases.contains(caseId)
                && this.memberCases.size() > 1;
    }

    public Set<UUID> getMemberCases() {
        return Collections.unmodifiableSet(memberCases);
    }

    public UUID getGroupMaster() {
        return groupMaster;
    }

    public ProsecutionCase getMasterCase() {
        return masterCase;
    }
}
