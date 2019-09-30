package uk.gov.moj.cpp.data.anonymization.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import static javax.json.Json.createObjectBuilder;

@SuppressWarnings({"squid:S3008","squid:S3776", "squid:S134", "squid:MethodCyclomaticComplexity"})
public class ParseDataGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParseDataGenerator.class);
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");
    private static Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private final PastDateGenerator pastDateGenerator = new PastDateGenerator();
    private final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
    private final List<String> nonAnonymisedList = Collections.unmodifiableList(Arrays.asList("resultLevel", "type", "label", "state"));

    @SuppressWarnings({"squid:S1166","squid:S2139"})
    public String convert(String data) {

        final JsonObjectBuilder transformedPayloadObjectBuilder = createObjectBuilder();
        try {
            final JsonObject payload = mapper.readValue(StringEscapeUtils.unescapeJson(data.trim()), JsonObject.class);
            final JsonObject anonymisedPayload = processJsonPayload(payload, transformedPayloadObjectBuilder).build();
            return anonymisedPayload.toString();

        } catch (IOException e) {
            LOGGER.error("data " + data + " unable to parse: " + e.getMessage(), e.getCause());
            throw new IllegalStateException("data " + data + " unable to parse: " + e.getMessage());
        }
    }

    private  JsonObjectBuilder processJsonPayload(JsonObject payload,  JsonObjectBuilder transformedPayloadObjectBuilder) {
        final Iterator<?> keys = payload.keySet().iterator();
        while (keys.hasNext()) {
            final String fieldName = (String) keys.next();
            final JsonValue jsonValue = payload.get(fieldName);
            if (jsonValue.getValueType().equals(JsonValue.ValueType.OBJECT)) {
                JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
                jsonObjectBuilder = processJsonPayload((JsonObject) jsonValue, jsonObjectBuilder);
                transformedPayloadObjectBuilder.add(fieldName, jsonObjectBuilder);
            } else if (jsonValue.getValueType().equals(JsonValue.ValueType.ARRAY)) {
                final JsonArray jsonArray = ((JsonArray) jsonValue);
                final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
                JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
                for (int counter = 0; counter < jsonArray.size(); counter++) {
                    final JsonValue value = jsonArray.get(counter);
                    if (value.getValueType().equals(JsonValue.ValueType.OBJECT)) {
                        final JsonObject jsonObject = jsonArray.getJsonObject(counter);
                        jsonObjectBuilder = processJsonPayload(jsonObject, jsonObjectBuilder);
                        jsonArrayBuilder.add(jsonObjectBuilder);
                    } else if (value.getValueType().equals(JsonValue.ValueType.STRING)) {
                        jsonArrayBuilder.add(value);
                    }
                }
                transformedPayloadObjectBuilder.add(fieldName, jsonArrayBuilder);
            } else if (jsonValue.getValueType().equals(JsonValue.ValueType.STRING)) {
                final String fieldValue = payload.getString(fieldName);
                setFieldValue( fieldName, fieldValue, transformedPayloadObjectBuilder);
            } else if (jsonValue.getValueType().equals(JsonValue.ValueType.NUMBER)) {
                transformedPayloadObjectBuilder.add(fieldName, 123456);
            } else if (jsonValue.getValueType().equals(JsonValue.ValueType.TRUE) || jsonValue.getValueType().equals(JsonValue.ValueType.FALSE)) {
                final Boolean fieldValue = payload.getBoolean(fieldName);
                transformedPayloadObjectBuilder.add(fieldName, fieldValue ? JsonValue.TRUE : JsonValue.FALSE);
            }
        }
        return transformedPayloadObjectBuilder;
    }

    private void setFieldValue( String fieldName, String fieldValue, JsonObjectBuilder transformedPayloadObjectBuilder) {
        if(UUID_PATTERN.matcher(fieldValue).matches() || nonAnonymisedList.contains(fieldName)) {
            transformedPayloadObjectBuilder.add(fieldName, fieldValue);
        } else if (DATE_PATTERN.matcher(fieldValue).matches()) {
            transformedPayloadObjectBuilder.add(fieldName, pastDateGenerator.convert());
        } else {
            transformedPayloadObjectBuilder.add(fieldName, "XXXX");
        }
    }
}