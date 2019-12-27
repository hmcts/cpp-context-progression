package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryDocumentTypeData;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import java.io.IOException;

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
        caseId = randomUUID().toString();
        docId = randomUUID().toString();
        defendantId = randomUUID().toString();
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
        final String commandUri = getWriteUrl("/cases/" + caseId + "/defendants/" + defendantId + "/financial-means");
        final Response response = postCommand(commandUri,
                "application/vnd.progression.delete-financial-means+json",
                "{}");
        assertThat(response.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
    }

    private void setUpProsecutionCaseWithDefendantCourtDocument() throws IOException {

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        addCourtDocumentToProsecutionCase();
        assertCourtDocumentAdded();
    }


    private void assertCourtDocumentAdded() {
        final Matcher[] matchers = {
                withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath("$.courtDocuments[0].courtDocumentId", is(docId)),
                withJsonPath("$.courtDocuments[0].documentCategory.defendantDocument.prosecutionCaseId", is(caseId)),
                withJsonPath("$.courtDocuments[0].name", is("SJP Notice")),
                withJsonPath("$.courtDocuments[0].documentTypeId", is("0bb7b276-9dc0-4af2-83b9-f4acef0c7898")),
                withJsonPath("$.courtDocuments[0].mimeType", is("pdf")),
                withJsonPath("$.courtDocuments[0].containsFinancialMeans", is(true)),
                withJsonPath("$.courtDocuments[0].materials[0].id", is("5e1cc18c-76dc-47dd-99c1-d6f87385edf1")),
        };

        pollProsecutionCasesProgressionFor(caseId, matchers);
    }

    private void assertCourtDocumentRemoved() {
        final Matcher[] matchers = {
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.courtDocuments", empty())
        };
        pollProsecutionCasesProgressionFor(caseId, matchers);
    }

    private void addCourtDocumentToProsecutionCase() throws IOException {
        String body = getPayload("progression.add-court-document.json");
        body = body.replaceAll("%RANDOM_DOCUMENT_ID%", docId.toString())
                .replaceAll("%RANDOM_CASE_ID%", caseId.toString())
                .replaceAll("%RANDOM_DEFENDANT_ID%", defendantId.toString());
        final Response writeResponse = postCommand(getWriteUrl("/courtdocument/" + docId),
                "application/vnd.progression.add-court-document+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
    }
}
