package uk.gov.moj.cpp.progression.helper;

import static java.util.Arrays.stream;
import static java.util.Objects.nonNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubGetCountryByPostCode;

import uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;


public class DocumentGenerationHelper {
    public static final String ENGLISH_TEMPLATE_NAME = "NPE_RefferalDisqualificationWarning";
    public static final String TEMPLATE_IDENTIFIER_EMPTY_PAGE = "EmptyPage";
    public static final String WELSE_TEMPLATE_NAME = "NPB_RefferalDisqualificationWarning";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy");
    private static final DateTimeFormatter WELSH_DATE_TIME_FORMATTER = DATE_TIME_FORMATTER.withLocale(new Locale("cy"));
    private static String defendantPostCode = "W1T 1JY";

    public static void validateEnglishReferalDisqualifyWarning(final JSONObject documentGenerationRequest, final Path testResourceBasePath, final String caseUrn, final Boolean hasWelshPostCode,  final Boolean hasPostCode) {
        String postcode;
        JSONObject expectedPayload;
        try {
            if (hasPostCode) {
                postcode = hasWelshPostCode ? "CF10 1BY" : defendantPostCode;
                expectedPayload = new JSONObject(FileHelper.read(testResourceBasePath.toString(),
                        caseUrn, postcode));
            } else {
                expectedPayload = new JSONObject(FileHelper.read(testResourceBasePath.toString(),
                        caseUrn));
            }
            validateDocumentGenerationRequest(documentGenerationRequest, ENGLISH_TEMPLATE_NAME, expectedPayload);
        }catch (JSONException e) {
            throw new RuntimeException(e);
        }

    }

    public static void validateWelshReferalDisqualifyWarning(final JSONObject documentGenerationRequest, final Path testResourceBasePath, final String caseUrn) throws JSONException{

        final JSONObject expectedPayload = new JSONObject(FileHelper.read(testResourceBasePath.toString(),
                caseUrn));
        validateDocumentGenerationRequest(documentGenerationRequest, WELSE_TEMPLATE_NAME, expectedPayload);
    }

    private static void validateDocumentGenerationRequest(final JSONObject documentGenerationRequest, final String templateName, final JSONObject expectedPayload) throws JSONException{
        assertThat(documentGenerationRequest.getString("conversionFormat"), is("pdf"));
        assertThat(documentGenerationRequest.getString("templateName"), is(templateName));
        assertEquals(expectedPayload, documentGenerationRequest.getJSONObject("templatePayload"), true);
    }

    public static void givenCaseIsReferredToMags(final String postCode, final String... templates) {
        if(nonNull(postCode)) {
            stubGetCountryByPostCode(postCode.replaceAll("\\s", ""));
            }
             stream(templates).forEach(DocumentGeneratorStub::stubDocumentGeneration);
    }
}

