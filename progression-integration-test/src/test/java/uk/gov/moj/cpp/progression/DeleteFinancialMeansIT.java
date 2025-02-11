package uk.gov.moj.cpp.progression;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentsByCase;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentsByCaseWithMatchers;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubGetDocumentsTypeAccess;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryDocumentTypeData;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import java.io.IOException;
import java.util.UUID;

import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

public class DeleteFinancialMeansIT extends AbstractIT {

    private String caseId;
    private String docId;
    private String defendantId;


    @BeforeEach
    public void setup() {
        caseId = randomUUID().toString();
        docId = randomUUID().toString();
        defendantId = randomUUID().toString();
        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");
        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");

    }

    @Test
    public void shouldDeleteDefendantFinancialMeansDocument() throws IOException, JSONException {

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

    private void setUpProsecutionCaseWithDefendantCourtDocument() throws IOException, JSONException {

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        addCourtDocumentToProsecutionCase();
        assertCourtDocumentByCase();
    }


    private void assertCourtDocumentRemoved() {
        final String actualPayload = getCourtDocumentsByCase(UUID.randomUUID().toString(), caseId);

        final String expectedPayload = "{\"documentIndices\":[]}";

        assertThat(expectedPayload, equalTo(actualPayload));

    }

    private void addCourtDocumentToProsecutionCase() throws IOException {
        String body = getPayload("progression.add-court-document.json");
        body = body.replaceAll("%RANDOM_DOCUMENT_ID%", docId)
                .replaceAll("%RANDOM_CASE_ID%", caseId)
                .replaceAll("%RANDOM_DEFENDANT_ID1%", defendantId);
        final Response writeResponse = postCommand(getWriteUrl("/courtdocument/" + docId),
                "application/vnd.progression.add-court-document+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
    }

    private CustomComparator getCustomComparator() {
        return new CustomComparator(STRICT,
                new Customization("documentIndices[0].document.materials[0].uploadDateTime", (o1, o2) -> true),
                new Customization("documentIndices[1].document.materials[0].uploadDateTime", (o1, o2) -> true),
                new Customization("documentIndices[0].document.documentTypeRBAC", (o1, o2) -> true),
                new Customization("documentIndices[0].document.documentTypeRBAC", (o1, o2) -> true),
                new Customization("documentIndices[0].document.materials[0].id", (o1, o2) -> true),
                new Customization("documentIndices[0].document.materials[1].id", (o1, o2) -> true)
        );
    }

    private void assertCourtDocumentByCase() throws JSONException {

        final String courtDocumentsByCaseStatus = getCourtDocumentsByCaseWithMatchers(UUID.randomUUID().toString(), docId, caseId);
        final String expectedPayload = getPayload("expected/expected.progression.court-document-delete-financial-means.json")
                .replace("COURT-DOCUMENT-ID1", docId)
                .replace("CASE-ID", caseId)
                .replace("DEFENDENT-ID", defendantId);


        JSONAssert.assertEquals(expectedPayload, courtDocumentsByCaseStatus, getCustomComparator());
    }
}
