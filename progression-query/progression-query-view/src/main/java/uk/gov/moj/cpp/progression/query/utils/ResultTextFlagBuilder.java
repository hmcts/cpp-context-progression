package uk.gov.moj.cpp.progression.query.utils;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@SuppressWarnings({"squid:S1166"})
public class ResultTextFlagBuilder {

    public JsonArray rebuildWithResultTextFlag(final JsonArray payload){
        final JsonArrayBuilder response = Json.createArrayBuilder();
        payload.stream().map(JsonObject.class::cast).map(this::rebuildObject).forEach(response::add);
        return response.build();

    }

    private JsonObject rebuildObject(final JsonObject payload) {
        final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
        final JsonNode jsonNode = objectMapper.valueToTree(payload);
        jsonNode.path("defendantJudicialResults").forEach(defendantJudicialResults ->
                        ((ObjectNode)defendantJudicialResults).put("useResultText", isUseResultText(Optional.ofNullable(defendantJudicialResults.get("resultText")).map(text -> text.asText("NA")).orElse("NA")))

        );
        jsonNode.path("defendantCaseJudicialResults").forEach(defendantCaseJudicialResults ->
                ((ObjectNode)defendantCaseJudicialResults).put("useResultText", isUseResultText(Optional.ofNullable(defendantCaseJudicialResults.get("resultText")).map(text -> text.asText("NA")).orElse("NA")))

        );
        try {
            return objectMapper.treeToValue(jsonNode, JsonObject.class);
        } catch (JsonProcessingException e) {
            return payload;
        }
    }

    private boolean isUseResultText(final String resultText){
        final String checkValue = Arrays.stream(resultText.split(" ")).limit(3).collect(Collectors.joining(" "));
        final Pattern pattern = Pattern.compile("\\w - \\w", Pattern.CASE_INSENSITIVE);
        final Matcher matcher = pattern.matcher(checkValue);
        return matcher.find();
    }
}
