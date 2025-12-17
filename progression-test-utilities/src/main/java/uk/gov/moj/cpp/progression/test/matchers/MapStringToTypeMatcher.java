package uk.gov.moj.cpp.progression.test.matchers;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.DeserializationFeature.READ_ENUMS_USING_TO_STRING;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_WITH_ZONE_ID;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_ENUMS_USING_TO_STRING;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_NULL_MAP_VALUES;
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.TimeZone.getTimeZone;
import static uk.gov.justice.services.common.converter.ZonedDateTimes.ISO_8601;

import uk.gov.justice.services.common.converter.exception.ConverterException;
import uk.gov.justice.services.common.converter.jackson.additionalproperties.AdditionalPropertiesModule;
import uk.gov.justice.services.common.converter.jackson.jsr353.InclusionAwareJSR353Module;

import java.io.IOException;
import java.time.ZonedDateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class MapStringToTypeMatcher<T> extends BaseMatcher<String> {
    private Class<T> clz;
    private final Matcher<T> matcher;

    private ObjectMapper objectMapper;

    public MapStringToTypeMatcher(Class<T> clz, Matcher<T> matcher) {
        this.clz = clz;
        this.matcher = matcher;

        objectMapper = new ObjectMapper()
                .registerModule(javaTimeModuleWithFormattedDateTime())
                .registerModule(new Jdk8Module())
                .registerModule(new ParameterNamesModule(PROPERTIES))
                .registerModule(new InclusionAwareJSR353Module())
                .registerModule(new AdditionalPropertiesModule())
                .configure(WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(WRITE_DATES_WITH_ZONE_ID, false)
                .configure(WRITE_NULL_MAP_VALUES, false)
                .disable(FAIL_ON_UNKNOWN_PROPERTIES)
                .setTimeZone(getTimeZone(UTC))
                .setSerializationInclusion(NON_ABSENT)
                .enable(WRITE_ENUMS_USING_TO_STRING)
                .enable(READ_ENUMS_USING_TO_STRING);
    }

    @Override
    public boolean matches(Object item) {
        final T subject = convert(clz, (String) item);
        return this.matcher.matches(subject);
    }

    @Override
    public void describeTo(Description description) {
        this.matcher.describeTo(description);
    }

    @Override
    public void describeMismatch(Object item, Description description) {
        final T subject = convert(clz, (String) item);
        this.matcher.describeMismatch(subject, description);
    }

    private T convert(Class<T> clazz, String source) {

        try {
            final T object = this.objectMapper.readValue(source, clazz);
            if (object == null) {
                throw new ConverterException(String.format("Failed to convert %s to Object", source));
            } else {
                return object;
            }
        } catch (IOException var4) {
            throw new IllegalArgumentException(String.format("Error while converting %s to JsonObject", source), var4);
        }
    }

    public static <T> MapStringToTypeMatcher<T> convertStringTo(Class<T> clazz, Matcher<T> matcher) {
        return new MapStringToTypeMatcher<>(clazz, matcher);
    }

    private JavaTimeModule javaTimeModuleWithFormattedDateTime() {
        final JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer(ofPattern(ISO_8601)));
        return javaTimeModule;
    }
}
