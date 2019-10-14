package uk.gov.moj.cpp.progression.domain.transformation.ctl;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;

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

@SuppressWarnings({"squid:S106","squid:S00112"})
public class TransformUtil {

    private TransformUtil(){}

    public static JsonObjectBuilder clone(final JsonObject jsonObject) {
        final BiFunction<JsonValue, Deque<String>, Object> filter = (jsonValue, path) -> jsonValue;
        return cloneWithPathFilter(jsonObject, filter);
    }

    public static String pathString(final Deque<String> path) {
        return path.stream().collect(Collectors.joining("/"));
    }

    /**
    This method is for debugging
     */
    public static JsonObjectBuilder cloneVerbose(final JsonObject jsonObject) {
        final  BiFunction<JsonValue, Deque<String>, Object> filter = (jsonValue, path) -> {
            System.out.println(pathString(path) + "=>" + jsonValue);

            return jsonValue;
        };
        return cloneWithPathFilter(jsonObject, filter);
    }

    public static JsonObjectBuilder cloneWithPathFilter(final JsonObject jsonObject, BiFunction<JsonValue, Deque<String>, Object> filter) {
        return cloneObjectWithPathFilter(jsonObject, filter, new ArrayDeque<>());
    }

    public static JsonArrayBuilder cloneArrayWithPathFilter(final JsonArray jsonArray, BiFunction<JsonValue, Deque<String>, Object> filter, final Deque<String> path) {
        final JsonArrayBuilder result = createArrayBuilder();
        for (int done=0; done<jsonArray.size(); done++) {
            final String property = ""+done;
            path.push(property);
            Object value;
            value = filter.apply(jsonArray.get(done), path);
            if (value instanceof JsonArray) {
                final JsonArrayBuilder newJsonArray = cloneArrayWithPathFilter((JsonArray)value, filter, path);
                result.add(newJsonArray);
            }
            else if (value instanceof JsonObject) {
                result.add(cloneObjectWithPathFilter((JsonObject) value, filter, path));
            } else if (value instanceof JsonValue) {
                result.add((JsonValue)value);
            } else if (value instanceof JsonObjectBuilder) {
                result.add((JsonObjectBuilder)value);
            } else if (value instanceof JsonArrayBuilder) {
                result.add((JsonArrayBuilder)value);
            } else {
                throw new RuntimeException(String.format("unexpected property %s type:%s ", value, (value==null?"null":value.getClass().getName()) ));
            }
            path.pop();
        }
        return result;
    }

    public static JsonObjectBuilder cloneObjectWithPathFilter(final JsonObject jsonObject, BiFunction<JsonValue, Deque<String>, Object> filter) {
        return cloneObjectWithPathFilter(jsonObject, filter, new ArrayDeque<String>());
    }

    public static JsonObjectBuilder cloneObjectWithPathFilter(final JsonObject jsonObject, BiFunction<JsonValue, Deque<String>, Object> filter, final Deque<String> path) {

        final JsonObjectBuilder result = createObjectBuilder();
        for (final Map.Entry<String, JsonValue> entry : jsonObject.entrySet()) {
            final String property = entry.getKey();
            path.push(property);
            Object value;
            value = filter.apply(entry.getValue(), path);
            if (value instanceof JsonArray) {
               final JsonArrayBuilder newJsonArray = cloneArrayWithPathFilter((JsonArray)value, filter, path);
                result.add(property, newJsonArray);

            }
            else if (value instanceof JsonObject) {
                result.add(property, cloneObjectWithPathFilter((JsonObject) value, filter, path));
            } else if (value instanceof JsonValue) {
                result.add(property, (JsonValue)value);
            } else if (value instanceof JsonObjectBuilder) {
                result.add(property, (JsonObjectBuilder)value);
            } else if (value instanceof JsonArrayBuilder) {
                result.add(property, (JsonArrayBuilder)value);
            } else {
                throw new RuntimeException(String.format("unexpected property %s type:%s ", value, (value==null?"null":value.getClass().getName()) ));
            }
            path.pop();
        }
        return result;
    }



}
