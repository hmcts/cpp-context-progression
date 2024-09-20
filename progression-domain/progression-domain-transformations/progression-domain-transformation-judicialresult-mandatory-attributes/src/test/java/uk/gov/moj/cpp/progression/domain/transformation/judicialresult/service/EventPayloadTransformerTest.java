package uk.gov.moj.cpp.progression.domain.transformation.judicialresult.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.json.JsonValue.FALSE;
import static javax.json.JsonValue.TRUE;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.integer;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.randomEnum;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.APPLICATION_REFERRED_TO_COURT;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.BOXWORK_APPLICATION_REFERRED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.COURT_APPLICATION_ADDED_TO_CASE;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.COURT_APPLICATION_CREATED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.COURT_APPLICATION_UPDATED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.HEARING_APPLICATION_LINK_CREATED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.HEARING_CONFIRMED_CASE_STATUS_UPDATED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.HEARING_EXTENDED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.HEARING_INITIATE_ENRICHED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.HEARING_RESULTED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.HEARING_RESULTED_CASE_UPDATED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.LISTED_COURT_APPLICATION_CHANGED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.PROSECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.PROSECUTION_CASE_DEFENDANT_UPDATED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.PROSECUTION_CASE_OFFENCES_UPDATED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.SLOTS_BOOKED_FOR_APPLICATION;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.UNSCHEDULED_HEARING_LISTING_REQUESTED;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform;
import uk.gov.moj.cpp.progression.domain.transformation.judicialresult.exception.TransformationException;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class EventPayloadTransformerTest {

    private static final String JUDICIAL_RESULTS = "judicialResults";
    private static final String PUBLISHED_AS_A_PROMPT = "publishedAsAPrompt";
    private static final String EXCLUDED_FROM_RESULTS = "excludedFromResults";
    private static final String ALWAYS_PUBLISHED = "alwaysPublished";
    private static final String OFFENCES = "offences";


    public static Stream<Arguments> validEventToTransform() {
        return Stream.of(
                Arguments.of(BOXWORK_APPLICATION_REFERRED.getEventName()),
                Arguments.of(HEARING_EXTENDED.getEventName()),
                Arguments.of(HEARING_RESULTED.getEventName()),
                Arguments.of(COURT_APPLICATION_UPDATED.getEventName()),
                Arguments.of(PROSECUTION_CASE_OFFENCES_UPDATED.getEventName()),
                Arguments.of(PROSECUTION_CASE_DEFENDANT_UPDATED.getEventName()),
                Arguments.of(HEARING_RESULTED_CASE_UPDATED.getEventName()),
                Arguments.of(HEARING_INITIATE_ENRICHED.getEventName()),
                Arguments.of(COURT_APPLICATION_CREATED.getEventName()),
                Arguments.of(COURT_APPLICATION_ADDED_TO_CASE.getEventName()),
                Arguments.of(LISTED_COURT_APPLICATION_CHANGED.getEventName()),
                Arguments.of(APPLICATION_REFERRED_TO_COURT.getEventName()),
                Arguments.of(HEARING_APPLICATION_LINK_CREATED.getEventName()),
                Arguments.of(PROSECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED.getEventName()),
                Arguments.of(HEARING_CONFIRMED_CASE_STATUS_UPDATED.getEventName()),
                Arguments.of(UNSCHEDULED_HEARING_LISTING_REQUESTED.getEventName()),
                Arguments.of(SLOTS_BOOKED_FOR_APPLICATION.getEventName())
        );
    }

    private final EventPayloadTransformer eventPayloadTransformer = new EventPayloadTransformer();

    @Test
    public void shouldEvaluateRollUpPromptsToTrueWhenAllMandatoryAttributesAreFalse() {
        final JsonEnvelope event = prepareValidEventToTransformWithAllMandatoryAttributesSetToFalse();

        final JsonObject transformedPayload = eventPayloadTransformer.transform(event);

        assertThat(transformedPayload, isJson(allOf(
                withJsonPath("$.offences[0].judicialResults[0].publishedAsAPrompt", equalTo(FALSE)),
                withJsonPath("$.offences[0].judicialResults[0].excludedFromResults", equalTo(FALSE)),
                withJsonPath("$.offences[0].judicialResults[0].alwaysPublished", equalTo(FALSE)),
                withJsonPath("$.offences[0].judicialResults[0].publishedForNows", equalTo(FALSE)),
                withJsonPath("$.offences[0].judicialResults[0].rollUpPrompts", equalTo(TRUE))
        )));
    }

    @Test
    public void shouldEvaluateRollUpPromptsToFalseWhenOneOfTheMandatoryAttributesIsTrue() {
        final JsonEnvelope event = prepareValidEventToTransformWithOneOfTheMandatoryAttributesSetToTrue();

        final JsonObject transformedPayload = eventPayloadTransformer.transform(event);

        final JsonObject expectedJudicialResult = event.payloadAsJsonObject().getJsonArray(OFFENCES).getValuesAs(JsonObject.class).get(0)
                .getJsonArray(JUDICIAL_RESULTS).getValuesAs(JsonObject.class).get(0);

        assertThat(expectedJudicialResult.getBoolean(PUBLISHED_AS_A_PROMPT) ||
                expectedJudicialResult.getBoolean(EXCLUDED_FROM_RESULTS) ||
                expectedJudicialResult.getBoolean(ALWAYS_PUBLISHED), is(true));
        assertThat(transformedPayload, isJson(allOf(
                withJsonPath("$.offences[0].judicialResults[0].publishedAsAPrompt", equalTo(expectedJudicialResult.get(PUBLISHED_AS_A_PROMPT))),
                withJsonPath("$.offences[0].judicialResults[0].excludedFromResults", equalTo(expectedJudicialResult.get(EXCLUDED_FROM_RESULTS))),
                withJsonPath("$.offences[0].judicialResults[0].alwaysPublished", equalTo(expectedJudicialResult.get(ALWAYS_PUBLISHED))),
                withJsonPath("$.offences[0].judicialResults[0].publishedForNows", equalTo(FALSE)),
                withJsonPath("$.offences[0].judicialResults[0].rollUpPrompts", equalTo(FALSE))
        )));
    }

    @Test
    public void shouldThrowExceptionWhenOneOrMoreOfTheMandatoryAttributeIsMissing() {

        final JsonEnvelope invalidEvent = prepareInvalidEventWithOneOfTheMandatoryAttributeMissing();

        final TransformationException transformationException = assertThrows(
                TransformationException.class,
                () -> eventPayloadTransformer.transform(invalidEvent));

        assertThat(transformationException.getMessage(), matchesPattern("Mandatory attribute/s [\\w,]+ missing from judicialResult payload \\{.*\\}"));
    }

    @ParameterizedTest
    @MethodSource("validEventToTransform")
    public void shouldTransformRealEventsFromFile(final String eventToTransform) {
        final JsonObject eventPayload = loadTestFile(format("%s.json", eventToTransform));
        final String expectedPayloadInString = eventPayload.toString();
        assertThat(expectedPayloadInString, not(containsString("publishedForNows")));
        assertThat(expectedPayloadInString, not(containsString("rollUpPrompts")));

        final JsonObject transformedPayload = eventPayloadTransformer.transform(envelope()
                .with(metadataWithRandomUUID(eventToTransform))
                .withPayloadFrom(eventPayload).build());

        final String actualPayloadInString = transformedPayload.toString();
        assertThat(actualPayloadInString, containsString("publishedForNows"));
        assertThat(actualPayloadInString, containsString("rollUpPrompts"));
    }

    private JsonEnvelope prepareValidEventToTransformWithAllMandatoryAttributesSetToFalse() {
        final JsonObject judicialResults = createObjectBuilder().add(JUDICIAL_RESULTS, createArrayBuilder()
                .add(createObjectBuilder()
                        .add(PUBLISHED_AS_A_PROMPT, false)
                        .add(EXCLUDED_FROM_RESULTS, false)
                        .add(ALWAYS_PUBLISHED, false)
                )).build();

        return envelope()
                .with(metadataWithRandomUUID(randomEnum(EventToTransform.class).next().getEventName()))
                .withPayloadOf(createArrayBuilder().add(judicialResults).build(), OFFENCES)
                .build();
    }

    private JsonEnvelope prepareValidEventToTransformWithOneOfTheMandatoryAttributesSetToTrue() {
        final Integer attributePosition = integer(1, 3).next();
        final JsonObject judicialResults = createObjectBuilder().add(JUDICIAL_RESULTS, createArrayBuilder()
                .add(createObjectBuilder()
                        .add(PUBLISHED_AS_A_PROMPT, attributePosition == 1)
                        .add(EXCLUDED_FROM_RESULTS, attributePosition == 2)
                        .add(ALWAYS_PUBLISHED, attributePosition == 3)
                )).build();

        return envelope()
                .with(metadataWithRandomUUID(randomEnum(EventToTransform.class).next().getEventName()))
                .withPayloadOf(createArrayBuilder().add(judicialResults).build(), OFFENCES)
                .build();
    }

    private JsonEnvelope prepareInvalidEventWithOneOfTheMandatoryAttributeMissing() {
        final List<String> mandatoryAttributes = asList(PUBLISHED_AS_A_PROMPT, EXCLUDED_FROM_RESULTS, ALWAYS_PUBLISHED);
        Collections.shuffle(mandatoryAttributes);
        final Integer noOfAttributesToInclude = integer(0, 2).next();
        final JsonObjectBuilder judicialResult = createObjectBuilder();
        if (noOfAttributesToInclude != 0) {
            final List<String> attributesToInclude = mandatoryAttributes.subList(0, noOfAttributesToInclude);
            attributesToInclude.forEach(attribute -> judicialResult.add(attribute, false));
        }

        final JsonObject judicialResults = createObjectBuilder().add(JUDICIAL_RESULTS, createArrayBuilder()
                .add(judicialResult)).build();

        return envelope()
                .with(metadataWithRandomUUID(randomEnum(EventToTransform.class).next().getEventName()))
                .withPayloadOf(createArrayBuilder().add(judicialResults).build(), OFFENCES)
                .build();
    }

    private Matcher<String> matchesPattern(final String regex) {
        return new TypeSafeMatcher<String>() {
            @Override
            public void describeTo(final Description description) {
                description.appendText("matching pattern " + regex);
            }

            @Override
            protected boolean matchesSafely(final String item) {
                return item.matches(regex);
            }
        };
    }

    private JsonObject loadTestFile(final String resourceFileName) {
        try {
            final InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceFileName);
            try (final JsonReader jsonReader = Json.createReader(is)) {
                return jsonReader.readObject();
            }
        } catch (final Exception ex) {
            throw new RuntimeException("failed to load test file " + resourceFileName, ex);
        }
    }

}