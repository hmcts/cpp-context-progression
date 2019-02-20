package uk.gov.moj.cpp.progression.handler;

import uk.gov.justice.core.courts.CreateNowsRequest;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
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
import uk.gov.moj.cpp.progression.aggregate.NowsAggregate;

import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("WeakerAccess")
@ServiceComponent(Component.COMMAND_HANDLER)
public class GenerateNowsHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private Enveloper enveloper;

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateNowsHandler.class.getName());

    @Handles("progression.command.generate-nows")
    public void generateNows(final JsonEnvelope envelope) throws EventStreamException {
        LOGGER.debug("progression.command.generate-nows {}", envelope);
        final JsonObject requestJson = envelope.payloadAsJsonObject().getJsonObject("createNowsRequest");
        final CreateNowsRequest createNowsRequest = jsonObjectToObjectConverter.convert(requestJson, CreateNowsRequest.class);
        final EventStream eventStream = eventSource.getStreamById(createNowsRequest.getHearing().getId());
        final NowsAggregate nowsAggregate = aggregateService.get(eventStream, NowsAggregate.class);
        final Stream<Object> events = nowsAggregate.create(createNowsRequest);
        appendEventsToStream(envelope, eventStream, events);
    }

    @Handles("resultinghmps.update-nows-material-status-dummy")
    public void doesNothing(final JsonEnvelope jsonEnvelope) {
        // required by framework
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(
                events
                        .map(enveloper.withMetadataFrom(jsonEnvelope)));
    }

}
