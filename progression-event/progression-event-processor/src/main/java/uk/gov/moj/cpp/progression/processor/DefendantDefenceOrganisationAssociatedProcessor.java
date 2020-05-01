package uk.gov.moj.cpp.progression.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class DefendantDefenceOrganisationAssociatedProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefendantDefenceOrganisationAssociatedProcessor.class.getName());

    private static final String PUBLIC_ASSOCIATE_DEFENCE_ORGANISATION_FOR_LAA = "public.progression.defence-organisation-for-laa-associated";

    private static final String PUBLIC_DEFENCE_ORGANISATION_FOR_LAA_DISASSOCIATED = "public.progression.defence-organisation-for-laa-disassociated";

    private static final String PUBLIC_DEFENDANT_LAA_CONTRACT_ASSOCIATED = "public.progression.defendant-laa-contract-associated";

    @Inject
    private Sender sender;

    @Handles("progression.event.defendant-defence-organisation-associated")
    public void processAssociatedEvent(final JsonEnvelope event) {
        LOGGER.info("Received Defendant DefenceOrganisationAssociated Event {}", event);
        sender.send(
                Enveloper.envelop(event.payloadAsJsonObject())
                        .withName(PUBLIC_ASSOCIATE_DEFENCE_ORGANISATION_FOR_LAA)
                        .withMetadataFrom(event));
    }

    @Handles("progression.event.defendant-defence-organisation-disassociated")
    public void processDisassociatedEvent(final JsonEnvelope event) {
        LOGGER.info("Received Defendant DefenceOrganisationDisassociated Event {}", event);

        sender.send(
                Enveloper.envelop(event.payloadAsJsonObject())
                        .withName(PUBLIC_DEFENCE_ORGANISATION_FOR_LAA_DISASSOCIATED)
                        .withMetadataFrom(event));
    }

    @Handles("progression.event.defendant-laa-associated")
    public void processDefendantLAAAssociated(final JsonEnvelope event) {

        LOGGER.info("Received Defendant LAA Contract Event {}", event);

        sender.send(
                Enveloper.envelop(event.payloadAsJsonObject())
                        .withName(PUBLIC_DEFENDANT_LAA_CONTRACT_ASSOCIATED)
                        .withMetadataFrom(event));

    }
}
