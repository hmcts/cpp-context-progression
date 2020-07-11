package uk.gov.moj.cpp.progression.processor;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.JsonObjects.getJsonObject;

@ServiceComponent(EVENT_PROCESSOR)
public class StagingEnforcementAcknowledgmentEventProcessor {

    private static final String ORIGINATOR = "originator";
    private static final String COURTS = "Courts";
    public static final String MATERIAL_ID = "materialId";

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private Requester requester;

    @Handles("public.stagingenforcement.enforce-financial-imposition-acknowledgement")
    public void processAcknowledgement(final JsonEnvelope event) {

        final JsonObject enforcementResponsePayload = event.payloadAsJsonObject();
        final Optional<String> originator = JsonObjects.getString(enforcementResponsePayload, ORIGINATOR);

        if (originator.isPresent() && COURTS.equalsIgnoreCase(originator.get())) {
            Optional<String> errorCode = Optional.empty();

            final String acknowledgementLbl = "acknowledgement";

            final String errorCodeLbl = "errorCode";

            final Optional<JsonObject> acknowledgement = getJsonObject(enforcementResponsePayload, acknowledgementLbl);

            if (acknowledgement.isPresent()) {
                errorCode = JsonObjects.getString(acknowledgement.get(), errorCodeLbl);
            }

            final Optional<String> optionalRequestId = JsonObjects.getString(enforcementResponsePayload, "requestId");
            final String requestId = optionalRequestId.orElseThrow(() -> new IllegalArgumentException("RequestId is mandatory from enforcement"));
            final Map<String, String> materialIdsForRequestId = this.getMaterialIdsForRequestId(requestId, event);

            if (!errorCode.isPresent()) {
                processAckResponse(event, enforcementResponsePayload, materialIdsForRequestId);
            }
            if (errorCode.isPresent()) {
                processErrorAckResponse(event, enforcementResponsePayload, materialIdsForRequestId);
            }
        }
    }

    private void processAckResponse(JsonEnvelope event, JsonObject enforcementResponsePayload, Map<String, String> materialIdsForRequestId) {
        materialIdsForRequestId.forEach((materialId, payload) -> {
            final JsonObject commandPayload = JsonObjects.createObjectBuilder(enforcementResponsePayload).add(MATERIAL_ID, materialId).build();
            final Envelope<JsonObject> envelope = envelop(commandPayload).withName("progression.command.apply-enforcement-acknowledgement").withMetadataFrom(event);
            this.sender.sendAsAdmin(envelope);
        });
    }

    private void processErrorAckResponse(JsonEnvelope event, JsonObject enforcementResponsePayload, Map<String, String> materialIdsForRequestId) {
        materialIdsForRequestId.forEach((materialId, payload) -> {
            final JsonObject commandPayload = JsonObjects.createObjectBuilder(enforcementResponsePayload).add(MATERIAL_ID, materialId).build();
            final Envelope<JsonObject> envelope = envelop(commandPayload).withName("progression.command.enforcement-acknowledgement-error").withMetadataFrom(event);
            this.sender.sendAsAdmin(envelope);
        });
    }

    private Map<String, String> getMaterialIdsForRequestId(final String requestId, final JsonEnvelope event) {
        final JsonObject payload = Json.createObjectBuilder().add("requestId", requestId).build();
        final JsonObject requestMaterialIdPayload = requester.request(envelop(payload)
                .withName("progression.query.now-document-requests-by-request-id")
                .withMetadataFrom(event)).payloadAsJsonObject();
        return requestMaterialIdPayload.getJsonArray("nowDocumentRequests").getValuesAs(JsonObject.class).stream()
                .collect(Collectors.toMap(json->json.getString(MATERIAL_ID), json-> json.getString("payload")));

    }

}
