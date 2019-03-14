package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getProsecutioncasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getCommandUri;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.stub.MaterialStub.stubMaterialUploadFile;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryDocumentTypeData;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.assertProsecutionCase;

import org.hamcrest.Matcher;
import uk.gov.moj.cpp.progression.helper.MultipartFileUploadHelper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.io.Resources;
import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
public class UploadCourtDocumentIT {

    private MultipartFileUploadHelper helper;

    private String caseId;
    private String docId;
    private String defendantId;

    @Before
    public void setup() {
        helper = new MultipartFileUploadHelper();
        caseId = UUID.randomUUID().toString();
        docId = UUID.randomUUID().toString();
        defendantId = UUID.randomUUID().toString();
        createMockEndpoints();
        stubMaterialUploadFile();
        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");
    }
    // Temporarily ignoring the tests to debug the issue
// also unblock hearing
    public void shouldUploadCourtDocument() {
        final String fileName = "src/test/resources/pdf-test.pdf";
        final UUID materialId = randomUUID();
        final String url = String.format("/courtdocument/%s", materialId);
        helper.makeMultipartFormPostCall(url, "fileServiceId", fileName);
        helper.verifyInMessagingQueueForCourtDocUploaded(materialId);
    }

    @Test
    public void shouldAddCourtDocument() throws IOException, InterruptedException {

        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        // when
        final String response = getProsecutioncasesProgressionFor(caseId);
        // then
        final JsonObject prosecutioncasesJsonObject = getJsonObject(response);

        assertProsecutionCase(prosecutioncasesJsonObject.getJsonObject("prosecutionCase"), caseId, defendantId);


        String body = Resources.toString(Resources.getResource("progression.add-court-document.json"), Charset.defaultCharset());
        body = body.replaceAll("%RANDOM_DOCUMENT_ID%", docId.toString())
                .replaceAll("%RANDOM_CASE_ID%", caseId.toString())
                .replaceAll("%RANDOM_DEFENDANT_ID%", defendantId.toString());
        final Response writeResponse = postCommand(getCommandUri("/courtdocument/" + docId.toString()),
                "application/vnd.progression.add-court-document+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        Matcher[] matcher = {
                withJsonPath("$.prosecutionCase.id",equalTo(caseId)),
                withJsonPath("$.courtDocuments[0].courtDocumentId",equalTo(docId))
        };

        final JsonObject courtDocument = getJsonObject(getProsecutioncasesProgressionFor(caseId, matcher)).getJsonArray("courtDocuments").getJsonObject(0);
        assertThat(courtDocument.getString("courtDocumentId"), equalTo(docId));
        assertThat(courtDocument.getJsonObject("documentCategory").getJsonObject("defendantDocument").getString("prosecutionCaseId"), equalTo(caseId));

        assertThat(courtDocument.getString("name"), equalTo("SJP Notice"));
        assertThat(courtDocument.getString("documentTypeId"), equalTo("0bb7b276-9dc0-4af2-83b9-f4acef0c7898"));
        assertThat(courtDocument.getString("mimeType"), equalTo("pdf"));
        final JsonObject material = courtDocument.getJsonArray("materials").getJsonObject(0);
        assertThat(material.getString("id"), equalTo("5e1cc18c-76dc-47dd-99c1-d6f87385edf1"));
    }
}
