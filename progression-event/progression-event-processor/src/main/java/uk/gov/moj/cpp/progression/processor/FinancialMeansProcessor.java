package uk.gov.moj.cpp.progression.processor;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

@ServiceComponent(EVENT_PROCESSOR)
public class FinancialMeansProcessor {

    public static final String MATERIAL_IDS = "materialIds";
    public static final String MATERIAL_ID = "materialId";
    public static final String MATERIAL_COMMAND_DELETE_MATERIAL = "material.command.delete-material";
    public static final String PUBLIC_PROGRESSION_EVENTS_DEFENDANT_FINANCIAL_MEANS_DELETED = "public.progression.defendant-financial-means-deleted";

    @Inject
    private Sender sender;

    @Handles("progression.event.financial-means-deleted")
    public void deleteFinancialMeans(final JsonEnvelope envelope) {

        if (hasMaterialIds(envelope)) {
            sendCommandToDeleteMaterial(envelope);
            generateDefendantFinancialMeansDeletedEvent(envelope);
        }
    }

    private void sendCommandToDeleteMaterial(final JsonEnvelope envelope) {
        final JsonArray materialIds = envelope.payloadAsJsonObject().getJsonArray(MATERIAL_IDS);
        materialIds.stream()
                .map(this::getRequestPayload)
                .forEach(jsonObject -> sendCommand(envelope, jsonObject));
    }

    private JsonObject getRequestPayload(final JsonValue materialId) {
        return createObjectBuilder()
                .add(MATERIAL_ID, materialId).build();
    }

    private boolean hasMaterialIds(final JsonEnvelope envelope) {
        return envelope.payloadAsJsonObject().getJsonArray(MATERIAL_IDS) != null &&
                !envelope.payloadAsJsonObject().getJsonArray(MATERIAL_IDS).isEmpty();
    }

    private void sendCommand(final JsonEnvelope envelope, final JsonObject jsonObject) {
        sender.send(
                Enveloper.envelop(jsonObject)
                        .withName(MATERIAL_COMMAND_DELETE_MATERIAL)
                        .withMetadataFrom(envelope));
    }

    private void generateDefendantFinancialMeansDeletedEvent(final JsonEnvelope envelope) {
        sender.send(Enveloper.envelop(envelope.payloadAsJsonObject())
                .withName(PUBLIC_PROGRESSION_EVENTS_DEFENDANT_FINANCIAL_MEANS_DELETED)
                .withMetadataFrom(envelope));
    }

}
