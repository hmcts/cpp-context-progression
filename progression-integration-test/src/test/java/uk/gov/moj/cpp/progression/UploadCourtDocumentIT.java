package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentsByCase;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryDocumentTypeData;
import static uk.gov.moj.cpp.progression.test.matchers.BeanMatcher.isBean;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.QueryUtil.waitForQueryMatch;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.courts.progression.query.ApplicationDocument;
import uk.gov.justice.courts.progression.query.Courtdocuments;
import uk.gov.justice.courts.progression.query.DocumentCategory;
import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.moj.cpp.progression.helper.MultipartFileUploadHelper;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.test.matchers.BeanMatcher;
import uk.gov.moj.cpp.progression.util.Utilities;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.UUID;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

// Temporarily ignoring to unblock hearing pipeline
@Ignore
public class UploadCourtDocumentIT extends AbstractIT {

    private static final String PROGRESSION_QUERY_COURTDOCUMENTSSEARCHAPPLICATION = "/progression-service/query/api/rest/progression/courtdocumentsearch?applicationId=%s";

    private MultipartFileUploadHelper helper;

    private String caseId;
    private String docId;
    private String defendantId;

    private final MessageConsumer publicEventConsumer = publicEvents
            .createConsumer("public.progression.court-document-added");

    @Before
    public void setup() {
        helper = new MultipartFileUploadHelper();
        caseId = randomUUID().toString();
        docId = randomUUID().toString();
        defendantId = randomUUID().toString();
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

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));


        String body = Resources.toString(Resources.getResource("progression.add-court-document.json"), Charset.defaultCharset());
        body = body.replaceAll("%RANDOM_DOCUMENT_ID%", docId.toString())
                .replaceAll("%RANDOM_CASE_ID%", caseId.toString())
                .replaceAll("%RANDOM_DEFENDANT_ID%", defendantId.toString());
        final Response writeResponse = postCommand(getWriteUrl("/courtdocument/" + docId.toString()),
                "application/vnd.progression.add-court-document+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        final Matcher[] matcher = {
                withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath("$.courtDocuments[0].courtDocumentId", equalTo(docId))
        };

        assertCourtDocumentByCase();

        verifyInMessagingQueueForPublicCourtDocumentAdded();
    }

    private void assertCourtDocumentByCase() {
        final String courtDocumentsByCaseStatus = getCourtDocumentsByCase(UUID.randomUUID().toString(), caseId);
        final String expectedPayload = getPayload("expected/expected.progression.upload.court-document.json")
                .replace("COURT-DOCUMENT-ID1", docId)
                .replace("CASE-ID", caseId)
                .replace("DEFENDENT-ID", defendantId);


        assertEquals(expectedPayload, courtDocumentsByCaseStatus, getCustomComparator());
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

    private void verifyInMessagingQueueForPublicCourtDocumentAdded() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(publicEventConsumer);
        assertTrue(message.isPresent());
    }

    @Test
    public void uploadApplicationDocument() throws Exception {
        final UUID applicationId = randomUUID();
        final UUID materialId = randomUUID();
        final CourtDocument courtDocument = CourtDocument.courtDocument()
                .withCourtDocumentId(randomUUID())
                .withDocumentTypeId(randomUUID())
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
        final UUID docId = randomUUID();
        final Response writeResponse = postCommand(getWriteUrl("/courtdocument/" + docId.toString()),
                "application/vnd.progression.add-court-document+json",
                strJson);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        //search for the document by application id

        final BeanMatcher<Courtdocuments> preGeneratedResultMatcher = isBean(Courtdocuments.class)
                .withValue(cds -> cds.getDocumentIndices().size(), 1);

        final RequestParams preGeneratedRequestParams = requestParams(getReadUrl(String.format(PROGRESSION_QUERY_COURTDOCUMENTSSEARCHAPPLICATION, applicationId.toString())),
                APPLICATION_VND_PROGRESSION_QUERY_SEARCH_COURTDOCUMENTS_JSON)
                .withHeader(CPP_UID_HEADER.getName(), USER_ID_VALUE)
                .build();

        waitForQueryMatch(preGeneratedRequestParams, 45, preGeneratedResultMatcher, Courtdocuments.class);


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
