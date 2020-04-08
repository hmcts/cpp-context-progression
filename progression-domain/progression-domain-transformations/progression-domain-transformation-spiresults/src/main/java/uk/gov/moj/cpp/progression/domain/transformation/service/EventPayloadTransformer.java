package uk.gov.moj.cpp.progression.domain.transformation.service;

import static java.lang.String.format;
import static javax.json.Json.createReader;
import static uk.gov.moj.cpp.progression.domain.transformation.EventMapper.getMappedJsonPaths;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.transformation.domain.Prompt;
import uk.gov.moj.cpp.progression.domain.transformation.domain.ResultDefinition;
import uk.gov.moj.cpp.progression.domain.transformation.exception.TransformationException;
import uk.gov.moj.cpp.progression.domain.transformation.helper.JsonPathHelper;

import java.io.StringReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.json.JsonReader;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventPayloadTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventPayloadTransformer.class);

    public static final String JUDICIAL_RESULTS_ATTRIBUTE = "judicialResults";
    public static final String JUDICIAL_RESULT_PROMPTS_ATTRIBUTE = "judicialResultPrompts";
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final String DATE_OF_HEARING_CORRECT_SPELLING = "Date of hearing";
    public static final String DATE_OF_HEARING_INCORRECT_SPELLING = "Date of hearig";
    public static final String LABEL_ATTRIBUTE_NAME = "label";
    public static final String ATTRIBUTE_VALUE_YES = "Y";

    private Map<String, String> hearingSittingDateMap = Maps.newHashMap();

    private ResultDefinitionService resultDefinitionService;
    private HearingService hearingService;

    public EventPayloadTransformer() {
        resultDefinitionService = new ResultDefinitionService();
        hearingService = new HearingService();
    }

    public javax.json.JsonObject transform(final JsonEnvelope eventEnvelope) {
        final String eventPayload = eventEnvelope.payload().toString();
        final String eventName = eventEnvelope.metadata().name();
        final Optional<String> hearingId = getHearingIdFromEvent(eventPayload, eventName);

        final JsonObject payloadToTransform = new Gson().fromJson(eventPayload, JsonObject.class);
        final String preTransformedPayload = payloadToTransform.toString();

        LOGGER.debug("Before: Payload '{}'", preTransformedPayload);
        process(payloadToTransform, hearingId);
        final String postTransformedPayload = payloadToTransform.toString();
        LOGGER.debug("After: Payload '{}'", postTransformedPayload);

        try (final JsonReader jsonReader = createReader(new StringReader(postTransformedPayload))) {
            return jsonReader.readObject();
        }
    }

    private void process(final JsonElement element, final Optional<String> backupHearingId) {

        if (element.isJsonObject()) {
            final Set<Map.Entry<String, JsonElement>> entries = element.getAsJsonObject().entrySet();
            for (final Map.Entry<String, JsonElement> el : entries) {
                if (JUDICIAL_RESULTS_ATTRIBUTE.equals(el.getKey())) {
                    final JsonElement judicialResults = el.getValue();
                    updateJudicialResults(judicialResults, backupHearingId);
                } else {
                    process(el.getValue(), backupHearingId);
                }
            }
        } else if (element.isJsonArray()) {
            element.getAsJsonArray().iterator().forEachRemaining(ae -> process(ae, backupHearingId));
        }
    }

    private void updateJudicialResults(final JsonElement judicialResults, final Optional<String> backupHearingId) {
        if (null != judicialResults && judicialResults.isJsonArray()) {
            judicialResults.getAsJsonArray().iterator().forEachRemaining(arrayElement -> {
                if (arrayElement.isJsonObject()) {
                    JsonObject judicialResult = (JsonObject) arrayElement;
                    final String resultDefinitionLabel = getJudicialResultLabel(judicialResult);
                    final String hearingDate = getHearingSittingDate(judicialResult, backupHearingId);
                    final ResultDefinition resultDefinition = resultDefinitionService.getResultDefinition(hearingDate, resultDefinitionLabel);
                    judicialResult.addProperty("urgent", ATTRIBUTE_VALUE_YES.equalsIgnoreCase(resultDefinition.getUrgent()));
                    judicialResult.addProperty("d20", ATTRIBUTE_VALUE_YES.equalsIgnoreCase(resultDefinition.getD20()));

                    judicialResult.addProperty("judicialResultTypeId", resultDefinition.getId());
                    judicialResult.addProperty("resultText", "NA");
                    judicialResult.addProperty("terminatesOffenceProceedings", false);
                    judicialResult.addProperty("lifeDuration", false);
                    judicialResult.addProperty("publishedAsAPrompt", false);
                    judicialResult.addProperty("excludedFromResults", false);
                    judicialResult.addProperty("alwaysPublished", false);

                    if (judicialResult.has(JUDICIAL_RESULT_PROMPTS_ATTRIBUTE)) {
                        updateJudicialResultPrompts(judicialResult.getAsJsonArray(JUDICIAL_RESULT_PROMPTS_ATTRIBUTE), resultDefinition, hearingDate);
                    }
                }

            });
        }
    }

    private String getJudicialResultLabel(final JsonObject judicialResult) {
        if (judicialResult.has(LABEL_ATTRIBUTE_NAME)) {
            return judicialResult.get(LABEL_ATTRIBUTE_NAME).getAsString();
        }

        // fallback - bad event - look for an attribute with empty string key
        if (judicialResult.has("")) {
            LOGGER.info("Reinserting label attribute for judicial result object");
            final String resultDefinitionLabel = judicialResult.get("").getAsString();
            judicialResult.addProperty(LABEL_ATTRIBUTE_NAME, resultDefinitionLabel);
            judicialResult.remove("");
            return resultDefinitionLabel;
        }

        throw new TransformationException("Cannot find label attribute for judicialResult object");
    }

    private String getHearingSittingDate(final JsonObject judicialResult, final Optional<String> backupHearingId) {


        final boolean orderedHearingIdAvailable = judicialResult.has("orderedHearingId");

        if (!orderedHearingIdAvailable && !backupHearingId.isPresent()) {
            throw new TransformationException("No hearing ID found");
        }

        final String hearingIdToUse = orderedHearingIdAvailable ? judicialResult.get("orderedHearingId").getAsString() : backupHearingId.get();

        if (hearingSittingDateMap.containsKey(hearingIdToUse)) {
            return hearingSittingDateMap.get(hearingIdToUse);
        }

        final LocalDate hearingSittingDate = hearingService.getHearingSittingDate(hearingIdToUse);
        final String formattedHearingSittingDate = hearingSittingDate.format(DATE_TIME_FORMATTER);

        hearingSittingDateMap.put(hearingIdToUse, formattedHearingSittingDate);
        return formattedHearingSittingDate;
    }

    @SuppressWarnings("squid:S3776")
    private void updateJudicialResultPrompts(final JsonElement judicialResultPrompts, final ResultDefinition resultDefinition, final String eventPublishedDate) {

        if (null != judicialResultPrompts && judicialResultPrompts.isJsonArray()) {
            judicialResultPrompts.getAsJsonArray().iterator().forEachRemaining(arrayElement -> {
                if (arrayElement.isJsonObject()) {
                    JsonObject judicialResultPrompt = (JsonObject) arrayElement;
                    final String isAvailableForCourtExtractAttribute = "isAvailableForCourtExtract";
                    final boolean availableForCourtExtractAttributePresent = judicialResultPrompt.has(isAvailableForCourtExtractAttribute);
                    final boolean isAvailableForCourtExtract = availableForCourtExtractAttributePresent && judicialResultPrompt.get(isAvailableForCourtExtractAttribute).getAsBoolean();
                    final String promptLabel = judicialResultPrompt.get(LABEL_ATTRIBUTE_NAME).getAsString();
                    final Optional<Prompt> optionalPrompt = getMatchingPrompt(resultDefinition, promptLabel);
                    if (!optionalPrompt.isPresent()) {
                        throw new TransformationException(format("No matching prompt found for label '%s' for date '%s' in result definition with ID '%s'", promptLabel, eventPublishedDate, resultDefinition.getId()));
                    }
                    judicialResultPrompt.addProperty("judicialResultPromptTypeId", optionalPrompt.get().getId());
                    judicialResultPrompt.addProperty("courtExtract", isAvailableForCourtExtract ? "Y" : "N");
                    if (availableForCourtExtractAttributePresent) {
                        judicialResultPrompt.remove(isAvailableForCourtExtractAttribute);
                    }
                }
            });
        }
    }

    private Optional<Prompt> getMatchingPrompt(final ResultDefinition resultDefinition, final String promptLabel) {
        return resultDefinition.getPrompts().stream().filter(p -> {
            final boolean promptFound = p.getLabel().equals(promptLabel);

            if (promptFound) {
                return true;
            }

            // this is a workaround for mispelt prompt label
            final List<String> possiblePromptLabelOptions = Lists.newArrayList(DATE_OF_HEARING_CORRECT_SPELLING, DATE_OF_HEARING_INCORRECT_SPELLING);
            return possiblePromptLabelOptions.stream().anyMatch(promptLabel::equalsIgnoreCase);
        }).findFirst();
    }

    private Optional<String> getHearingIdFromEvent(final String eventPayload, final String eventName) {
        for (final String path : getMappedJsonPaths(eventName)) {
            final Optional<String> pathValue = JsonPathHelper.getFirstValueForJsonPath(eventPayload, path);
            if (pathValue.isPresent()) {
                return Optional.of(pathValue.get());
            }
        }

        return Optional.empty();
    }


    @VisibleForTesting
    void setResultDefinitionService(final ResultDefinitionService resultDefinitionService) {
        this.resultDefinitionService = resultDefinitionService;
    }

    @VisibleForTesting
    public void setHearingService(final HearingService hearingService) {
        this.hearingService = hearingService;
    }
}
