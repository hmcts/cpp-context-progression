package uk.gov.moj.cpp.progression.command.api.helper;

import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

public class ProgressionCommandHelper {

    private ProgressionCommandHelper() {

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

    public static JsonObject addProperty(final JsonObject origin, final String key, final String value){
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        for (final Map.Entry<String, JsonValue> entry : origin.entrySet()){
                builder.add(entry.getKey(), entry.getValue());
        }
        builder.add(key, value);
        return builder.build();
    }
}
