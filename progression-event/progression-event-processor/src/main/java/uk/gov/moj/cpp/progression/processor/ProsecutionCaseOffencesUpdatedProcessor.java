package uk.gov.moj.cpp.progression.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@SuppressWarnings({"squid:S3457", "squid:S3655",})
@ServiceComponent(EVENT_PROCESSOR)
public class ProsecutionCaseOffencesUpdatedProcessor {

    static final String PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED = "public.progression.defendant-offences-changed";
    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionCaseOffencesUpdatedProcessor.class.getCanonicalName());

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    //listen to this private event to raise public event only
    @Handles("progression.events.offences-for-defendant-changed")
    public void handleProsecutionCaseOffencesUpdatedEvent(final JsonEnvelope jsonEnvelope) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received prosecution case offences updated with payload : {}", jsonEnvelope.toObfuscatedDebugString());
        }

        sender.send(enveloper.withMetadataFrom(jsonEnvelope, PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED).apply(jsonEnvelope.payloadAsJsonObject()));
    }


}