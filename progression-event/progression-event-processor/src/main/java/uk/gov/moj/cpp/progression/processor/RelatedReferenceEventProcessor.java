package uk.gov.moj.cpp.progression.processor;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S3655"})
@ServiceComponent(EVENT_PROCESSOR)
public class RelatedReferenceEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelatedReferenceEventProcessor.class);

    private static final String PUBLIC_PROGRESSION_RELATED_REFERENCE_ADDED = "public.progression.related-reference-added";

    private static final String PUBLIC_PROGRESSION_RELATED_REFERENCE_DELETED = "public.progression.related-reference-deleted";

    public static final String RELATED_REFERENCE_ID = "relatedReferenceId";

    public static final String RELATED_REFERENCE = "relatedReference";

    public static final String PROSECUTION_CASE_ID = "prosecutionCaseId";

    @Inject
    private Sender sender;

    @Inject
    private ProgressionService progressionService;

    @Handles("progression.event.related-reference-added")
    public void handleRelatedReferenceAdded(final JsonEnvelope envelope) {

        final JsonObject publicEventPayload =
                createObjectBuilder()
                        .add(RELATED_REFERENCE_ID, envelope.payloadAsJsonObject().getString(RELATED_REFERENCE_ID))
                        .add(RELATED_REFERENCE, envelope.payloadAsJsonObject().getString(RELATED_REFERENCE))
                        .add(PROSECUTION_CASE_ID, envelope.payloadAsJsonObject().getString(PROSECUTION_CASE_ID))
                        .build();

        sender.send(envelopeFrom(
                metadataFrom(envelope.metadata()).withName(PUBLIC_PROGRESSION_RELATED_REFERENCE_ADDED),
                publicEventPayload));

        LOGGER.info("Public event sent - {}", publicEventPayload);
    }

    @Handles("progression.event.related-reference-deleted")
    public void handleRelatedReferenceDeleted(final JsonEnvelope envelope) {

        final JsonObject publicEventPayload =
                createObjectBuilder()
                        .add(RELATED_REFERENCE_ID, envelope.payloadAsJsonObject().getString(RELATED_REFERENCE_ID))
                        .add(PROSECUTION_CASE_ID, envelope.payloadAsJsonObject().getString(PROSECUTION_CASE_ID))
                        .build();

        sender.send(envelopeFrom(
                metadataFrom(envelope.metadata()).withName(PUBLIC_PROGRESSION_RELATED_REFERENCE_DELETED),
                publicEventPayload));

        LOGGER.info("Public event sent - {}", publicEventPayload);
    }


}
