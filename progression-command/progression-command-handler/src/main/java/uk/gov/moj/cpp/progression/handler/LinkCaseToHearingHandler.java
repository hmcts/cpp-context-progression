package uk.gov.moj.cpp.progression.handler;

import static java.util.Objects.nonNull;

import uk.gov.justice.core.courts.LinkCaseToHearing;
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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.COMMAND_HANDLER)
public class LinkCaseToHearingHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LinkCaseToHearingHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Handles("progression.command-link-prosecution-cases-to-hearing")
    public void handle(final Envelope<LinkCaseToHearing> envelope) throws EventStreamException {
        LOGGER.debug("progression.command-link-prosecution-cases-to-hearing {}", envelope);
        final LinkCaseToHearing caseLinkedToHearing = envelope.payload();

        final EventStream eventStream = eventSource.getStreamById(caseLinkedToHearing.getCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.linkProsecutionCaseToHearing(caseLinkedToHearing.getHearingId(), caseLinkedToHearing.getCaseId());
        appendEventsToStream(envelope, eventStream, events);

        final UUID groupId = nonNull(caseAggregate.getProsecutionCase()) ? caseAggregate.getProsecutionCase().getGroupId() : null;
        if (nonNull(groupId)) {
            final List<UUID> memberCases = getMemberCasesExcludingMasterCase(groupId, caseLinkedToHearing.getCaseId());
            for (final UUID caseId : memberCases) {
                linkProsecutionCaseToHearing(envelope, caseLinkedToHearing.getHearingId(), caseId);
            }
        }

    }

    private void linkProsecutionCaseToHearing(final Envelope<?> envelope, final UUID hearingId, final UUID caseId) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(caseId);
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.linkProsecutionCaseToHearing(hearingId, caseId);
        appendEventsToStream(envelope, eventStream, events);
    }

    private List<UUID> getMemberCasesExcludingMasterCase(final UUID groupId, final UUID masterCaseId) {
        final EventStream eventStreamForGroupCases = eventSource.getStreamById(groupId);
        final GroupCaseAggregate groupCaseAggregate = aggregateService.get(eventStreamForGroupCases, GroupCaseAggregate.class);
        return groupCaseAggregate.getMemberCases().stream()
                .filter(caseId -> !caseId.equals(masterCaseId))
                .collect(Collectors.toList());
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(enveloper.withMetadataFrom(jsonEnvelope)));
    }
}
