package uk.gov.moj.cpp.progression.query.api.helper;

import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

public class ProgressionQueryHelper {

    private ProgressionQueryHelper() {

    }
    public static JsonObject removeProperty(final JsonObject origin, final String key){
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        for (final Map.Entry<String, JsonValue> entry : origin.entrySet()){
            if (!entry.getKey().equals(key)){
                builder.add(entry.getKey(), entry.getValue());
            }
        }
        return builder.build();
    }

    public static JsonObject addProperty(final JsonObject origin, final String key, final JsonObject value){
        final JsonObjectBuilder builder = buildJsonBuilder(origin);
        builder.add(key, value);
        return builder.build();
    }

    public static JsonObject addProperty(final JsonObject origin, final String key, final JsonArray value){
        final JsonObjectBuilder builder = buildJsonBuilder(origin);
        builder.add(key, value);
        return builder.build();
    }

    private static JsonObjectBuilder buildJsonBuilder(final JsonObject origin) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        for (final Map.Entry<String, JsonValue> entry : origin.entrySet()) {
            builder.add(entry.getKey(), entry.getValue());
        }
        return builder;
    }


}
