package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getProsecutioncasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getCommandUri;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryDocumentTypeData;
import static uk.gov.moj.cpp.progression.test.matchers.BeanMatcher.isBean;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.assertProsecutionCase;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.courts.progression.query.ApplicationDocument;
import uk.gov.justice.courts.progression.query.Courtdocuments;
import uk.gov.justice.courts.progression.query.DocumentCategory;
import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.moj.cpp.progression.helper.MultipartFileUploadHelper;
import uk.gov.moj.cpp.progression.test.matchers.BeanMatcher;
import uk.gov.moj.cpp.progression.util.QueryUtil;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.io.Resources;
import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

// Temporarily ignoring to unblock hearing pipeline
@Ignore
public class UploadCourtDocumentIT extends AbstractIT {

    private static final String PROGRESSION_QUERY_COURTDOCUMENTSSEARCHAPPLICATION = "progression.query.courtdocumentsbyapplication";

    private MultipartFileUploadHelper helper;

    private String caseId;
    private String docId;
    private String defendantId;

    @Before
    public void setup() {
        super.setUp();
        helper = new MultipartFileUploadHelper();
        caseId = UUID.randomUUID().toString();
        docId = UUID.randomUUID().toString();
        defendantId = UUID.randomUUID().toString();
        createMockEndpoints();
        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");
    }

    @Test
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

        assertNotNull(material.getString("uploadDateTime"));
        final ZonedDateTime zonedDateTime = ZonedDateTime.parse(material.getString("uploadDateTime"));
        assertThat(zonedDateTime.getZone().getId(), equalTo(("Z")));
    }

    static class UploadRequest {
        private CourtDocument courtDocument;

        public CourtDocument getCourtDocument() {
            return courtDocument;
        }

        public void setCourtDocument(CourtDocument courtDocument) {
            this.courtDocument = courtDocument;
        }
    }

    @Test
    public void uploadApplicationDocument() throws Exception {
        final UUID applicationId = UUID.randomUUID();
        final UUID materialId = UUID.randomUUID();
        final CourtDocument courtDocument = CourtDocument.courtDocument()
                .withCourtDocumentId(UUID.randomUUID())
                .withDocumentTypeId(UUID.randomUUID())
                .withDocumentTypeDescription("test document")
                .withName("test")
                .withMimeType("mimeType")
                .withMaterials(asList(
                        Material.material()
                                .withId(materialId)
                                .withName("immaterial")
                                .withUserGroups(asList("Court Admin"))
                                .build()
                ))
                .withDocumentCategory(DocumentCategory.documentCategory()
                        .withApplicationDocument(ApplicationDocument.applicationDocument()
                                .withApplicationId(applicationId)
                                .build())
                        .build()).build();
        final UploadRequest uploadRequest = new UploadRequest();
        uploadRequest.setCourtDocument(courtDocument);
        final String strJson = Utilities.JsonUtil.toJsonString(uploadRequest);
        final UUID docId = UUID.randomUUID();
        final Response writeResponse = postCommand(getCommandUri("/courtdocument/" + docId.toString()),
                "application/vnd.progression.add-court-document+json",
                strJson);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        //search for the document by application id

        final BeanMatcher<Courtdocuments> preGeneratedResultMatcher = isBean(Courtdocuments.class)
                .withValue(cds -> cds.getDocumentIndices().size(), 1);

        System.out.println("applicationId " + applicationId.toString());

        //Thread.sleep(20000);


        final RequestParams preGeneratedRequestParams = requestParams(getURL(PROGRESSION_QUERY_COURTDOCUMENTSSEARCHAPPLICATION, applicationId.toString()),
                APPLICATION_VND_PROGRESSION_QUERY_SEARCH_COURTDOCUMENTS_JSON)
                .withHeader(CPP_UID_HEADER.getName(), USER_ID_VALUE)
                .build();

        QueryUtil.waitForQueryMatch(preGeneratedRequestParams, 45, preGeneratedResultMatcher, Courtdocuments.class);


    }

}
