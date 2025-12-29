package uk.gov.moj.cpp.progression.query.utils;

import uk.gov.justice.services.common.converter.Converter;

import java.io.StringReader;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonReader;

import com.google.common.base.Strings;


public class StringToJsonArray implements Converter<String, JsonArray> {

    public JsonArray convert(final String source) {
        if (Strings.isNullOrEmpty(source)) {
            return JsonObjects.createArrayBuilder().build();
        }
        JsonArray jsonArray;
        try (JsonReader reader = JsonObjects.createReader(new StringReader(source))) {
            jsonArray = reader.readArray();
        }
        return jsonArray;
    }
}