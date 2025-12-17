package uk.gov.justice.api.resource.utils.payload;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ResultTextFlagBuilder {

    public static final String RESULT_TEXT = "resultText";

    public JsonObject rebuildWithResultTextFlag(final JsonObject payload) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
        final JsonNode jsonNode = objectMapper.valueToTree(payload);
        jsonNode.path("defendant").path("offences").forEach(offence ->
                offence.path("results").forEach(result ->
                        ((ObjectNode)result).put(RESULT_TEXT, isUseResultText(Optional.ofNullable(result.get(RESULT_TEXT)).map(text -> text.asText("NA")).orElse("NA")))
                )
        );

        jsonNode.path("defendant").path("results").forEach(result ->
                        ((ObjectNode)result).put(RESULT_TEXT, isUseResultText(Optional.ofNullable(result.get(RESULT_TEXT)).map(text -> text.asText("NA")).orElse("NA")))
        );

        return objectMapper.treeToValue(jsonNode, JsonObject.class);
    }

    private String isUseResultText(final String resultText){
        final String checkValue = Arrays.stream(resultText.split(" ")).limit(3).collect(Collectors.joining(" "));
        final Pattern pattern = Pattern.compile("\\w - \\w", Pattern.CASE_INSENSITIVE);
        final Matcher matcher = pattern.matcher(checkValue);
        if(matcher.find()){
            return resultText;
        }else{
            return "NA";
        }
    }
}
