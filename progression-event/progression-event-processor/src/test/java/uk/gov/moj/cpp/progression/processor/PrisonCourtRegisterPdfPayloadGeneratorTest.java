package uk.gov.moj.cpp.progression.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.AllOf.allOf;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;

import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

import javax.inject.Inject;
import javax.json.JsonObject;

import com.google.common.io.Resources;

import org.junit.jupiter.api.Test;

public class PrisonCourtRegisterPdfPayloadGeneratorTest {

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Test
    public void shouldMapPayloadFilterApplicationWithoutResults() {
        final JsonObject body = getPayload("progression.add-prison-court-register-payload.json");
        final PrisonCourtRegisterPdfPayloadGenerator prisonCourtRegisterPdfPayloadGenerator = new PrisonCourtRegisterPdfPayloadGenerator();
        final JsonObject responseBody = prisonCourtRegisterPdfPayloadGenerator.mapPayload(body);
        assertThat(responseBody.toString(), is(getPayload("prisonCourtRegisterWithoutApplicationPdfPayload.json")
                .toString().replaceAll("%AGE%", String.valueOf(Period.between(LocalDate.of(2008,8,8),LocalDate.now()).getYears()))
                .replace("%CURRENT_DATE%", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        ));
    }

    @Test
    public void shouldMapPayloadFilterOffenceWithoutResults() {
        final JsonObject body = getPayload("progression.add-prison-court-register-payload.json");
        final PrisonCourtRegisterPdfPayloadGenerator prisonCourtRegisterPdfPayloadGenerator = new PrisonCourtRegisterPdfPayloadGenerator();
        final JsonObject responseBody = prisonCourtRegisterPdfPayloadGenerator.mapPayload(body);
        assertThat(responseBody.toString(), is(getPayload("prisonCourtRegisterWithoutOffencePdfPayload.json")
                .toString().replaceAll("%AGE%", String.valueOf(Period.between(LocalDate.of(2008,8,8),LocalDate.now()).getYears()))
                .replace("%CURRENT_DATE%", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        ));
    }
    @Test
    public void shouldCorrectlyMapPayloadWithApplicationResults() {
        final JsonObject body = getPayload("progression.add-prison-court-register-payload-with-application.json");
        final PrisonCourtRegisterPdfPayloadGenerator prisonCourtRegisterPdfPayloadGenerator = new PrisonCourtRegisterPdfPayloadGenerator();
        final JsonObject responseBody = prisonCourtRegisterPdfPayloadGenerator.mapPayload(body);
        assertThat(responseBody.toString(), is(getPayload("prisonCourtRegisterPdfPayloadWithApplication.json")
                .toString().replaceAll("%AGE%", String.valueOf(Period.between(LocalDate.of(2008,8,8),LocalDate.now()).getYears()))
                .replace("%CURRENT_DATE%", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        ));
    }

    @Test
    public void shouldCorrectlyMapPayloadWithApplicationTwoResults() {
        final JsonObject body = getPayload("progression.add-prison-court-register-payload-with-application-2-result.json");
        final PrisonCourtRegisterPdfPayloadGenerator prisonCourtRegisterPdfPayloadGenerator = new PrisonCourtRegisterPdfPayloadGenerator();
        final JsonObject responseBody = prisonCourtRegisterPdfPayloadGenerator.mapPayload(body);
        assertThat(responseBody.toString(), is(getPayload("prisonCourtRegisterPdfPayloadWithApplication2Results.json")
                .toString().replaceAll("%AGE%", String.valueOf(Period.between(LocalDate.of(1964,12,3),LocalDate.now()).getYears()))
                .replace("%CURRENT_DATE%", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        ));
    }

    @Test
    public void shouldCorrectlyMapPayloadWithApplicationEmptyResults() {
        final JsonObject body = getPayload("progression.add-prison-court-register-payload-with-application-no-result.json");
        final PrisonCourtRegisterPdfPayloadGenerator prisonCourtRegisterPdfPayloadGenerator = new PrisonCourtRegisterPdfPayloadGenerator();
        final JsonObject responseBody = prisonCourtRegisterPdfPayloadGenerator.mapPayload(body);
        assertThat(responseBody.toString(), is(getPayload("prisonCourtRegisterPdfPayloadWithApplicationNoResult.json")
                .toString().replaceAll("%AGE%", String.valueOf(Period.between(LocalDate.of(2008,8,8),LocalDate.now()).getYears()))
                .replace("%CURRENT_DATE%", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        ));
    }

    @Test
    public void shouldMapMinimumPayload() {
        final JsonObject body = getPayload("progression.add-prison-court-register-payload-min.json");
        final PrisonCourtRegisterPdfPayloadGenerator prisonCourtRegisterPdfPayloadGenerator = new PrisonCourtRegisterPdfPayloadGenerator();
        final JsonObject responseBody = prisonCourtRegisterPdfPayloadGenerator.mapPayload(body);
        assertThat(responseBody.toString(), is(getPayload("prisonCourtRegisterPdfPayload-min.json")
                .toString().replace("%CURRENT_DATE%", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        ));
    }

    @Test
    public void shouldReplaceWhiteSpace(){
        final JsonObject body = getPayload("progression.add-prison-court-register-payload-with-whitespaces.json");
        final PrisonCourtRegisterPdfPayloadGenerator prisonCourtRegisterPdfPayloadGenerator = new PrisonCourtRegisterPdfPayloadGenerator();
        final JsonObject responseBody = prisonCourtRegisterPdfPayloadGenerator.mapPayload(body);

        assertThat(responseBody.toString(), isJson(allOf(
                withJsonPath("$.cases[0].offences[0].results[0].resultText", is("IMP - description\nAbsolute discharge\n O10 17"))
        )));
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
