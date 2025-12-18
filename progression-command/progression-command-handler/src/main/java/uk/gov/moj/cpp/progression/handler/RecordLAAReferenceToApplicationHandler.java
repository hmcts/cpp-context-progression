package uk.gov.moj.cpp.progression.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.LaaReference;
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
import uk.gov.moj.cpp.progression.service.LegalStatusReferenceDataService;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.UUID.fromString;

@ServiceComponent(Component.COMMAND_HANDLER)
public class RecordLAAReferenceToApplicationHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(RecordLAAReferenceToApplicationHandler.class.getName());
    public static final String APPLICATION_ID = "applicationId";
    public static final String SUBJECT_ID = "subjectId";
    public static final String OFFENCE_ID = "offenceId";

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Enveloper enveloper;

    @Inject
    LegalStatusReferenceDataService legalStatusReferenceDataService;

    @Handles("progression.command.handler.record-laareference-for-application")
    public void handle(final JsonEnvelope envelope) throws EventStreamException {
        LOGGER.debug("progression.command.record-laareference-for-application {}", envelope.payload());
        final JsonObject payload = envelope.payloadAsJsonObject();
        final UUID applicationId = fromString(payload.getString(APPLICATION_ID));
        final UUID subjectId = fromString(payload.getString(SUBJECT_ID));
        final UUID offenceId = fromString(payload.getString(OFFENCE_ID));
        final String statusCode = payload.getString("statusCode");
        final Optional<JsonObject> optionalLegalStatus = legalStatusReferenceDataService.getLegalStatusByStatusIdAndStatusCode
                (envelope,  statusCode);
        if(optionalLegalStatus.isPresent()) {
            final JsonObject legalStatus = optionalLegalStatus.get();
            final LaaReference laaReference =  LaaReference.laaReference()
                    .withStatusCode(statusCode)
                    .withStatusId(fromString(legalStatus.getString("id")))
                    .withStatusDescription(legalStatus.getString("statusDescription"))
                    .withStatusDate(LocalDate.parse(payload.getString("statusDate")))
                    .withApplicationReference(payload.getString("applicationReference"))
                    .withOffenceLevelStatus(legalStatus.getString("defendantLevelStatus", null))
                    .build();
            final EventStream eventStream = eventSource.getStreamById(applicationId);
            final ApplicationAggregate applicationAggregate = aggregateService.get(eventStream, ApplicationAggregate.class);
            final Stream<Object> events = applicationAggregate.recordLAAReferenceForApplication(applicationId,subjectId,offenceId,laaReference);
            appendEventsToStream(envelope, eventStream, events);

        } else {
            LOGGER.error("Unable to get Ref Data for Legal Status by Status Code {}", statusCode);
        }
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(events.map(enveloper.withMetadataFrom(jsonEnvelope)));
    }
}
