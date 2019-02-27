package uk.gov.moj.cpp.progression.activiti.common;


import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonMetadata.ID;
import static uk.gov.justice.services.messaging.JsonMetadata.NAME;
import static uk.gov.justice.services.messaging.JsonMetadata.USER_ID;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.services.messaging.Metadata;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.Optional;
import java.util.UUID;

public class JsonHelper {

    public static final String PROCESS_ID = "processId";
    public static final String CONTEXT = "context";
    public static final String ORIGINATOR = "originator";
    public static final String ORIGINATOR_VALUE = "court";

    private JsonHelper() {
    }

    public static Metadata createMetadataWithProcessIdAndUserId(final String id, final String name, final String processId, final String userId) {
        return metadataFrom(Json.createObjectBuilder()
                .add(ID, id)
                .add(NAME, name)
                .add(PROCESS_ID, processId)
                .add(ORIGINATOR, ORIGINATOR_VALUE)
                .add(CONTEXT, Json.createObjectBuilder()
                        .add(USER_ID, userId))
                .build()).build();
    }
    public static Optional<String> getOriginatorValueFromJsonMetadata(final JsonObject jsonMetadata) {
        return JsonObjects.getString(jsonMetadata, ORIGINATOR);
    }
    public static JsonEnvelope assembleEnvelopeWithPayloadAndMetaDetails(final JsonObject payload, final String contentType, final String processId, final String userId) {
        final Metadata metadata = createMetadataWithProcessIdAndUserId(UUID.randomUUID().toString(), contentType, processId, userId);
        final JsonObject payloadWithMetada = addMetadataToPayload(payload, metadata);
        return envelopeFrom(metadata, payloadWithMetada);
    }

    private static JsonObject addMetadataToPayload(final JsonObject load, final Metadata metadata) {
        final JsonObjectBuilder job = Json.createObjectBuilder();
        load.entrySet().forEach(entry -> job.add(entry.getKey(), entry.getValue()));
        job.add(JsonEnvelope.METADATA, metadata.asJsonObject());
        return job.build();
    }
}
