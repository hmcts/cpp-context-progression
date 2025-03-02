package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addStandaloneCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getApplicationExtractPdf;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.DOCUMENT_TEXT;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryDocumentTypeAccessQueryData;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryDocumentTypeData;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.moj.cpp.progression.helper.CourtApplicationsHelper;

import java.io.IOException;

import javax.jms.JMSException;

import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

public class ApplicationExtractIT extends AbstractIT {
    private String hearingId;
    private String courtApplicationId;
    private String docId;
    private String defendantId;
    private String updatedDefendantId;

    @BeforeEach
    public void setUp() {
        hearingId = randomUUID().toString();
        courtApplicationId = randomUUID().toString();
        docId = randomUUID().toString();
        defendantId = randomUUID().toString();
        updatedDefendantId = randomUUID().toString();
        stubQueryDocumentTypeAccessQueryData("/restResource/ref-data-document-type-for-standalone.json");
    }

    @AfterAll
    public static void tearDown() throws JMSException {
        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");
    }

    @Test
    public void shouldAddDocumentInStandAloneApplicationAndThenUpdateIt() throws Exception {
        // given
        addStandaloneCourtApplication(courtApplicationId, randomUUID().toString(), new CourtApplicationsHelper.CourtApplicationRandomValues(), "progression.command.create-standalone-court-application.json");
        pollForApplication(courtApplicationId);
        // when
        final String documentContentResponse = getApplicationExtractPdf(courtApplicationId, hearingId);
        // then
        assertThat(documentContentResponse, equalTo(DOCUMENT_TEXT));
        verifyAddCourtDocument();
        //Given
        final String bodyForUpdate = prepareUpdateCourtDocumentPayload();
        final Response writeResponseForUpdate = postCommand(getWriteUrl("/courtdocument"),
                "application/vnd.progression.update-court-document+json",
                bodyForUpdate);

        assertThat(writeResponseForUpdate.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        final String actualDocumentAfterUpdate = getCourtDocumentFor(docId, allOf(
                withJsonPath("$.courtDocument.courtDocumentId", equalTo(docId)),
                withJsonPath("$.courtDocument.containsFinancialMeans", equalTo(false)),
                withJsonPath("$.courtDocument.documentTypeDescription", equalTo("Applications"))
        ));
        final String expectedPayloadAfterUpdate = getPayload("expected/expected.progression.court-document-updated-for-standalone.json")
                .replace("COURT-DOCUMENT-ID", docId)
                .replace("%RANDOM_APPLICATION_ID%", courtApplicationId);

        assertEquals(expectedPayloadAfterUpdate, actualDocumentAfterUpdate, getCustomComparator());
    }

    private void verifyAddCourtDocument() throws JSONException {
        //Given
        final String body = prepareAddCourtDocumentPayload();
        //When
        final Response writeResponse = postCommand(getWriteUrl("/courtdocument/" + docId),
                "application/vnd.progression.add-court-document+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        //Then
        final String actualDocument = getCourtDocumentFor(docId, allOf(
                withJsonPath("$.courtDocument.courtDocumentId", equalTo(docId)),
                withJsonPath("$.courtDocument.containsFinancialMeans", equalTo(false)))
        );

        final String expectedPayload = getPayload("expected/expected.progression.add-court-document-for-standalone.json")
                .replace("%RANDOM_DOCUMENT_ID%", docId)
                .replace("%RANDOM_APPLICATION_ID%", courtApplicationId);

        assertEquals(expectedPayload, actualDocument, getCustomComparator());
    }

    private String prepareAddCourtDocumentPayload() {
        String body = getPayload("progression.add-court-document-for-standalone.json");
        body = body.replaceAll("%RANDOM_DOCUMENT_ID%", docId)
                .replaceAll("%RANDOM_APPLICATION_ID%", courtApplicationId)
                .replaceAll("%RANDOM_DEFENDANT_ID%", defendantId);
        return body;
    }

    private CustomComparator getCustomComparator() {
        return new CustomComparator(STRICT,
                new Customization("courtDocument.materials[0].uploadDateTime", (o1, o2) -> true)
        );
    }

    private String prepareUpdateCourtDocumentPayload() {

        String body = getPayload("progression.update-court-document-for-standalone.json");
        body = body.replaceAll("%RANDOM_DOCUMENT_ID%", docId)
                .replaceAll("%RANDOM_APPLICATION_ID%", courtApplicationId)
                .replaceAll("%DEFENDENT-ID2%", updatedDefendantId);

        return body;
    }

}
