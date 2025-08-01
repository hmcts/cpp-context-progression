package uk.gov.moj.cpp.progression.service;

import static javax.json.Json.createReader;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.json.JsonReader;

import com.google.common.io.Resources;
import org.json.JSONException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

@ExtendWith(MockitoExtension.class)
public class DefendantUpdateDifferenceServiceTest {
    
    @Spy
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new JsonObjectConvertersFactory().objectToJsonObjectConverter();

    public static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of("allfields"),
                Arguments.of("parent-guardian-remove"),
                Arguments.of("new-fields"),
                Arguments.of("custodial-establishment"),
                Arguments.of("matched-defendant-without-custody"),
                Arguments.of("without-custodial-establishment")
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    public void calculateDefendantUpdate(final String testCase) throws IOException, JSONException {

        DefendantUpdateDifferenceService defendantUpdateDifferenceService = new DefendantUpdateDifferenceService();

        Defendant originalDefendantPreviousVersion = jsonObjectToObjectConverter.convert(
                getJson("/original-defendant-previous-version.json", testCase), Defendant.class);

        DefendantUpdate originalDefendantNextVersion = jsonObjectToObjectConverter.convert(
                getJson("/original-defendant-update-next-version.json", testCase), DefendantUpdate.class);

        Defendant matchedDefendantPreviousVersion = jsonObjectToObjectConverter.convert(
                getJson("/matched-defendant-previous-version.json", testCase), Defendant.class);

        DefendantUpdate matchedDefendantExceptedUpdate = jsonObjectToObjectConverter.convert(
                getJson("/matched-defendant-expected-version.json", testCase), DefendantUpdate.class);

        DefendantUpdate matchedDefendantActualUpdate = defendantUpdateDifferenceService.calculateDefendantUpdate(
                originalDefendantPreviousVersion,
                originalDefendantNextVersion,
                matchedDefendantPreviousVersion);
        JSONAssert.assertEquals(objectToJsonObjectConverter.convert(matchedDefendantActualUpdate).toString(),
                objectToJsonObjectConverter.convert(matchedDefendantExceptedUpdate).toString(), JSONCompareMode.LENIENT);
    }

    private JsonObject getJson(final String file, final String testCase) throws IOException {
        final String jsonString = Resources.toString(Resources.getResource("defendant-update/" + testCase + "/" + file), Charset.defaultCharset());

        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readObject();
        }
    }
}