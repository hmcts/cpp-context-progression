package uk.gov.moj.cpp.progression.handler;

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
        LOGGER.debug("progression.command-link-prosecution-cases-to-hearing {}", "caseId: " + envelope.payload().getCaseId() + " , " + "hearingId: " + envelope.payload().getHearingId());
        final LinkCaseToHearing caseLinkedToHearing = envelope.payload();
        final EventStream eventStream = eventSource.getStreamById(caseLinkedToHearing.getCaseId());
        final CaseAggregate caseAggregate = aggregateService.get(eventStream, CaseAggregate.class);
        final Stream<Object> events = caseAggregate.linkProsecutionCaseToHearing(caseLinkedToHearing.getHearingId(), caseLinkedToHearing.getCaseId());
        appendEventsToStream(envelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(enveloper.withMetadataFrom(jsonEnvelope)));
    }
}
