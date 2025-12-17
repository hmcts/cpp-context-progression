package uk.gov.moj.cpp.progression.command;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

@ServiceComponent(COMMAND_API)
public class ResendLaaCaseOutcomeAPi {

    @Inject
    private Sender sender;

    /*
     * Resend outcome conluded update to LAA for existing progression.event.laa-defendant-proceeding-concluded-changed event using BDF.
     * This is a temporary workaround until root cause is fixed(CCT-1631) .
     */
    @Handles("progression.command.resend-laa-outcome-concluded")
    public void handle(final JsonEnvelope envelope) {

        final JsonObject payload = envelope.payloadAsJsonObject();
        final JsonArray jsonArrayOfCaseIds = payload.getJsonArray("caseIds");

        for (int i = 0; i < jsonArrayOfCaseIds.size(); i++) {
            final String caseId = jsonArrayOfCaseIds.getJsonString(i).getString();

            sender.send(envelop(Json.createObjectBuilder().add("caseId", caseId).build())
                    .withName("progression.command.handler.resend-laa-outcome-concluded")
                    .withMetadataFrom(envelope));

        }

    }
}
