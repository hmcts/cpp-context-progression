package uk.gov.moj.cpp.progression.service;

import static org.apache.activemq.artemis.utils.JsonLoader.createReader;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;

import javax.json.JsonObject;
import javax.json.JsonReader;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

@RunWith(Parameterized.class)
public class DefendantUpdateDifferenceServiceTest {

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    @Spy
    @InjectMocks
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter();
    @Spy
    @InjectMocks
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();
    private String testCase;

    public DefendantUpdateDifferenceServiceTest(final String testCase) {
        this.testCase = testCase;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"allfields"},{"parent-guardian-remove"}, {"new-fields"}
        });
    }

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Test
    public void calculateDefendantUpdate() throws IOException {

        DefendantUpdateDifferenceService defendantUpdateDifferenceService = new DefendantUpdateDifferenceService();

        Defendant originalDefendantPreviousVersion = jsonObjectToObjectConverter.convert(
                getJson("/original-defendant-previous-version.json"), Defendant.class);

        DefendantUpdate originalDefendantNextVersion = jsonObjectToObjectConverter.convert(
                getJson("/original-defendant-update-next-version.json"), DefendantUpdate.class);

        Defendant matchedDefendantPreviousVersion = jsonObjectToObjectConverter.convert(
                getJson("/matched-defendant-previous-version.json"), Defendant.class);

        DefendantUpdate matchedDefendantExceptedUpdate = jsonObjectToObjectConverter.convert(
                getJson("/matched-defendant-expected-version.json"), DefendantUpdate.class);

        DefendantUpdate matchedDefendantActualUpdate = defendantUpdateDifferenceService.calculateDefendantUpdate(
                originalDefendantPreviousVersion,
                originalDefendantNextVersion,
                matchedDefendantPreviousVersion);
        JSONAssert.assertEquals(objectToJsonObjectConverter.convert(matchedDefendantActualUpdate).toString(),
                objectToJsonObjectConverter.convert(matchedDefendantExceptedUpdate).toString(), JSONCompareMode.LENIENT);
    }

    private JsonObject getJson(String file) throws IOException {
        final String jsonString = Resources.toString(Resources.getResource("defendant-update/" + testCase + "/" + file), Charset.defaultCharset());

        try (JsonReader jsonReader = createReader(new StringReader(jsonString))) {
            return jsonReader.readObject();
        }
    }
}