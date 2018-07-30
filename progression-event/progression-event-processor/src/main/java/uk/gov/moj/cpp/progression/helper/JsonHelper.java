package uk.gov.moj.cpp.progression.helper;

import static uk.gov.justice.services.messaging.DefaultJsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.ID;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.NAME;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.USER_ID;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataFrom;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.services.messaging.Metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

public class JsonHelper {

    public static final String LIFECYCLE_ID = "lifecycleId";
    public static final String CONTEXT = "context";
    public static final String USER = "user";

    private JsonHelper() {
    }

    public static Metadata createMetadataWithNoLifecycleId(final String id, final String name) {
        return metadataFrom(Json.createObjectBuilder()
                .add(ID, id)
                .add(NAME, name)
                .build());
    }

    public static Metadata createMetadataWithProcessIdAndUserId(final String id, final String name, final String lifecycleId, final String userId) {
        return metadataFrom(Json.createObjectBuilder()
                .add(ID, id)
                .add(NAME, name)
                .add(LIFECYCLE_ID, lifecycleId)
                .add(CONTEXT, Json.createObjectBuilder()
                        .add(USER_ID, userId))
                .build());
    }

    public static JsonEnvelope createJsonEnvelope(final Metadata metadata, final JsonValue jsonValue) {
        return envelopeFrom(metadata, jsonValue);
    }

    public static Optional<String> getLifecycleIdFromJsonMetadata(final JsonObject jsonMetadata) {
        return JsonObjects.getString(jsonMetadata, new String[]{LIFECYCLE_ID});
    }

    public static String getUserIdFromJsonMetadata(final JsonObject jsonMetadata) {
        final Optional<JsonObject> contextAsJsonObject = JsonObjects.getJsonObject(jsonMetadata, new String[]{CONTEXT});
        if (contextAsJsonObject.isPresent()) {
            return contextAsJsonObject.get().getString(USER);
        }
        return null;
    }

    public static JsonEnvelope assembleEnvelopeWithPayloadAndMetaDetails(final JsonObject payload, final String contentType, final String processId, final String userId) {
        final Metadata metadata = createMetadataWithProcessIdAndUserId(UUID.randomUUID().toString(), contentType, processId, userId);
        final JsonObject payloadWithMetada = addMetadataToPayload(payload, metadata);
        return envelopeFrom(metadata, payloadWithMetada);
    }

    public static JsonEnvelope assembleEnvelopeWithPayloadAndMetaData(final JsonObject payload, final Metadata metadata) {
        final JsonObject payloadWithMetada = addMetadataToPayload(payload, metadata);
        return envelopeFrom(metadata, payloadWithMetada);
    }

    private static JsonObject addMetadataToPayload(final JsonObject load, final Metadata metadata) {
        final JsonObjectBuilder job = Json.createObjectBuilder();
        load.entrySet().forEach(entry -> job.add(entry.getKey(), entry.getValue()));
        job.add(JsonEnvelope.METADATA, metadata.asJsonObject());
        return job.build();
    }

    public static Metadata addLifecycleIdToMetadata(final Metadata metadata, final String lifecycleId) {
        final JsonObjectBuilder job = Json.createObjectBuilder();
        metadata.asJsonObject().entrySet().forEach(entry -> job.add(entry.getKey(), entry.getValue()));
        job.add(LIFECYCLE_ID, lifecycleId);
        return metadataFrom(job.build());
    }

    public static List<JsonObject> getJsonObjectsFromArray(final JsonObject jsonObject, final String arrayElementName) {
        final JsonArray jsonArray = jsonObject.getJsonArray(arrayElementName);

        if (jsonArray == null) {
            return new ArrayList<>();
        } else {
            return jsonArray.stream()
                    .map(jv -> (JsonObject) jv)
                    .collect(Collectors.toList());
        }
    }
}
