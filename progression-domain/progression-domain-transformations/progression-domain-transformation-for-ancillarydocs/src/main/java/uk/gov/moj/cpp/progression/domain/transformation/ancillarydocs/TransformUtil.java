package uk.gov.moj.cpp.progression.domain.transformation.ancillarydocs;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;

import uk.gov.moj.cpp.progression.domain.transformation.ancillarydocs.core.EventTransformationException;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

public class TransformUtil {

    private TransformUtil() {
    }

    public static JsonObjectBuilder clone(final JsonObject jsonObject) {
        BiFunction<JsonValue, Deque<String>, Object> filter = (jsonValue, path) -> jsonValue;
        return cloneWithPathFilter(jsonObject, filter);
    }

    public static String pathString(final Deque<String> path) {
        return path.stream().collect(Collectors.joining("/"));
    }

    public static JsonObjectBuilder cloneWithPathFilter(final JsonObject jsonObject, BiFunction<JsonValue, Deque<String>, Object> filter) {
        return cloneObjectWithPathFilter(jsonObject, filter, new ArrayDeque<>());
    }

    public static JsonArrayBuilder cloneArrayWithPathFilter(final JsonArray jsonArray, BiFunction<JsonValue, Deque<String>, Object> filter, final Deque<String> path) {
        JsonArrayBuilder result = createArrayBuilder();
        for (int done = 0; done < jsonArray.size(); done++) {
            String property = "" + done;
            path.push(property);
            Object value;
            value = filter.apply(jsonArray.get(done), path);
            if (value instanceof JsonArray) {
                JsonArrayBuilder newJsonArray = cloneArrayWithPathFilter((JsonArray) value, filter, path);
                result.add(newJsonArray);
            } else if (value instanceof JsonObject) {
                result.add(cloneObjectWithPathFilter((JsonObject) value, filter, path));
            } else if (value instanceof JsonValue) {
                result.add((JsonValue) value);
            } else if (value instanceof JsonObjectBuilder) {
                result.add((JsonObjectBuilder) value);
            } else if (value instanceof JsonArrayBuilder) {
                result.add((JsonArrayBuilder) value);
            } else {
                throw new EventTransformationException(String.format("unexpected property %s type:%s ", value, (value == null ? "null" : value.getClass().getName())));
            }
            path.pop();
        }
        return result;
    }

    public static JsonObjectBuilder cloneObjectWithPathFilter(final JsonObject jsonObject, BiFunction<JsonValue, Deque<String>, Object> filter) {
        return cloneObjectWithPathFilter(jsonObject, filter, new ArrayDeque<>());
    }

    public static JsonObjectBuilder cloneObjectWithPathFilter(final JsonObject jsonObject, BiFunction<JsonValue, Deque<String>, Object> filter, final Deque<String> path) {

        final JsonObjectBuilder result = createObjectBuilder();
        for (final Map.Entry<String, JsonValue> property : jsonObject.entrySet()) {
            String key = property.getKey();
            path.push(key);
            Object value;
            value = filter.apply(property.getValue(), path);
            if (value instanceof JsonArray) {
                JsonArrayBuilder newJsonArray = cloneArrayWithPathFilter((JsonArray) value, filter, path);
                result.add(key, newJsonArray);

            } else if (value instanceof JsonObject) {
                result.add(key, cloneObjectWithPathFilter((JsonObject) value, filter, path));
            } else if (value instanceof JsonValue) {
                result.add(key, (JsonValue) value);
            } else if (value instanceof JsonObjectBuilder) {
                result.add(key, (JsonObjectBuilder) value);
            } else if (value instanceof JsonArrayBuilder) {
                result.add(key, (JsonArrayBuilder) value);
            } else {
                throw new EventTransformationException(String.format("unexpected property %s type:%s ", value, (value == null ? "null" : value.getClass().getName())));
            }
            path.pop();
        }
        return result;
    }


}
