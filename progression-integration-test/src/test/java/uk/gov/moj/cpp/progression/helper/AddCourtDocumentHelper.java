package uk.gov.moj.cpp.progression.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentFor;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.google.common.io.Resources;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;

public class AddCourtDocumentHelper {

    private static final DateTimeFormatter ZONE_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");


    public static String addCourtDocumentDefendantLevel(final String payloadPath, final String docId, final String defendantId1, final String defendantId2, final String caseId) throws IOException {
        //Given
        final String body = prepareAddCourtDocumentPayload(docId, caseId, defendantId1, defendantId2, payloadPath);
        //When
        final Response writeResponse = postCommand(getWriteUrl("/courtdocument/" + docId),
                "application/vnd.progression.add-court-document+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
        //Then
        return getCourtDocumentFor(docId, allOf(
                withJsonPath("$.courtDocument.courtDocumentId", equalTo(docId)),
                withJsonPath("$.courtDocument.containsFinancialMeans", equalTo(true)),
                withJsonPath("$.courtDocument.sendToCps", equalTo(true)))
        );
    }


    public static String addCourtDocumentCaseLevel(final String resourceAddCourtDocument, final String caseId, final String docId) throws IOException {

        String body = prepareAddCourtDocumentPayload(docId, caseId, null, null, resourceAddCourtDocument);

        final Response writeResponse = postCommand(getWriteUrl("/courtdocument/" + docId),
                "application/vnd.progression.add-court-document+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        return getCourtDocumentFor(docId, allOf(
                withJsonPath("$.courtDocument.courtDocumentId", equalTo(docId)),
                withJsonPath("$.courtDocument.containsFinancialMeans", equalTo(true))
        ));
    }

    public static String addCourtDocumentHearingLevel(final String resourceAddCourtDocument, final String caseId, final String defendantId, final String hearingId, final String docId) throws IOException {
        final String body = prepareAddCourtDocumentPayload(docId, caseId, defendantId, null, hearingId, resourceAddCourtDocument);

        final Response writeResponse = postCommand(getWriteUrl("/courtdocument/" + docId),
                "application/vnd.progression.add-court-document+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        return getCourtDocumentFor(docId, allOf(
                withJsonPath("$.courtDocument.courtDocumentId", equalTo(docId)),
                withJsonPath("$.courtDocument.containsFinancialMeans", equalTo(true))
        ));
    }

    public static String prepareAddCourtDocumentPayload(final String docId, final String caseId, final String defendantId1, final String defendantId2, final String addCourtDocumentResource) throws IOException {
        String body = Resources.toString(Resources.getResource(addCourtDocumentResource),
                Charset.defaultCharset());
        body = body.replaceAll("%RANDOM_DOCUMENT_ID%", docId)
                .replaceAll("%RANDOM_CASE_ID%", caseId)
                .replaceAll("%RANDOM_DEFENDANT_ID1%", defendantId1)
                .replaceAll("%RANDOM_DEFENDANT_ID2%", defendantId1)
                .replaceAll("%UPLOADDATETIME%", ZONE_DATETIME_FORMATTER.format(ZonedDateTime.now()));
        return body;
    }

    public static String prepareAddCourtDocumentPayload(final String docId, final String caseId, final String defendantId1, final String defendantId2, final String hearingId, final String addCourtDocumentResource) throws IOException {
        final String body = prepareAddCourtDocumentPayload(docId, caseId, defendantId1, defendantId2, addCourtDocumentResource);
        return  body.replaceAll("%RANDOM_HEARING_ID%", hearingId);

    }

}
