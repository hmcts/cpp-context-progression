package uk.gov.moj.cpp.progression.helper;

import static uk.gov.justice.services.messaging.DefaultJsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.ID;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.NAME;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.USER_ID;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataFrom;

import javax.json.Json;
import javax.json.JsonValue;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
/**
 * 
 * @deprecated
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
public class JsonHelper {

    public static final String LIFECYCLE_ID = "lifecycleId";
    public static final String CONTEXT = "context";
    public static final String USER = "user";

    private JsonHelper() {
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








}
