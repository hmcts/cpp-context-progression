package uk.gov.moj.cpp.progression.processor;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class CourtApplicationDeletedEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtApplicationDeletedEventProcessor.class.getName());
    private static final String HEARING_ID = "hearingId";
    private static final String SEEDING_HEARING_ID = "seedingHearingId";
    private static final String APPLICATION_ID = "applicationId";

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Handles("progression.event.delete-application-for-case-requested")
    public void processDeleteApplicationForCaseRequested(final JsonEnvelope jsonEnvelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("progression.event.delete-application-for-case-requested is processed and payload is {}", jsonEnvelope.toObfuscatedDebugString());
        }

        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        jsonObjectBuilder.add(SEEDING_HEARING_ID, jsonEnvelope.payloadAsJsonObject().getString(SEEDING_HEARING_ID));
        jsonObjectBuilder.add(APPLICATION_ID, jsonEnvelope.payloadAsJsonObject().getString(APPLICATION_ID));

        sender.send(envelopeFrom(metadataFrom(jsonEnvelope.metadata()).withName("progression.command.delete-application-for-case"),
                jsonObjectBuilder.build()));

    }

    @Handles("progression.event.delete-court-application-hearing-requested")
    public void processDeleteCourtApplicationHearingRequested(final JsonEnvelope jsonEnvelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("progression.event.delete-court-application-hearing-requested is processed and payload is {}", jsonEnvelope.toObfuscatedDebugString());
        }

        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        jsonObjectBuilder.add(SEEDING_HEARING_ID, jsonEnvelope.payloadAsJsonObject().getString(SEEDING_HEARING_ID));
        jsonObjectBuilder.add(HEARING_ID, jsonEnvelope.payloadAsJsonObject().getString(HEARING_ID));
        jsonObjectBuilder.add(APPLICATION_ID, jsonEnvelope.payloadAsJsonObject().getString(APPLICATION_ID));

        sender.send(envelopeFrom(metadataFrom(jsonEnvelope.metadata()).withName("progression.command.delete-court-application-hearing"),
                jsonObjectBuilder.build()));

    }

    @Handles("progression.event.court-application-hearing-deleted")
    public void processCourtApplicationDeleted(final JsonEnvelope jsonEnvelope) {
        final String hearingId = jsonEnvelope.payloadAsJsonObject().getString(HEARING_ID);
        final String seedingHearingId = jsonEnvelope.payloadAsJsonObject().getString(SEEDING_HEARING_ID);
        final String applicationId = jsonEnvelope.payloadAsJsonObject().getString(APPLICATION_ID);

        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        jsonObjectBuilder.add(HEARING_ID, hearingId);
        jsonObjectBuilder.add(APPLICATION_ID, applicationId);
        sender.send(envelopeFrom(metadataFrom(jsonEnvelope.metadata()).withName("public.progression.events.court-application-deleted").build(),
                jsonObjectBuilder.build()));


        commandRemoveApplicationFromSeedingHearing(jsonEnvelope, seedingHearingId, applicationId);
    }

    private void commandRemoveApplicationFromSeedingHearing(final JsonEnvelope jsonEnvelope, final String seedingHearingId, final String applicationId) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        jsonObjectBuilder.add(SEEDING_HEARING_ID, seedingHearingId);
        jsonObjectBuilder.add(APPLICATION_ID, applicationId);

        sender.send(envelopeFrom(metadataFrom(jsonEnvelope.metadata()).withName("progression.command.remove-application-from-seedingHearing"),
                jsonObjectBuilder.build()));
    }

}
