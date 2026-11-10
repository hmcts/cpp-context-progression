package uk.gov.moj.cpp.progression.transformer;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.moj.cpp.progression.transformer.HearingHelper.transformedHearing;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.COURT_APPLICATIONS;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.DEFENDANTS;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.DEFENDANT_JUDICIAL_RESULTS;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.JUDICIAL_RESULTS;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.OFFENCES;
import static uk.gov.moj.cpp.progression.transformer.SchemaVariableConstants.PROSECUTION_CASES;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;

import java.nio.charset.Charset;

import javax.json.JsonObject;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HearingHelperTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingHelperTest.class.getName());

    @Test
    public void shouldTransformHearing() {
        final JsonObject hearingJson = getPayload();

        final JsonObject transformedObject = transformedHearing(hearingJson).getJsonObject("hearing");

        final JsonObject prosecutionCaseJson = transformedObject.getJsonArray(PROSECUTION_CASES).getValuesAs(JsonObject.class).get(0);
        final JsonObject defendantJson = prosecutionCaseJson.getJsonArray(DEFENDANTS).getValuesAs(JsonObject.class).get(0);
        final JsonObject offenceJson = defendantJson.getJsonArray(OFFENCES).getValuesAs(JsonObject.class).get(0);
        final JsonObject applicationJson = transformedObject.getJsonArray(COURT_APPLICATIONS).getValuesAs(JsonObject.class).get(0);

        assertThat(transformedObject.getJsonArray(DEFENDANT_JUDICIAL_RESULTS).size(), is(1));
        assertThat(defendantJson.getJsonArray(JUDICIAL_RESULTS).size(), is(2));
        assertThat(offenceJson.getJsonArray(JUDICIAL_RESULTS).size(), is(2));
        assertThat(applicationJson.getJsonArray(JUDICIAL_RESULTS).size(), is(2));
    }

    private static JsonObject getPayload() {
        String response = null;
        try {
            response = Resources.toString(
                    Resources.getResource("hearing.json"),
                    Charset.defaultCharset()
            );
        } catch (final Exception e) {
            LOGGER.info("error {}", e.getMessage());
        }

        return createObjectBuilder().add("hearing", new StringToJsonObjectConverter().convert(response)).build();
    }
}
