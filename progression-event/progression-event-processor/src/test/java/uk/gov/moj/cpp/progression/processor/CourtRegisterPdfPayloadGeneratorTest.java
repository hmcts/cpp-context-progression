package uk.gov.moj.cpp.progression.processor;

import static org.junit.Assert.assertThat;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;

import java.nio.charset.Charset;

import javax.inject.Inject;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import org.hamcrest.core.Is;
import org.junit.Test;

public class CourtRegisterPdfPayloadGeneratorTest {

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Test
    public void shouldMapPayload() {
        final JsonObject body = getPayload("progression.add-court-register-document-payload.json");
        final CourtRegisterPdfPayloadGenerator courtRegisterPdfPayloadGenerator = new CourtRegisterPdfPayloadGenerator();
        final JsonObject responseBody = courtRegisterPdfPayloadGenerator.mapPayload(body);
        assertThat(responseBody.toString(), Is.is(getPayload("courtRegisterPdfPayload.json").toString()));
    }

    @Test
    public void shouldMapMinimumPayload() {
        final JsonObject body = getPayload("progression.add-court-register-document-min-payload.json");
        final CourtRegisterPdfPayloadGenerator courtRegisterPdfPayloadGenerator = new CourtRegisterPdfPayloadGenerator();
        final JsonObject responseBody = courtRegisterPdfPayloadGenerator.mapPayload(body);
        assertThat(responseBody.toString(), Is.is(getPayload("courtRegisterPdfPayload-min.json").toString()));
    }

    public static JsonObject getPayload(final String path) {
        String response = null;
        try {
            response = Resources.toString(
                    Resources.getResource(path),
                    Charset.defaultCharset()
            );
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return new StringToJsonObjectConverter().convert(response);
    }

}