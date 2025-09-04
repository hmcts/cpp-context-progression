package uk.gov.moj.cpp.progression.command;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

@ServiceComponent(COMMAND_API)
public class CaseStatusApi {
    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Handles("progression.inactive-case-bdf")
    public void handleCaseInactiveViaBdf(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("prosecutionCaseId", payload.getString("prosecutionCaseId"))
                .add("caseStatus", CaseStatusEnum.INACTIVE.name())
                .build();

        sender.send(envelop(jsonObject)
                .withName("progression.command.update-case-status-bdf")
                .withMetadataFrom(envelope));
    }

    @Handles("progression.update-case-status-bdf")
    public void handleUpdateCaseStatusBdf(final JsonEnvelope envelope) {
        sender.send(envelop(envelope.payloadAsJsonObject())
                .withName("progression.command.update-case-status-bdf")
                .withMetadataFrom(envelope));
    }
}
