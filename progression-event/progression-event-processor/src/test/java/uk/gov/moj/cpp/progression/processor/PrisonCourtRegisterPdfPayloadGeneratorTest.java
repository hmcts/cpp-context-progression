package uk.gov.moj.cpp.progression.processor;

import com.google.common.io.Resources;
import org.hamcrest.core.Is;
import org.junit.Test;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;

import javax.inject.Inject;
import javax.json.JsonObject;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

import static org.junit.Assert.assertThat;

public class PrisonCourtRegisterPdfPayloadGeneratorTest {

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Test
    public void shouldMapPayloadFilterApplicationWithoutResults() {
        final JsonObject body = getPayload("progression.add-prison-court-register-payload.json");
        final PrisonCourtRegisterPdfPayloadGenerator prisonCourtRegisterPdfPayloadGenerator = new PrisonCourtRegisterPdfPayloadGenerator();
        final JsonObject responseBody = prisonCourtRegisterPdfPayloadGenerator.mapPayload(body);
        assertThat(responseBody.toString(), Is.is(getPayload("prisonCourtRegisterWithoutApplicationPdfPayload.json")
                .toString().replaceAll("%AGE%", String.valueOf(Period.between(LocalDate.of(2008,8,8),LocalDate.now()).getYears()))
                .replace("%CURRENT_DATE%", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        ));
    }

    @Test
    public void shouldMapPayloadFilterOffenceWithoutResults() {
        final JsonObject body = getPayload("progression.add-prison-court-register-payload.json");
        final PrisonCourtRegisterPdfPayloadGenerator prisonCourtRegisterPdfPayloadGenerator = new PrisonCourtRegisterPdfPayloadGenerator();
        final JsonObject responseBody = prisonCourtRegisterPdfPayloadGenerator.mapPayload(body);
        assertThat(responseBody.toString(), Is.is(getPayload("prisonCourtRegisterWithoutOffencePdfPayload.json")
                .toString().replaceAll("%AGE%", String.valueOf(Period.between(LocalDate.of(2008,8,8),LocalDate.now()).getYears()))
                .replace("%CURRENT_DATE%", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        ));
    }
    @Test
    public void shouldCorrectlyMapPayloadWithApplicationResults() {
        final JsonObject body = getPayload("progression.add-prison-court-register-payload-with-application.json");
        final PrisonCourtRegisterPdfPayloadGenerator prisonCourtRegisterPdfPayloadGenerator = new PrisonCourtRegisterPdfPayloadGenerator();
        final JsonObject responseBody = prisonCourtRegisterPdfPayloadGenerator.mapPayload(body);
        assertThat(responseBody.toString(), Is.is(getPayload("prisonCourtRegisterPdfPayloadWithApplication.json")
                .toString().replaceAll("%AGE%", String.valueOf(Period.between(LocalDate.of(2008,8,8),LocalDate.now()).getYears()))
                .replace("%CURRENT_DATE%", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        ));
    }

    @Test
    public void shouldCorrectlyMapPayloadWithApplicationTwoResults() {
        final JsonObject body = getPayload("progression.add-prison-court-register-payload-with-application-2-result.json");
        final PrisonCourtRegisterPdfPayloadGenerator prisonCourtRegisterPdfPayloadGenerator = new PrisonCourtRegisterPdfPayloadGenerator();
        final JsonObject responseBody = prisonCourtRegisterPdfPayloadGenerator.mapPayload(body);
        assertThat(responseBody.toString(), Is.is(getPayload("prisonCourtRegisterPdfPayloadWithApplication2Results.json")
                .toString().replaceAll("%AGE%", String.valueOf(Period.between(LocalDate.of(1964,12,3),LocalDate.now()).getYears()))
                .replace("%CURRENT_DATE%", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        ));
    }

    @Test
    public void shouldCorrectlyMapPayloadWithApplicationEmptyResults() {
        final JsonObject body = getPayload("progression.add-prison-court-register-payload-with-application-no-result.json");
        final PrisonCourtRegisterPdfPayloadGenerator prisonCourtRegisterPdfPayloadGenerator = new PrisonCourtRegisterPdfPayloadGenerator();
        final JsonObject responseBody = prisonCourtRegisterPdfPayloadGenerator.mapPayload(body);
        assertThat(responseBody.toString(), Is.is(getPayload("prisonCourtRegisterPdfPayloadWithApplicationNoResult.json")
                .toString().replaceAll("%AGE%", String.valueOf(Period.between(LocalDate.of(2008,8,8),LocalDate.now()).getYears()))
                .replace("%CURRENT_DATE%", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
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
