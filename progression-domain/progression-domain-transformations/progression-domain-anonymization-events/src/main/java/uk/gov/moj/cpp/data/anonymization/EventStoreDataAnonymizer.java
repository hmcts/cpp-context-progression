package uk.gov.moj.cpp.data.anonymization;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;
import uk.gov.moj.cpp.data.anonymization.generator.AnonymizeGenerator;
import uk.gov.moj.cpp.data.anonymization.generator.AnonymizerType;
import uk.gov.moj.cpp.data.anonymization.generator.DummyNumberReplacer;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataOf;

@SuppressWarnings({"squid:S3776", "squid:S134", "squid:MethodCyclomaticComplexity"})
@Transformation
public final class EventStoreDataAnonymizer implements EventTransformation {

    private final AnonymizeGenerator anonymizeGenerator;
    private Enveloper enveloper;

    private final Map<String, Map<String, String>> fieldRuleMap;

    public EventStoreDataAnonymizer() throws IOException {
        fieldRuleMap = new RuleParser().loadAnanymisationRules("/data.anonymisation.json");
        anonymizeGenerator = new AnonymizeGenerator();
    }

    @Override
    public Action actionFor(JsonEnvelope event) {
        if(isApplicable(event)) {
            return new Action(true, false, false);
        }
        else {
            return Action.NO_ACTION;
        }
    }

    @Override
    public boolean isApplicable(final JsonEnvelope event) {
        return fieldRuleMap.containsKey(event.metadata().name());
    }

    @Override
    public Stream<JsonEnvelope> apply(final JsonEnvelope event) {

        final JsonEnvelope transformedEvent = buildTransformedPayload(event);
        final JsonEnvelope transformedEnvelope = enveloper.withMetadataFrom(event, transformedEvent.metadata().asJsonObject().getString("name")).apply(transformedEvent.payload());
        return Stream.of(transformedEnvelope);
    }

    @Override
    public void setEnveloper(final Enveloper enveloper) {
        this.enveloper = enveloper;
    }

    public JsonEnvelope buildTransformedPayload(JsonEnvelope event) {
        final String eventName = event.metadata().name();
        final JsonObjectBuilder transformedPayloadObjectBuilder = createObjectBuilder();
        final JsonObject payload = event.payloadAsJsonObject();
        final Map<String, String> eventFieldRuleMap = fieldRuleMap.get(eventName);
        final JsonObject transformedPayload = processJsonPayload(payload, eventFieldRuleMap, transformedPayloadObjectBuilder).build();
        return envelopeFrom(metadataOf(event.metadata().asJsonObject().getString("id"), eventName).build(), transformedPayload);

    }

    public JsonObjectBuilder processJsonPayload(JsonObject payload, Map<String, String> eventFieldRuleMap, JsonObjectBuilder transformedPayloadObjectBuilder) {
        final Iterator<?> keys = payload.keySet().iterator();
        while (keys.hasNext()) {
            final String fieldName = (String) keys.next();
            final JsonValue jsonValue = payload.get(fieldName);
            if (jsonValue.getValueType().equals(JsonValue.ValueType.OBJECT)) {
                JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
                jsonObjectBuilder = processJsonPayload((JsonObject) jsonValue, eventFieldRuleMap, jsonObjectBuilder);
                transformedPayloadObjectBuilder.add(fieldName, jsonObjectBuilder);
            } else if (jsonValue.getValueType().equals(JsonValue.ValueType.ARRAY)) {
                final JsonArray jsonArray = ((JsonArray) jsonValue);
                final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
                JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
                for (int counter = 0; counter < jsonArray.size(); counter++) {
                    final JsonValue value = jsonArray.get(counter);
                    if(value.getValueType().equals(JsonValue.ValueType.OBJECT)) {
                        final JsonObject jsonObject = jsonArray.getJsonObject(counter);
                        jsonObjectBuilder = processJsonPayload(jsonObject, eventFieldRuleMap, jsonObjectBuilder);
                        jsonArrayBuilder.add(jsonObjectBuilder);
                    } else if(value.getValueType().equals(JsonValue.ValueType.STRING)){
                        final String fieldValue = value.toString();
                        final String rule = eventFieldRuleMap.get(fieldName);
                        setFieldValue(rule, fieldName, fieldValue, transformedPayloadObjectBuilder);
                    }
                }
                transformedPayloadObjectBuilder.add(fieldName, jsonArrayBuilder);
            } else if (jsonValue.getValueType().equals(JsonValue.ValueType.STRING)) {
                final String fieldValue = payload.getString(fieldName);
                final String rule = eventFieldRuleMap.get(fieldName);
                setFieldValue(rule, fieldName, fieldValue, transformedPayloadObjectBuilder);
            } else if (jsonValue.getValueType().equals(JsonValue.ValueType.NUMBER)) {
                final String fieldValue = payload.getJsonNumber(fieldName).toString();
                final String rule = eventFieldRuleMap.get(fieldName);
                setFieldValue(rule, fieldName, fieldValue, transformedPayloadObjectBuilder);
            } else if (jsonValue.getValueType().equals(JsonValue.ValueType.TRUE) || jsonValue.getValueType().equals(JsonValue.ValueType.FALSE)) {
                final Boolean fieldValue = payload.getBoolean(fieldName);
                transformedPayloadObjectBuilder.add(fieldName, fieldValue ? JsonValue.TRUE : JsonValue.FALSE);
            }
        }
        return transformedPayloadObjectBuilder;

    }

    private void setFieldValue(String rule, String fieldName, String fieldValue, JsonObjectBuilder transformedPayloadObjectBuilder) {
        if (null != rule) {
            final Object replacedFieldValue = applyAnonymizationRule(rule, fieldValue);
            if(replacedFieldValue instanceof String) {
                transformedPayloadObjectBuilder.add(fieldName, (String) replacedFieldValue);
            }
            else if(replacedFieldValue instanceof BigInteger) {
                transformedPayloadObjectBuilder.add(fieldName, (BigInteger) replacedFieldValue);
            }
        } else {
            transformedPayloadObjectBuilder.add(fieldName, fieldValue);
        }
    }

    private Object applyAnonymizationRule(String fieldRule, String fieldValue) {
        if(fieldRule.startsWith(AnonymizerType.DUMMY_NUMBER_PREFIX.toString())) {
            return DummyNumberReplacer.replace(fieldRule);
        }
        else {
            return anonymizeGenerator.getGenerator(fieldRule).convert(fieldValue);
        }
    }
}
