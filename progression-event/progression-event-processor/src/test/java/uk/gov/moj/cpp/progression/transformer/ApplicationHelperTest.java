package uk.gov.moj.cpp.progression.transformer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;

import java.nio.charset.Charset;

import javax.json.JsonArray;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationHelperTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationHelperTest.class);

    @Test
    public void shouldTransformApplication(){
        final JsonArray applicationsJson = getPayload("hearing.json");

        final JsonArray externalApplications = ApplicationHelper.transformApplications(applicationsJson);

        assertThat(externalApplications, is(getPayload("expected-external-applications.json")));

    }

    private JsonArray getPayload(final String fileName) {
        String response = null;
        try {
            response = Resources.toString(
                    Resources.getResource(fileName),
                    Charset.defaultCharset()
            );
        } catch (final Exception e) {
            LOGGER.info("error {}", e.getMessage());
        }

        return new StringToJsonObjectConverter().convert(response).getJsonArray("courtApplications");
    }

}
