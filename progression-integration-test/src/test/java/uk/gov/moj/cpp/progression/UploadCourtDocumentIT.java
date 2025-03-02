package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.justice.core.courts.CourtDocument.courtDocument;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentsByApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentsByCaseWithMatchers;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubGetDocumentsTypeAccess;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryCpsProsecutorData;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryDocumentTypeData;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.courts.progression.query.ApplicationDocument;
import uk.gov.justice.courts.progression.query.DocumentCategory;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.progression.helper.MultipartFileUploadHelper;
import uk.gov.moj.cpp.progression.stub.ReferenceDataStub;
import uk.gov.moj.cpp.progression.util.Utilities;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.io.Resources;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

public class UploadCourtDocumentIT extends AbstractIT {

    private MultipartFileUploadHelper helper;

    private String caseId;
    private String docId;
    private String defendantId;
    private String cpsDefendantId;

    private final JmsMessageConsumerClient publicEventConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.court-document-added").getMessageConsumerClient();

    @BeforeEach
    public void setup() {
        helper = new MultipartFileUploadHelper();
        caseId = randomUUID().toString();
        docId = randomUUID().toString();
        defendantId = randomUUID().toString();
        cpsDefendantId = randomUUID().toString();
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
    public void shouldUploadCourtDocumentForDefence() {
        final String fileName = "src/test/resources/pdf-test.pdf";
        final UUID materialId = randomUUID();
        final UUID defendantId = randomUUID();
        final String url = String.format("/defendant/%s/courtdocument/%s", defendantId, materialId);
        helper.stubForDefence(defendantId);
        helper.makeMultipartFormPostCall(url, "fileServiceId", fileName);
        helper.verifyInMessagingQueueForCourtDocUploaded(materialId);
        helper.resetUserRoles();
    }

    @Test
    public void shouldAddCourtDocumentAndQueryWithPagination() throws IOException, JSONException {

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");

        addCourtDocument(docId, caseId, defendantId);

        final Matcher[] matcher = {
                withJsonPath("$.courtDocuments[0].name", CoreMatchers.is("SJP Notice"))
        };

        final String courtDocumentsByCaseStatus = pollForResponse("/courtdocumentsearch?caseId=" + caseId,
                "application/vnd.progression.query.courtdocuments.with.pagination+json",
                randomUUID().toString(),
                matcher);
        final String expectedPayload = getPayload("expected/expected.progression.upload.court-document-with-pagination.json")
                .replace("COURT-DOCUMENT-ID1", docId)
                .replace("CASE-ID", caseId)
                .replace("DEFENDENT-ID", defendantId);


        assertEquals(expectedPayload, courtDocumentsByCaseStatus, getCustomComparatorForPaging());
        verifyInMessagingQueueForPublicCourtDocumentAdded();
    }

    @Test
    public void shouldUpdateCaseCpsDefendantIdOnDocumentUpload() throws Exception {
        stubQueryCpsProsecutorData("/restResource/referencedata.query.prosecutor.by.oucode.json", randomUUID(), HttpStatus.SC_OK);
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("Harry")))));

        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");
        verifyAddCourtDocumentForCase();
        verifyInMessagingQueueForPublicCourtDocumentAdded();
    }

    private void verifyAddCourtDocumentForCase() {
        final String body = prepareAddCourtDocumentPayload();
        final Response writeResponse = postCommand(getWriteUrl("/courtdocument/" + docId),
                "application/vnd.progression.add-court-document-v2+json", body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                List.of(allOf(withJsonPath("$.prosecutionCase.defendants[0].cpsDefendantId", is(cpsDefendantId)),
                        withJsonPath("$.prosecutionCase.prosecutor.prosecutorCode", is("TFL"))))));

        getCourtDocumentsByCaseWithMatchers(UUID.randomUUID().toString(), docId, caseId);
    }

    private String prepareAddCourtDocumentPayload() {
        String body = getPayload("progression.add-court-document-for-case-v2.json");
        body = body.replaceAll("%RANDOM_DOCUMENT_ID%", docId)
                .replaceAll("%RANDOM_CASE_ID%", caseId)
                .replaceAll("%RANDOM_DEFENDANT_ID1%", defendantId)
                .replaceAll("%RANDOM_CPS_DEFENDANT_ID%", cpsDefendantId);

        return body;
    }

    private void addCourtDocument(final String docId, final String caseId, final String defendantId) throws IOException {
        String body = Resources.toString(Resources.getResource("progression.add-court-document.json"), Charset.defaultCharset());
        body = body.replaceAll("%RANDOM_DOCUMENT_ID%", docId)
                .replaceAll("%RANDOM_CASE_ID%", caseId)
                .replaceAll("%RANDOM_DEFENDANT_ID1%", defendantId);
        final Response writeResponse = postCommand(getWriteUrl("/courtdocument/" + docId),
                "application/vnd.progression.add-court-document+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
    }

    private CustomComparator getCustomComparatorForPaging() {
        return new CustomComparator(STRICT,
                new Customization("courtDocuments[0].material.uploadDateTime", (o1, o2) -> true),
                new Customization("courtDocuments[0].documentTypeRBAC", (o1, o2) -> true),
                new Customization("courtDocuments[0].material.id", (o1, o2) -> true)
        );
    }

    private void verifyInMessagingQueueForPublicCourtDocumentAdded() {
        final Optional<JsonObject> message = retrieveMessageBody(publicEventConsumer);
        assertTrue(message.isPresent());
    }

    @Test
    public void uploadApplicationDocument() throws Exception {
        final UUID applicationId = randomUUID();
        final UUID materialId = randomUUID();
        final UUID docId = randomUUID();
        final UUID documentTypeId = UUID.fromString("460f7ec0-c002-11e8-a355-529269fb1459");
        final ZonedDateTime uploadTime = ZonedDateTime.now();

        final CourtDocument courtDocument = courtDocument()
                .withCourtDocumentId(docId)
                .withDocumentTypeId(documentTypeId)
                .withDocumentTypeDescription("test document")
                .withName("test")
                .withMimeType("mimeType")
                .withMaterials(singletonList(
                        Material.material()
                                .withId(materialId)
                                .withName("immaterial")
                                .withUserGroups(List.of("Court Admin"))
                                .withUploadDateTime(uploadTime)
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
        final Response writeResponse = postCommand(getWriteUrl("/courtdocument/" + docId),
                "application/vnd.progression.add-court-document+json",
                strJson);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        //search for the document by application id
        ReferenceDataStub.stubQueryDocumentTypeData("/restResource/ref-data-document-type.json", documentTypeId.toString());
        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");

        assertCourtDocumentByApplication(docId.toString(), documentTypeId.toString(), materialId.toString(), applicationId.toString());
    }


    private void assertCourtDocumentByApplication(String documentId, String documentTypeId, String materialId, String applicationId) throws JSONException {
        final String courtDocumentsByApplication = getCourtDocumentsByApplication(USER_ID_VALUE.toString(), applicationId);
        final String expectedPayload = getPayload("expected/expected.progression.upload.court-document-1.json")
                .replace("%DOCUMENT_ID%", documentId)
                .replace("%APPLICATION_ID%", applicationId)
                .replace("%DOCUMENT_TYPE_ID%", documentTypeId)
                .replace("%MATERIAL_ID%", materialId);


        assertEquals(expectedPayload, courtDocumentsByApplication, getCustomComparatorForApplication());
    }

    private CustomComparator getCustomComparatorForApplication() {
        return new CustomComparator(STRICT,
                new Customization("documentIndices[0].document.materials[0].uploadDateTime", (o1, o2) -> true)
        );
    }

    static class UploadRequest {
        private CourtDocument courtDocument;

        public CourtDocument getCourtDocument() {
            return courtDocument;
        }

        public void setCourtDocument(final CourtDocument courtDocument) {
            this.courtDocument = courtDocument;
        }
    }

}
