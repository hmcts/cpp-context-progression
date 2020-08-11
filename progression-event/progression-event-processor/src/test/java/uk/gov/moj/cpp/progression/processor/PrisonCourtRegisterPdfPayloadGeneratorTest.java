package uk.gov.moj.cpp.progression.processor;

import static org.junit.Assert.assertThat;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;

import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

import javax.inject.Inject;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import org.hamcrest.core.Is;
import org.junit.Test;

public class PrisonCourtRegisterPdfPayloadGeneratorTest {

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Test
    public void shouldMapPayload() {
        final JsonObject body = getPayload("progression.add-prison-court-register-payload.json");
        final PrisonCourtRegisterPdfPayloadGenerator prisonCourtRegisterPdfPayloadGenerator = new PrisonCourtRegisterPdfPayloadGenerator();
        final JsonObject responseBody = prisonCourtRegisterPdfPayloadGenerator.mapPayload(body);
        assertThat(responseBody.toString(), Is.is(getPayload("prisonCourtRegisterPdfPayload.json")
                .toString().replace("%CURRENT_DATE%", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .replace("%AGE%", String.valueOf(Period.between(LocalDate.parse("2008-08-08"), LocalDate.now()).getYears()))
        ));
    }

    @Test
    public void shouldMapMinimumPayload() {
        final JsonObject body = getPayload("progression.add-prison-court-register-payload-min.json");
        final PrisonCourtRegisterPdfPayloadGenerator prisonCourtRegisterPdfPayloadGenerator = new PrisonCourtRegisterPdfPayloadGenerator();
        final JsonObject responseBody = prisonCourtRegisterPdfPayloadGenerator.mapPayload(body);
        assertThat(responseBody.toString(), Is.is(getPayload("prisonCourtRegisterPdfPayload-min.json")
                .toString().replace("%CURRENT_DATE%", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        ));
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