package uk.gov.moj.cpp.progression;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class CreateJsonSampleUtil {
    public static final String PROGRESSION_COMMAND_PROGRESSION_COMMAND_API_SRC_RAML_JSON = "./progression-command/progression-command-api/src/raml/json";
    public static final String PROGRESSION_COMMAND_PROGRESSION_COMMAND_HANDLER_SRC_RAML_JSON = "./progression-command/progression-command-handler/src/raml/json";
    public static final String PROGRESSION_EVENT_PROGRESSION_EVENT_LISTENER_SRC_RAML_JSON = "./progression-event/progression-event-listener/src/raml/json";
    public static final String PROGRESSION_EVENT_HEARING_EVENT_PROCESSOR_SRC_RAML_JSON = "./progression-event/progression-event-processor/src/raml/json";

    private CreateJsonSampleUtil() {

    }
    public static ObjectMapper createObjectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
        return objectMapper;
    }

}

