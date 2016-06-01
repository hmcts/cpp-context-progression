package uk.gov.moj.cpp.progression.query.view.service;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;

public class ProgressionHelperService {

    @Inject
    StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    ObjectMapper objectMapper;

    public <T> JsonArray arraysToJsonArray(List<T> sourceList) {
        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        sourceList.forEach((offence) -> {
            try {
                jsonArrayBuilder.add(stringToJsonObjectConverter.convert(objectMapper.writeValueAsString(offence)));
            } catch (IOException e) {
                throw new IllegalArgumentException(String.format("Error while converting %s toJsonObject", offence), e);
            }
        });
        return jsonArrayBuilder.build();
    }

  
}