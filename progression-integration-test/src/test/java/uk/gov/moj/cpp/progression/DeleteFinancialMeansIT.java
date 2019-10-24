package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getProsecutioncasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getCommandUri;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryDocumentTypeData;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.assertProsecutionCase;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.io.Resources;
import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

public class DeleteFinancialMeansIT extends AbstractIT {

    private String caseId;
    private String docId;
    private String defendantId;


    @Before
    public void setup() {
        super.setUp();
        caseId = UUID.randomUUID().toString();
        docId = UUID.randomUUID().toString();
        defendantId = UUID.randomUUID().toString();
        createMockEndpoints();
        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");
    }

    @Test
    public void shouldDeleteDefendantFinancialMeansDocument() throws IOException {

        //Given
        setUpProsecutionCaseWithDefendantCourtDocument();
        //When
        deleteFinancialMeansData();
        //Then
        assertCourtDocumentRemoved();
    }

    private void deleteFinancialMeansData() throws IOException {
        final String commandUri = getCommandUri("/cases/" + caseId + "/defendants/" + defendantId + "/financial-means");
        final Response response = postCommand(commandUri,
                "application/vnd.progression.delete-financial-means+json",
                "{}");
        assertThat(response.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
    }

    public void setUpProsecutionCaseWithDefendantCourtDocument() throws IOException {

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        assertProsecutioncaseAdded();
        addCourtDocumentToProsecutionCase();
        assertCourtDocumentAdded();
    }

    private void assertProsecutioncaseAdded() {

        final String response = getProsecutioncasesProgressionFor(caseId);
        final JsonObject prosecutioncasesJsonObject = getJsonObject(response);
        assertProsecutionCase(prosecutioncasesJsonObject.getJsonObject("prosecutionCase"), caseId, defendantId);
    }

    private void assertCourtDocumentAdded() {
        final Matcher[] matcher = {
                withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath("$.courtDocuments[0].courtDocumentId", equalTo(docId))
        };

        final JsonObject courtDocument = getJsonObject(getProsecutioncasesProgressionFor(caseId, matcher)).getJsonArray("courtDocuments").getJsonObject(0);
        assertThat(courtDocument.getString("courtDocumentId"), equalTo(docId));
        assertThat(courtDocument.getJsonObject("documentCategory").getJsonObject("defendantDocument").getString("prosecutionCaseId"), equalTo(caseId));

        assertThat(courtDocument.getString("name"), equalTo("SJP Notice"));
        assertThat(courtDocument.getString("documentTypeId"), equalTo("0bb7b276-9dc0-4af2-83b9-f4acef0c7898"));
        assertThat(courtDocument.getString("mimeType"), equalTo("pdf"));
        assertThat(courtDocument.getBoolean("containsFinancialMeans"), equalTo(true));
        final JsonObject material = courtDocument.getJsonArray("materials").getJsonObject(0);
        assertThat(material.getString("id"), equalTo("5e1cc18c-76dc-47dd-99c1-d6f87385edf1"));
    }

    private void assertCourtDocumentRemoved() {

        final Matcher[] matcher = {
                withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withoutJsonPath("$.courtDocuments[0].courtDocumentId")
        };

        final JsonObject jsonObject = getJsonObject(getProsecutioncasesProgressionFor(caseId, matcher));
        assertThat(jsonObject.getJsonArray("courtDocuments").size(), equalTo(0));
    }

    private void addCourtDocumentToProsecutionCase() throws IOException {
        String body = Resources.toString(Resources.getResource("progression.add-court-document.json"), Charset.defaultCharset());
        body = body.replaceAll("%RANDOM_DOCUMENT_ID%", docId.toString())
                .replaceAll("%RANDOM_CASE_ID%", caseId.toString())
                .replaceAll("%RANDOM_DEFENDANT_ID%", defendantId.toString());
        final Response writeResponse = postCommand(getCommandUri("/courtdocument/" + docId.toString()),
                "application/vnd.progression.add-court-document+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
    }
}
