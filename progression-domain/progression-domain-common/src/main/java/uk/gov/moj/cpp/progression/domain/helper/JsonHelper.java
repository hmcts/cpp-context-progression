package uk.gov.moj.cpp.progression.domain.helper;

import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

public class JsonHelper {

    private JsonHelper() {

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
        final JsonObjectBuilder builder = createBuilder(origin);
        builder.add(key, value);
        return builder.build();
    }

    public static JsonObjectBuilder createBuilder(final JsonObject origin) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        for (final Map.Entry<String, JsonValue> entry : origin.entrySet()){
            builder.add(entry.getKey(), entry.getValue());
        }
        return builder;
    }

    public static JsonObject addProperty(final JsonObject origin, final String key, final boolean value){
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        for (final Map.Entry<String, JsonValue> entry : origin.entrySet()){
            builder.add(entry.getKey(), entry.getValue());
        }
        builder.add(key, value);
        return builder.build();
    }

    public static JsonObject addProperty(final JsonObject origin, final String key, final JsonObject value){
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        for (final Map.Entry<String, JsonValue> entry : origin.entrySet()){
            builder.add(entry.getKey(), entry.getValue());
        }
        builder.add(key, value);
        return builder.build();
    }

    public static JsonObject addProperty(final JsonObject origin, final String key, final JsonArray value){
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        for (final Map.Entry<String, JsonValue> entry : origin.entrySet()){
            builder.add(entry.getKey(), entry.getValue());
        }
        builder.add(key, value);
        return builder.build();
    }

    public static JsonObject renameProperty(final JsonObject origin, final String key, final String newName){
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        for (final Map.Entry<String, JsonValue> entry : origin.entrySet()){
            if (!entry.getKey().equals(key)){
                builder.add(entry.getKey(), entry.getValue());
            } else {
                builder.add(newName, entry.getValue());
            }
        }
        return builder.build();
    }
}
