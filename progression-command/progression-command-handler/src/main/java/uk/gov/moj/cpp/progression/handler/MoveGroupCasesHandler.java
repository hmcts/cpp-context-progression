package uk.gov.moj.cpp.progression.handler;

import static java.util.Objects.equals;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.core.courts.CivilFees;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.FeeAggregate;
import uk.gov.moj.cpp.progression.aggregate.GroupCaseAggregate;
import uk.gov.moj.cpp.progression.command.CurrentGroup;
import uk.gov.moj.cpp.progression.command.MoveGroupCases;
import uk.gov.moj.cpp.progression.command.NewGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Splits a subset of cases out of an existing group into a brand new group. The moved cases
 * already exist as {@code CaseAggregate} streams — this handler only changes group association,
 * never case creation. Hearing mirroring for the new group does not happen here: it is handled by
 * {@code GroupCreatedFromMovedCasesProcessor}, an event processor reacting to
 * {@code GroupCreatedFromMovedCases}, mirroring how {@code CourtProceedingsInitiatedProcessor}
 * handles the equivalent hearing orchestration for brand-new bulk case creation. A command handler
 * must not read the viewstore directly.
 */
@ServiceComponent(COMMAND_HANDLER)
public class MoveGroupCasesHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MoveGroupCasesHandler.class);

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles("progression.command.move-group-cases")
    public void handle(final Envelope<MoveGroupCases> envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{} payload: {}", "progression.command.move-group-cases", envelope.payload());
        }

        final MoveGroupCases command = envelope.payload();
        final CurrentGroup currentGroup = command.getCurrentGroup();
        final NewGroup newGroup = command.getNewGroup();

        final UUID currentGroupId = currentGroup.getGroupId();
        final UUID newGroupId = newGroup.getGroupId();
        final List<UUID> movedCaseIds = newGroup.getCaseIds();
        final UUID newLeadCaseId = newGroup.getLeadCaseId();
        final UUID providedOldGroupNewLeadCaseId = currentGroup.getNewLeadCaseId();

        if (!movedCaseIds.contains(newLeadCaseId)) {
            throw new IllegalArgumentException("newGroup.leadCaseId must be one of newGroup.caseIds");
        }

        final EventStream oldGroupStream = eventSource.getStreamById(currentGroupId);
        final GroupCaseAggregate oldGroupAggregate = aggregateService.get(oldGroupStream, GroupCaseAggregate.class);

        final Set<UUID> movedCaseIdSet = new HashSet<>(movedCaseIds);
        final boolean oldMasterIsMoved = movedCaseIdSet.contains(oldGroupAggregate.getGroupMaster());

        if (oldMasterIsMoved && isNull(providedOldGroupNewLeadCaseId)) {
            throw new IllegalArgumentException("currentGroup.newLeadCaseId is required when the current lead is among the moved cases");
        }
        if (!oldMasterIsMoved && nonNull(providedOldGroupNewLeadCaseId)) {
            throw new IllegalArgumentException("currentGroup.newLeadCaseId must not be supplied unless the current lead is among the moved cases");
        }

        if (!oldGroupAggregate.canMoveCasesOut(movedCaseIdSet)) {
            LOGGER.info("Move would leave group {} with zero members. groupId: {}, caseIds: {}", currentGroupId, currentGroupId, movedCaseIds);
            final Stream<Object> rejectEvents = oldGroupAggregate.rejectLastCasesToBeMovedFromGroup(currentGroupId, movedCaseIdSet);
            appendEventsToStream(envelope, oldGroupStream, rejectEvents);
            return;
        }

        final EventStream newGroupStream = eventSource.getStreamById(newGroupId);

        // One CaseAggregate per moved case, loaded once and reused across both the group-association
        // update and the fee-cloning step below — mirrors RemoveCaseFromGroupCasesHandler's own
        // single-instance reuse within one handler invocation.
        final Map<UUID, CaseAggregate> caseAggregatesByCaseId = new LinkedHashMap<>();
        final Map<UUID, EventStream> caseStreamsByCaseId = new HashMap<>();
        final List<ProsecutionCase> movedProsecutionCases = new ArrayList<>();
        ProsecutionCase leadCase = null;

        for (final UUID caseId : movedCaseIds) {
            final EventStream caseStream = eventSource.getStreamById(caseId);
            final CaseAggregate caseAggregate = aggregateService.get(caseStream, CaseAggregate.class);
            final boolean isNewGroupMaster = caseId.equals(newLeadCaseId);

            final Stream<Object> groupInfoEvents = caseAggregate.updateCaseGroupInfo(isNewGroupMaster, true, newGroupId);
            appendEventsToStream(envelope, caseStream, groupInfoEvents);

            final ProsecutionCase movedCase = caseAggregate.getProsecutionCase();
            caseAggregatesByCaseId.put(caseId, caseAggregate);
            caseStreamsByCaseId.put(caseId, caseStream);
            movedProsecutionCases.add(movedCase);
            if (isNewGroupMaster) {
                leadCase = movedCase;
            }
        }

        ProsecutionCase oldGroupNewMaster = null;
        if (nonNull(providedOldGroupNewLeadCaseId)) {
            final EventStream oldGroupNewMasterStream = eventSource.getStreamById(providedOldGroupNewLeadCaseId);
            final CaseAggregate oldGroupNewMasterAggregate = aggregateService.get(oldGroupNewMasterStream, CaseAggregate.class);
            final Stream<Object> oldGroupNewMasterEvents = oldGroupNewMasterAggregate.updateCaseGroupInfo(true, true);
            appendEventsToStream(envelope, oldGroupNewMasterStream, oldGroupNewMasterEvents);
            oldGroupNewMaster = oldGroupNewMasterAggregate.getProsecutionCase();
        }

        final Stream<Object> oldGroupEvents = oldGroupAggregate.moveCasesToNewGroup(currentGroupId, movedProsecutionCases, oldGroupNewMaster);
        appendEventsToStream(envelope, oldGroupStream, oldGroupEvents);

        final GroupCaseAggregate newGroupAggregate = aggregateService.get(newGroupStream, GroupCaseAggregate.class);
        final Stream<Object> newGroupEvents = newGroupAggregate.createGroupFromMovedCases(newGroupId, movedCaseIds, leadCase);
        appendEventsToStream(envelope, newGroupStream, newGroupEvents);

        final List<CivilFees> clonedCivilFees = new ArrayList<>();
        for (final CivilFees civilFee : leadCase.getCivilFees()) {
            final EventStream feeEventStream = eventSource.getStreamById(civilFee.getFeeId());
            final FeeAggregate feeAggregate = aggregateService.get(feeEventStream, FeeAggregate.class);

            final CivilFees newCivilFee = CivilFees.civilFees()
                    .withValuesFrom(feeAggregate.getCivilFees())
                    .withFeeId(randomUUID())
                    .build();
            clonedCivilFees.add(newCivilFee);

            final Stream<Object> feeEvents = feeAggregate.addCivilFee(newCivilFee);
            appendEventsToStream(envelope, feeEventStream, feeEvents);
        }

        for (final UUID caseId : movedCaseIds) {
            final CaseAggregate caseAggregate = caseAggregatesByCaseId.get(caseId);
            final EventStream caseStream = caseStreamsByCaseId.get(caseId);

            final ProsecutionCase caseWithClonedFees = ProsecutionCase.prosecutionCase()
                    .withValuesFrom(caseAggregate.getProsecutionCase())
                    .withCivilFees(clonedCivilFees)
                    .build();

            final Stream<Object> feeAssignmentEvents = caseAggregate.createProsecutionCase(caseWithClonedFees, clonedCivilFees);
            appendEventsToStream(envelope, caseStream, feeAssignmentEvents);
        }
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
