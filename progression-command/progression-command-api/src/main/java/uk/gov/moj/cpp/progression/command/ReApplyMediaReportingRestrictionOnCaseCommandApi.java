package uk.gov.moj.cpp.progression.command;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonObject;

@ServiceComponent(COMMAND_API)
public class ReApplyMediaReportingRestrictionOnCaseCommandApi {

    @Inject
    private Sender sender;

    @Handles("progression.command.reapply-media-reporting-restrictions")
    public void handle(final JsonEnvelope envelope) {

        final JsonObject payload = envelope.payloadAsJsonObject();
        final JsonArray jsonArrayOfCaseIds = payload.getJsonArray("caseIds");

        jsonArrayOfCaseIds.forEach(caseId ->
            sender.send(envelop(JsonObjects.createObjectBuilder().add("caseId", caseId).build())
                    .withName("progression.command.handler.reapply-media-reporting-restrictions")
                    .withMetadataFrom(envelope))
        );
    }
}
