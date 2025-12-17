package uk.gov.moj.cpp.progression.query.utils;

import uk.gov.justice.services.common.converter.Converter;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonReader;

import com.google.common.base.Strings;


public class StringToJsonArray implements Converter<String, JsonArray> {

    public JsonArray convert(final String source) {
        if (Strings.isNullOrEmpty(source)) {
            return Json.createArrayBuilder().build();
        }
        JsonArray jsonArray;
        try (JsonReader reader = Json.createReader(new StringReader(source))) {
            jsonArray = reader.readArray();
        }
        return jsonArray;
    }
}