package uk.gov.moj.cpp.progression.handler;

import static java.util.Objects.nonNull;

import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.GroupCaseAggregate;
import uk.gov.moj.cpp.progression.command.RemoveCaseFromGroupCases;

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class RemoveCaseFromGroupCasesHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoveCaseFromGroupCasesHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles("progression.command.remove-case-from-group-cases")
    public void handle(final Envelope<RemoveCaseFromGroupCases> envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{} payload: {}", "progression.command.remove-case-from-group-cases", envelope.payload());
        }

        final RemoveCaseFromGroupCases removeCaseFromGroupCases = envelope.payload();
        final UUID groupId = removeCaseFromGroupCases.getGroupId();
        final UUID removedCaseId = removeCaseFromGroupCases.getProsecutionCaseId();

        final EventStream groupEventStream = eventSource.getStreamById(groupId);
        final GroupCaseAggregate groupAggregate = aggregateService.get(groupEventStream, GroupCaseAggregate.class);

        if (groupAggregate.canBeRemoved(removedCaseId)) {
            final EventStream removedCaseEventStream = eventSource.getStreamById(removedCaseId);
            final CaseAggregate removedCaseAggregate = aggregateService.get(removedCaseEventStream, CaseAggregate.class);
            final Stream<Object> removedCaseEvents = removedCaseAggregate.updateCaseGroupInfo(false, false);
            appendEventsToStream(envelope, removedCaseEventStream, removedCaseEvents);
            final ProsecutionCase removedCase = removedCaseAggregate.getProsecutionCase();

            final UUID newGroupMasterId = groupAggregate.getNewGroupMaster(removedCaseId);
            ProsecutionCase newGroupMaster = null;
            if (nonNull(newGroupMasterId)) {
                final EventStream newMasterCaseEventStream = eventSource.getStreamById(newGroupMasterId);
                final CaseAggregate newMasterCaseAggregate = aggregateService.get(newMasterCaseEventStream, CaseAggregate.class);
                final Stream<Object> newMasterCaseEvents = newMasterCaseAggregate.updateCaseGroupInfo(true, true);
                appendEventsToStream(envelope, newMasterCaseEventStream, newMasterCaseEvents);
                newGroupMaster = newMasterCaseAggregate.getProsecutionCase();
            }

            final Stream<Object> groupEvents = groupAggregate.removeCaseFromGroupCases(groupId, removedCase, newGroupMaster);
            appendEventsToStream(envelope, groupEventStream, groupEvents);
        } else {
            LOGGER.info("Last case cannot be removed from group cases. groupId: {}, caseId: {}", groupId, removedCaseId);
            final Stream<Object> groupEvents = groupAggregate.rejectLastCaseToBeRemovedFromGroup(groupId, removedCaseId);
            appendEventsToStream(envelope, groupEventStream, groupEvents);
        }
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(events.map(Enveloper.toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}