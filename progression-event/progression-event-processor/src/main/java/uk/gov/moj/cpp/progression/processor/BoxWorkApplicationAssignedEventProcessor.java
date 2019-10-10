package uk.gov.moj.cpp.progression.processor;


import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ServiceComponent(EVENT_PROCESSOR)
@SuppressWarnings({"squid:CommentedOutCodeLine", "squid:S2789", "squid:S1135", "squid:CallToDeprecatedMethod"})
public class BoxWorkApplicationAssignedEventProcessor {

    static final String PUBLIC_PROGRESSION_BOXWORK_APPLICATION_REFERRED = "public.progression.boxwork-assignment-changed";
    private static final Logger LOGGER = LoggerFactory.getLogger(BoxWorkApplicationAssignedEventProcessor.class.getCanonicalName());


    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;


    @Handles("progression.event.boxwork-assignment-changed")
    public void processBoxWAssignmentChanged(final JsonEnvelope jsonEnvelope) {

        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();

        LOGGER.info(" Box work assigned with payload {}", payload);
        sender.send(enveloper.withMetadataFrom(jsonEnvelope, PUBLIC_PROGRESSION_BOXWORK_APPLICATION_REFERRED).apply(payload));

    }

}
