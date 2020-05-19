package uk.gov.moj.cpp.progression.handler;

import uk.gov.justice.core.courts.AdjournHearingDefendantRequest;
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
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;
import javax.inject.Inject;
import javax.json.JsonValue;
import java.util.stream.Stream;

@ServiceComponent(Component.COMMAND_HANDLER)
public class AdjournHearingDefendantRequestHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles("progression.command.adjourn-hearing-request")
    public void handle(final Envelope<AdjournHearingDefendantRequest> adjournHearingDefendantRequestEnvelope) throws EventStreamException {
        final AdjournHearingDefendantRequest adjournHearingDefendantRequest = adjournHearingDefendantRequestEnvelope.payload();
        final EventStream eventStream = eventSource.getStreamById(adjournHearingDefendantRequest.getCurrentHearingId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.adjournHearingDefendantRequest(adjournHearingDefendantRequest.getCurrentHearingId(), adjournHearingDefendantRequest.getAdjournedHearingId());
        appendEventsToStream(adjournHearingDefendantRequestEnvelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(Enveloper.toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
