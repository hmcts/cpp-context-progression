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
public class PatchAndResendLaaCaseOutcomeAPi {

    @Inject
    private Sender sender;

    /**
     * Patch and resend LAA outcome concluded event which originally had schema validation error because missing hearingID and failed at event processor.
     */
    @Handles("progression.command.patch-and-resend-laa-outcome-concluded")
    public void handle(final JsonEnvelope envelope) {

        final JsonObject payload = envelope.payloadAsJsonObject();
        final JsonArray caseHearings = payload.getJsonArray("caseHearings");

        for (int i = 0; i < caseHearings.size(); i++) {
            final JsonObject caseHearing = caseHearings.getJsonObject(i);

            final JsonObject jsonObject = Json.createObjectBuilder()
                    .add("caseId", caseHearing.getString("caseId"))
                    .add("hearingId", caseHearing.getString("hearingId"))
                    .add("resultDate", caseHearing.getString("resultDate"))
                    .build();
            sender.send(envelop(jsonObject)
                    .withName("progression.command.handler.patch-and-resend-laa-outcome-concluded")
                    .withMetadataFrom(envelope));

        }
    }
}
