package uk.gov.moj.cpp.progression.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.DefendantsLAAService;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;


import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

@ServiceComponent(EVENT_PROCESSOR)
public class LAAOrganisationSetupProcessor {

    @Inject
    private Sender sender;

    @Inject
    private DefendantsLAAService defendantsLAAService;

    private static final Logger LOGGER =
            LoggerFactory.getLogger(LAAOrganisationSetupProcessor.class.getName());
    private static final String PROGRESSION_COMMAND_HANDLER_ASSOCIATE_ORPHANED_CASE = "progression.command.handler.associate-orphaned-case";

    private static final String LAA_CONTRACT_NUMBER = "laaContractNumber";
    private static final String ORGANISATION_ID = "organisationId";
    private static final String ORGANISATION_NAME = "organisationName";
    private static final String DEFENDANT_ID = "defendantId";


    @Handles("public.usersgroups.organisation-created")
    public void setUpLAAOrganisation (final JsonEnvelope envelope) {
        LOGGER.info("Received Organisation Created Event  {}", envelope.payloadAsJsonObject());
        final JsonObject payload = envelope.payloadAsJsonObject().getJsonObject("organisationDetails");
        if(payload.containsKey(LAA_CONTRACT_NUMBER)) {
            handleOrganisationSetupAndUpdate(envelope, payload);

        } else {
            LOGGER.info("No Organisation Set up for Legal Aid Agency");
        }
    }

    private void handleOrganisationSetupAndUpdate(JsonEnvelope envelope, JsonObject payload) {
        final String laaContractNumber = payload.getString(LAA_CONTRACT_NUMBER);
        final String organisationId = payload.getString(ORGANISATION_ID);
        final String organisationName = payload.getString(ORGANISATION_NAME);
        final JsonArray defendants = defendantsLAAService.getDefendantsByLAAContractNumber(envelope, laaContractNumber);

        defendants.stream().forEach(defendantId -> {
            final JsonObject associateOrphanedCasePayload = Json.createObjectBuilder()
                    .add(LAA_CONTRACT_NUMBER, laaContractNumber)
                    .add(ORGANISATION_ID, organisationId)
                    .add(ORGANISATION_NAME, organisationName)
                    .add(DEFENDANT_ID, defendantId)
                    .build();
            sender.send(
                    Enveloper.envelop(associateOrphanedCasePayload)
                            .withName(PROGRESSION_COMMAND_HANDLER_ASSOCIATE_ORPHANED_CASE)
                            .withMetadataFrom(envelope));
        });
    }

    @Handles("public.usersgroups.organisation-updated")
    public void  updateOrganisationLAAContractNumber(final JsonEnvelope envelope) {
        LOGGER.info("Received Organisation Updated Event  {}", envelope.payloadAsJsonObject());

        final JsonObject payload = envelope.payloadAsJsonObject().getJsonObject("organisationDetails");
        if(payload.containsKey(LAA_CONTRACT_NUMBER)) {
            handleOrganisationSetupAndUpdate(envelope, payload);

        } else {
            LOGGER.info("No Organisation LAA Contract Number has been updated");
        }
    }
}
