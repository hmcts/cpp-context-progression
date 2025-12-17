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
import javax.json.JsonObjectBuilder;

@ServiceComponent(COMMAND_API)
public class PatchUpdateApplicationsStatusApi {

    private static final String ID = "id";
    private static final String APPLICATION_STATUS = "applicationStatus";
    @Inject
    private Sender sender;


    @Handles("progression.patch-update-applications-status")
    public void handle(final JsonEnvelope envelope) {

        final JsonObject payload = envelope.payloadAsJsonObject();
        final JsonArray applications = payload.getJsonArray("applications");

        for (int i = 0; i < applications.size(); i++) {
            final JsonObject application = applications.getJsonObject(i);

            final JsonObjectBuilder jsonObject = Json.createObjectBuilder()
                    .add(ID, application.getString(ID));
            if (application.containsKey(APPLICATION_STATUS)) {
                jsonObject.add(APPLICATION_STATUS, application.getString(APPLICATION_STATUS));
            }

            sender.send(envelop(jsonObject.build())
                    .withName("progression.command.patch-update-application-status")
                    .withMetadataFrom(envelope));

        }
    }
}
