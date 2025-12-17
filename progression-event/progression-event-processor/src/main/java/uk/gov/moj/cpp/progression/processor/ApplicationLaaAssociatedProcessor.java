package uk.gov.moj.cpp.progression.processor;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class ApplicationLaaAssociatedProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationLaaAssociatedProcessor.class.getName());

    private static final String PUBLIC_DEFENDANT_LAA_CONTRACT_ASSOCIATED = "public.progression.defendant-laa-contract-associated";

    @Inject
    private Sender sender;

    /** This progression event triggered to pass through to defence context
     * Progression Event - progression.event.application-laa-associated
     * Public Event for Defence - public.progression.defendant-laa-contract-associated
    **/
    @Handles("progression.event.application-laa-associated")
    public void processDefendantLAAAssociated(final JsonEnvelope event) {

        LOGGER.info("Received Defendant LAA Contract Event {}", event);

        sender.send(
                Enveloper.envelop(event.payloadAsJsonObject())
                        .withName(PUBLIC_DEFENDANT_LAA_CONTRACT_ASSOCIATED)
                        .withMetadataFrom(event));

    }
}
