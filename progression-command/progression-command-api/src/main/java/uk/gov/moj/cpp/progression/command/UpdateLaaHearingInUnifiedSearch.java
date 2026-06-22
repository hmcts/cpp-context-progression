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
public class UpdateLaaHearingInUnifiedSearch {

    @Inject
    private Sender sender;

    /*
     * Update hearing details of LAA cases in unified search using BDF.
     */
    @Handles("progression.command.update-hearing-details-in-unified-search")
    public void handleUpdateLaaHearingDetailsUnifiedSearch(final JsonEnvelope envelope) {

        final JsonObject payload = envelope.payloadAsJsonObject();
        final JsonArray jsonArrayOfCaseIds = payload.getJsonArray("hearingIds");

        for (int i = 0; i < jsonArrayOfCaseIds.size(); i++) {
            final String caseId = jsonArrayOfCaseIds.getJsonString(i).getString();

            sender.send(envelop(JsonObjects.createObjectBuilder().add("hearingId", caseId).build())
                    .withName("progression.command.handler.update-hearing-details-in-unified-search")
                    .withMetadataFrom(envelope));

        }

    }
}
