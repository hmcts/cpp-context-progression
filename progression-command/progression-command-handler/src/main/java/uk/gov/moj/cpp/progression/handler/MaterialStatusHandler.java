package uk.gov.moj.cpp.progression.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.RecordNowsMaterialRequest;
import uk.gov.justice.core.courts.UpdateNowsMaterialStatus;
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
import uk.gov.moj.cpp.progression.aggregate.MaterialAggregate;

import javax.inject.Inject;
import javax.json.JsonValue;
import java.util.UUID;
import java.util.stream.Stream;

@ServiceComponent(Component.COMMAND_HANDLER)
public class MaterialStatusHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaterialStatusHandler.class);

    public static final String PROGRESSION_COMMAND_RECORD_NOWS_MATERIAL_REQUEST = "progression.command.record-nows-material-request";
    public static final String PROGRESSION_COMMAND_UPDATE_NOWS_MATERIAL_STATUS = "progression.command.update-nows-material-status";
    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private Enveloper enveloper;

    @Handles(PROGRESSION_COMMAND_RECORD_NOWS_MATERIAL_REQUEST)
    public void recordNowsMaterial(final JsonEnvelope envelope) throws EventStreamException {
        final RecordNowsMaterialRequest recordNowsMaterialRequest = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), RecordNowsMaterialRequest.class);
        final EventStream eventStream = eventSource.getStreamById(recordNowsMaterialRequest.getContext().getMaterialId());
        final MaterialAggregate materialAggregate = aggregateService.get(eventStream, MaterialAggregate.class);
        final Stream<Object> events = materialAggregate.create(recordNowsMaterialRequest.getContext());
        appendEventsToStream(envelope, eventStream, events);
    }

    @Handles(PROGRESSION_COMMAND_UPDATE_NOWS_MATERIAL_STATUS)
    public void updateStatus(final JsonEnvelope envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{} {}", PROGRESSION_COMMAND_UPDATE_NOWS_MATERIAL_STATUS, envelope.toObfuscatedDebugString());
        }
        final UpdateNowsMaterialStatus update = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), UpdateNowsMaterialStatus.class);
        final EventStream eventStream = eventSource.getStreamById(UUID.fromString(update.getMaterialId()));
        final MaterialAggregate nowsAggregate = aggregateService.get(eventStream, MaterialAggregate.class);
        final Stream<Object> events = nowsAggregate.nowsMaterialStatusUpdated(UUID.fromString(update.getMaterialId()), update.getStatus());
        appendEventsToStream(envelope, eventStream, events);
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(events.map(enveloper.withMetadataFrom(jsonEnvelope)));
    }

}
