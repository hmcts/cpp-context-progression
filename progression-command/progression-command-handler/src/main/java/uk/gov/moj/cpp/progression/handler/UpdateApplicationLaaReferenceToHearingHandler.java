package uk.gov.moj.cpp.progression.handler;

import java.util.UUID;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.LaaReference;
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
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;
import uk.gov.moj.cpp.progression.service.LegalStatusReferenceDataService;

import static java.util.UUID.fromString;

@ServiceComponent(Component.COMMAND_HANDLER)
public class UpdateApplicationLaaReferenceToHearingHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(UpdateApplicationLaaReferenceToHearingHandler.class.getName());
    public static final String APPLICATION_ID = "applicationId";
    public static final String HEARING_ID = "hearingId";
    public static final String SUBJECT_ID = "subjectId";
    public static final String OFFENCE_ID = "offenceId";
    public static final String LAA_REFERENCE = "laaReference";

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Handles("progression.command.update-application-laa-reference-for-hearing")
    public void handleUpdateApplication(final JsonEnvelope envelope) throws EventStreamException {
        LOGGER.debug("progression.command.update-application-laa-reference-for-hearing {}", envelope.payload());
        final JsonObject payload = envelope.payloadAsJsonObject();
        final UUID hearingId = fromString(payload.getString(HEARING_ID));
        final UUID applicationId = fromString(payload.getString(APPLICATION_ID));
        final UUID subjectId = fromString(payload.getString(SUBJECT_ID));
        final UUID offenceId = fromString(payload.getString(OFFENCE_ID));
        final JsonObject laaReferenceJson = payload.getJsonObject(LAA_REFERENCE);
        LaaReference laaReference = jsonObjectToObjectConverter.convert(laaReferenceJson, LaaReference.class);
        final EventStream eventStream = eventSource.getStreamById(hearingId);
        final HearingAggregate hearingAggregate = aggregateService.get(eventStream, HearingAggregate.class);
        final Stream<Object> events = hearingAggregate.updateApplicationLaaReferenceForHearing(hearingId, applicationId, subjectId, offenceId, laaReference);
        appendEventsToStream(envelope, eventStream, events);

    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(events.map(enveloper.withMetadataFrom(jsonEnvelope)));
    }
}
