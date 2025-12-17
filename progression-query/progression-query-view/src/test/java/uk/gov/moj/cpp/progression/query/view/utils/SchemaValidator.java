package uk.gov.moj.cpp.progression.query.view.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.fail;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

public class SchemaValidator {

    public static void validateObjectAgainstSchema(final Object objectToValidate, final String jsonSchemaPath) throws IOException {
        final URL resource = Resources.getResource(jsonSchemaPath);
        final String jsonSchema = Resources.toString(resource, Charset.defaultCharset());
        assertThat(jsonSchema, notNullValue());

        final JSONObject schemaObject = new JSONObject(new JSONTokener(jsonSchema));

        final JSONObject jsonObject = convert(objectToValidate);

        try {
            final SchemaLoader loader = SchemaLoader.builder()
                    .schemaJson(schemaObject)
                    .httpClient(new CustomSchemaClient()) // Use custom schema client to load external schemas
                    .build();

            final Schema schema = loader.load().build();

            schema.validate(jsonObject);
        } catch (final ValidationException e) {
            final StringBuffer errorMessage = new StringBuffer();
            printCausingExceptionInfo(Lists.newArrayList(e), errorMessage);

            fail(errorMessage.toString());
        }
    }

    private static void printCausingExceptionInfo(final List<ValidationException> causingExceptions, final StringBuffer errorMessageStringBuffer) {
        for (final ValidationException causingException : causingExceptions) {
            errorMessageStringBuffer.append(causingException.getMessage()).append("\n");

            final List<ValidationException> subExceptions = causingException.getCausingExceptions();
            if (subExceptions.size() > 0) {
                printCausingExceptionInfo(subExceptions, errorMessageStringBuffer);
            }
        }

    }

    private static JSONObject convert(final Object objectToValidate) {
        try {
            final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
            objectMapper.registerModule(new JavaTimeModule());

            return new JSONObject(objectMapper.writeValueAsString(objectToValidate));
        } catch (final IOException e) {
            throw new IllegalArgumentException(String.format("Error while converting %s toJsonObject", objectToValidate), e);
        }
    }

}
