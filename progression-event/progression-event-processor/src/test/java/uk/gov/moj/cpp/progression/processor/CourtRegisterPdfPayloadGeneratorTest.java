package uk.gov.moj.cpp.progression.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;

import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.Period;

import javax.inject.Inject;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import org.hamcrest.Matchers;
import org.junit.Ignore;
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
    public void shouldMapPayloadForMultiCourtRegisterDocumentRequests() {
        final JsonObject jsonObject = getPayload("progression.event.court-register-generated.json");
        final CourtRegisterPdfPayloadGenerator courtRegisterPdfPayloadGenerator = new CourtRegisterPdfPayloadGenerator();
        final JsonObject responseBody = courtRegisterPdfPayloadGenerator.mapPayload(jsonObject);
        assertThat(responseBody.toString(), is(getPayload("courtRegisterPdfPayload-multiCourtRegisterDocumentRequests.json").toString()
                .replaceAll("AGE",String.valueOf(Period.between(LocalDate.of(2007,8,22), LocalDate.now()).getYears()))));
    }

    @Test
    public void shouldMapMinimumPayload() {
        final JsonObject body = getPayload("progression.add-court-register-document-min-payload.json");
        final CourtRegisterPdfPayloadGenerator courtRegisterPdfPayloadGenerator = new CourtRegisterPdfPayloadGenerator();
        final JsonObject responseBody = courtRegisterPdfPayloadGenerator.mapPayload(body);
        assertThat(responseBody.toString(), is(getPayload("courtRegisterPdfPayload-min.json").toString()));
    }

    @Test
    public void shouldReplaceWhiteSpace() {
        final JsonObject body = getPayload("progression.add-court-register-document-payload-with-whitespaces.json");
        final CourtRegisterPdfPayloadGenerator courtRegisterPdfPayloadGenerator = new CourtRegisterPdfPayloadGenerator();
        final JsonObject responseBody = courtRegisterPdfPayloadGenerator.mapPayload(body);
        assertThat(responseBody.toString(), isJson(allOf(
                withJsonPath("$.cases[0].defendantResults[0].resultText", Matchers.is("IMP - label\nsome result\n with whitespaces"))
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