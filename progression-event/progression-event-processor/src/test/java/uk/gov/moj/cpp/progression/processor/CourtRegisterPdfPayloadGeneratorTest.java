package uk.gov.moj.cpp.progression.processor;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;

import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.Period;

import javax.inject.Inject;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import org.junit.Test;

public class CourtRegisterPdfPayloadGeneratorTest {

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Test
    public void shouldMapPayload() {
        final JsonObject body = getPayload("progression.add-court-register-document-payload.json");
        final CourtRegisterPdfPayloadGenerator courtRegisterPdfPayloadGenerator = new CourtRegisterPdfPayloadGenerator();
        final JsonObject responseBody = courtRegisterPdfPayloadGenerator.mapPayload(body);
        final String expected = responseBody.toString().replaceAll("%AGE%", String.valueOf(Period.between(LocalDate.of(2001,12,27),LocalDate.now()).getYears()));
        assertThat(responseBody.toString(), is(expected));
    }

    @Test
    public void shouldMapMinimumPayload() {
        final JsonObject body = getPayload("progression.add-court-register-document-min-payload.json");
        final CourtRegisterPdfPayloadGenerator courtRegisterPdfPayloadGenerator = new CourtRegisterPdfPayloadGenerator();
        final JsonObject responseBody = courtRegisterPdfPayloadGenerator.mapPayload(body);
        assertThat(responseBody.toString(), is(getPayload("courtRegisterPdfPayload-min.json").toString()));
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