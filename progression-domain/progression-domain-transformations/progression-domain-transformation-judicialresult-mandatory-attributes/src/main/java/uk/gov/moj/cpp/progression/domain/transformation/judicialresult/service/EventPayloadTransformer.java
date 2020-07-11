package uk.gov.moj.cpp.progression.domain.transformation.judicialresult.service;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static javax.json.Json.createReader;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.transformation.judicialresult.exception.TransformationException;

import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.json.JsonReader;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventPayloadTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventPayloadTransformer.class);

    private static final String JUDICIAL_RESULTS_ATTRIBUTE = "judicialResults";
    private static final String PUBLISHED_AS_A_PROMPT_ATTRIBUTE = "publishedAsAPrompt";
    private static final String EXCLUDED_FROM_RESULTS_ATTRIBUTE = "excludedFromResults";
    private static final String ALWAYS_PUBLISHED_ATTRIBUTE = "alwaysPublished";
    private static final String PUBLISHED_FOR_NOWS_ATTRIBUTE = "publishedForNows";
    private static final String ROLL_UP_PROMPTS_ATTRIBUTE = "rollUpPrompts";

    public javax.json.JsonObject transform(final JsonEnvelope eventEnvelope) {
        final String eventPayload = eventEnvelope.payload().toString();

        final JsonObject payloadToTransform = new Gson().fromJson(eventPayload, JsonObject.class);
        final String preTransformedPayload = payloadToTransform.toString();

        LOGGER.debug("Before: Payload '{}'", preTransformedPayload);
        process(payloadToTransform);
        final String postTransformedPayload = payloadToTransform.toString();
        LOGGER.debug("After: Payload '{}'", postTransformedPayload);

        try (final JsonReader jsonReader = createReader(new StringReader(postTransformedPayload))) {
            return jsonReader.readObject();
        }
    }

    private void process(final JsonElement element) {
        if (element.isJsonObject()) {
            final Set<Map.Entry<String, JsonElement>> entries = element.getAsJsonObject().entrySet();
            for (final Map.Entry<String, JsonElement> el : entries) {
                if (JUDICIAL_RESULTS_ATTRIBUTE.equals(el.getKey())) {
                    final JsonElement judicialResults = el.getValue();
                    updateJudicialResults(judicialResults);
                } else {
                    process(el.getValue());
                }
            }
        } else if (element.isJsonArray()) {
            element.getAsJsonArray().iterator().forEachRemaining(this::process);
        }
    }

    private void updateJudicialResults(final JsonElement judicialResults) {
        if (null != judicialResults && judicialResults.isJsonArray()) {
            judicialResults.getAsJsonArray().iterator().forEachRemaining(arrayElement -> {
                if (arrayElement.isJsonObject()) {
                    final JsonObject judicialResult = (JsonObject) arrayElement;
                    final boolean rollUpPromptsFromValue = getRollUpPromptsFrom(judicialResult);
                    judicialResult.addProperty(PUBLISHED_FOR_NOWS_ATTRIBUTE, false);
                    judicialResult.addProperty(ROLL_UP_PROMPTS_ATTRIBUTE, rollUpPromptsFromValue);
                }
            });
        }
    }

    private boolean getRollUpPromptsFrom(final JsonObject judicialResult) {
        if (judicialResult.has(PUBLISHED_AS_A_PROMPT_ATTRIBUTE) && judicialResult.has(EXCLUDED_FROM_RESULTS_ATTRIBUTE)
                && judicialResult.has(ALWAYS_PUBLISHED_ATTRIBUTE)) {
            return !judicialResult.get(PUBLISHED_AS_A_PROMPT_ATTRIBUTE).getAsBoolean() &&
                    !judicialResult.get(EXCLUDED_FROM_RESULTS_ATTRIBUTE).getAsBoolean() &&
                    !judicialResult.get(ALWAYS_PUBLISHED_ATTRIBUTE).getAsBoolean();
        }

        final List<String> missingAttributes = newArrayList();
        if (!judicialResult.has(PUBLISHED_AS_A_PROMPT_ATTRIBUTE)) {
            missingAttributes.add(PUBLISHED_AS_A_PROMPT_ATTRIBUTE);
        }
        if (!judicialResult.has(EXCLUDED_FROM_RESULTS_ATTRIBUTE)) {
            missingAttributes.add(EXCLUDED_FROM_RESULTS_ATTRIBUTE);
        }
        if (!judicialResult.has(ALWAYS_PUBLISHED_ATTRIBUTE)) {
            missingAttributes.add(ALWAYS_PUBLISHED_ATTRIBUTE);
        }
        throw new TransformationException(format("Mandatory attribute/s %s missing from judicialResult payload %s",
                String.join(",", missingAttributes), judicialResult.toString()));
    }
}