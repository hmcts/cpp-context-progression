package uk.gov.moj.cpp.progression.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.CreateHearingDefendantRequest;
import uk.gov.justice.core.courts.ListDefendantRequest;
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
import java.util.List;
import java.util.stream.Stream;

@ServiceComponent(Component.COMMAND_HANDLER)
public class CreateHearingDefendantRequestHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    private static final Logger LOGGER =
            LoggerFactory.getLogger(CreateHearingDefendantRequestHandler.class.getName());

    @Handles("progression.command.create-hearing-defendant-request")
    public void handle(final Envelope<CreateHearingDefendantRequest> createHearingDefendantRequestEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.create-hearing-defendant-request {}", createHearingDefendantRequestEnvelope );
        final List<ListDefendantRequest> listDefendantRequests = createHearingDefendantRequestEnvelope.payload().getDefendantRequests();
        final EventStream eventStream = eventSource.getStreamById(createHearingDefendantRequestEnvelope.payload().getHearingId());
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.createHearingDefendantRequest(listDefendantRequests);
        appendEventsToStream(createHearingDefendantRequestEnvelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(enveloper.withMetadataFrom(jsonEnvelope)));
    }
}
