package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.fromString;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentsByCase;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentsByCaseAndDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentsByDefendant;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryDocumentTypeData;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.setupAsAuthorisedUser;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.moj.cpp.progression.stub.ReferenceDataStub;
import uk.gov.moj.cpp.progression.util.FileUtil;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.io.Resources;
import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.BeforeClass;
import org.junit.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

public class CourtDocumentQueryIT extends AbstractIT {

    private static final String USER_ID = "07e9cd55-0eff-4eb3-961f-0d83e259e415";

    @BeforeClass
    public static void setup() {
        setupAsAuthorisedUser(fromString(USER_ID), "stub-data/usersgroups.get-specific-groups-by-user.json");
    }


    @Test
    public void shouldGetCourtDocumentsForGivenDefendantLevelDocsBasedOnRBAC() throws IOException {


        final String caseId = UUID.randomUUID().toString();
        final String docId = UUID.randomUUID().toString();
        final String defendantId = UUID.randomUUID().toString();
        final String docTypeId = UUID.randomUUID().toString();

        //Doc Type Ref Data
        ReferenceDataStub.stubQueryDocumentTypeData("/restResource/ref-data-document-type.json", docTypeId);
        //Defendant Level Document 1
        final String courtDocumentDefendantLevel = addCourtDocument(caseId, docId, defendantId,
                prepareAddCourtDocumentWithDocTypePayload(docTypeId, docId, caseId, defendantId, "progression.add-court-document-def-level.json"));
        final JsonObject courtDocumentDefendantLevelJson = new StringToJsonObjectConverter().convert(courtDocumentDefendantLevel);
        final String defendantLevelId = courtDocumentDefendantLevelJson.getJsonObject("courtDocument").getString("courtDocumentId");

        final String actualDocument = getCourtDocumentsByDefendant(USER_ID, defendantId);

        final String expectedPayload = FileUtil.getPayload("expected/expected.progression.court-document-def-level.json")
                .replace("COURT-DOCUMENT-ID", defendantLevelId)
                .replace("DOCUMENT-TYPE-ID", docTypeId)
                .replace("CASE-ID", caseId)
                .replace("DEFENDANT-ID", defendantId);

        JSONAssert.assertEquals(expectedPayload, actualDocument, getCustomComparator());
    }


    @Test
    public void shouldGetCourtDocumentsForGivenCaseLevelAndDefendantLevelDocsBasedOnRBAC() throws IOException {


        final String caseId = UUID.randomUUID().toString();
        final String docId = UUID.randomUUID().toString();
        final String defendantId = UUID.randomUUID().toString();
        final String docTypeId = UUID.randomUUID().toString();

        stubQueryDocumentTypeData("/restResource/ref-data-document-type-seqnum.json");
        //Defendant Level Document 1
        final String courtDocumentDefendantLevel = addCourtDocument(caseId, docId, defendantId,
                prepareAddCourtDocumentWithDocTypePayload(docTypeId, docId, caseId, defendantId, "progression.add-court-document-def-level.json"));
        final JsonObject courtDocumentDefendantLevelJson = new StringToJsonObjectConverter().convert(courtDocumentDefendantLevel);
        final String defendantLevelId = courtDocumentDefendantLevelJson.getJsonObject("courtDocument").getString("courtDocumentId");

        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");
        //Case Level Document 2
        final String docId1 = UUID.randomUUID().toString();
        final String docTypeId2 = UUID.randomUUID().toString();
        final String courtDocumentCaseLevel2 = addCourtDocument(caseId, docId1, defendantId,
                prepareAddCourtDocumentWithDocTypePayload(docTypeId2, docId1, caseId, defendantId, "progression.add-court-document-doctype-level.json"));
        final JsonObject courtDocumentCaseLevelJson2 = new StringToJsonObjectConverter().convert(courtDocumentCaseLevel2);
        final String caseLevelCourtDocumentId2 = courtDocumentCaseLevelJson2.getJsonObject("courtDocument").getString("courtDocumentId");


        //Doc Type Ref Data
        ReferenceDataStub.stubQueryDocumentTypeData("/restResource/ref-data-document-type.json", docTypeId);
        ReferenceDataStub.stubQueryDocumentTypeData("/restResource/ref-data-document-type.json", docTypeId2);


        final String actualDocument = getCourtDocumentsByCaseAndDefendant(USER_ID, caseId, defendantId);

        final String expectedPayload = FileUtil.getPayload("expected/expected.progression.court-document-case-def-level.json")
                .replace("COURT-DOCUMENT-ID1", caseLevelCourtDocumentId2)
                .replace("COURT-DOCUMENT-ID2", defendantLevelId)
                .replace("DOCUMENT-TYPE-ID1", docTypeId2)
                .replace("DOCUMENT-TYPE-ID2", docTypeId)
                .replace("CASE-ID", caseId)
                .replace("DEFENDANT-ID", defendantId);

        JSONAssert.assertEquals(expectedPayload, actualDocument, getCustomComparator());
    }

    @Test
    public void shouldGetCourtDocumentsForGivenCaseLevelAndDefendantLevelDocsGetOnlyDefendantBasedOnRBAC() throws IOException {

        final String caseId = UUID.randomUUID().toString();
        final String docId = UUID.randomUUID().toString();
        final String defendantId = UUID.randomUUID().toString();
        final String docTypeId = UUID.randomUUID().toString();

        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");
        //Defendant Level Document 1
        final String courtDocumentDefendantLevel = addCourtDocument(caseId, docId, defendantId,
                prepareAddCourtDocumentWithDocTypePayload(docTypeId, docId, caseId, defendantId, "progression.add-court-document-def-level.json"));
        final JsonObject courtDocumentDefendantLevelJson = new StringToJsonObjectConverter().convert(courtDocumentDefendantLevel);
        final String defendantLevelId = courtDocumentDefendantLevelJson.getJsonObject("courtDocument").getString("courtDocumentId");

        stubQueryDocumentTypeData("/restResource/ref-data-document-type-seqnum.json");

        //Case Level Document 2
        final String docId1 = UUID.randomUUID().toString();
        final String docTypeId2 = UUID.randomUUID().toString();
        final String courtDocumentCaseLevel2 = addCourtDocument(caseId, docId1, defendantId,
                prepareAddCourtDocumentWithDocTypePayload(docTypeId2, docId1, caseId, defendantId, "progression.add-court-document-def-level.json"));
        final JsonObject courtDocumentCaseLevelJson2 = new StringToJsonObjectConverter().convert(courtDocumentCaseLevel2);
        final String caseLevelCourtDocumentId2 = courtDocumentCaseLevelJson2.getJsonObject("courtDocument").getString("courtDocumentId");


        //Doc Type Ref Data
        ReferenceDataStub.stubQueryDocumentTypeData("/restResource/ref-data-document-type.json", docTypeId);
        ReferenceDataStub.stubQueryDocumentTypeData("/restResource/ref-data-document-type-legal-advisor.json", docTypeId2);


        final String actualDocument = getCourtDocumentsByCaseAndDefendant(USER_ID, caseId, defendantId);

        final String expectedPayload = FileUtil.getPayload("expected/expected.progression.court-document-def-level.json")
                .replace("COURT-DOCUMENT-ID", defendantLevelId)
                .replace("DOCUMENT-TYPE-ID", docTypeId)
                .replace("CASE-ID", caseId)
                .replace("DEFENDANT-ID", defendantId);

        JSONAssert.assertEquals(expectedPayload, actualDocument, getCustomComparator());
    }


    @Test
    public void shouldGetCourtDocumentsForGivenCaseLevelDocsBasedOnRBAC() throws IOException {


        final String caseId = UUID.randomUUID().toString();
        final String docId = UUID.randomUUID().toString();
        final String defendantId = UUID.randomUUID().toString();
        final String docTypeId = UUID.randomUUID().toString();

        stubQueryDocumentTypeData("/restResource/ref-data-document-type-seqnum.json");
        //Case Level Document 1
        addCourtDocument(caseId, docId, defendantId,
                prepareAddCourtDocumentWithDocTypePayload(docTypeId, docId, caseId, defendantId, "progression.add-court-document-doctype-level.json"));

        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");
        //Case Level Document 2
        final String docId1 = UUID.randomUUID().toString();
        final String docTypeId2 = UUID.randomUUID().toString();
        final String courtDocumentCaseLevel2 = addCourtDocument(caseId, docId1, defendantId,
                prepareAddCourtDocumentWithDocTypePayload(docTypeId2, docId1, caseId, defendantId, "progression.add-court-document-doctype-level.json"));
        final JsonObject courtDocumentCaseLevelJson2 = new StringToJsonObjectConverter().convert(courtDocumentCaseLevel2);
        final String caseLevelCourtDocumentId2 = courtDocumentCaseLevelJson2.getJsonObject("courtDocument").getString("courtDocumentId");

        //Doc Type Ref Data
        ReferenceDataStub.stubQueryDocumentTypeData("/restResource/ref-data-document-type-legal-advisor.json", docTypeId);
        ReferenceDataStub.stubQueryDocumentTypeData("/restResource/ref-data-document-type.json", docTypeId2);


        final String actualDocument = getCourtDocumentsByCase(USER_ID, caseId.toString());

        final String expectedPayload = FileUtil.getPayload("expected/expected.progression.court-document.json")
                .replace("CASE-LEVEL-COURT-DOCUMENT-ID", caseLevelCourtDocumentId2)
                .replace("DOCUMENT-TYPE-ID1", docTypeId2)
                .replace("CASE-ID", caseId.toString());

        JSONAssert.assertEquals(expectedPayload, actualDocument, getCustomComparator());
    }


    @Test
    public void shouldNotGetCourtDocumentsForGivenCaseLevelDocsBasedOnRBAC() throws IOException {

        final String caseId = UUID.randomUUID().toString();
        final String docId = UUID.randomUUID().toString();
        final String defendantId = UUID.randomUUID().toString();
        final String docTypeId = UUID.randomUUID().toString();

        //Case Level Document 1
        addCourtDocument(caseId, docId, defendantId,
                prepareAddCourtDocumentWithDocTypePayload(docTypeId, docId, caseId, defendantId, "progression.add-court-document-doctype-level.json"));

        //Case Level Document 2
        final String docId1 = UUID.randomUUID().toString();
        final String docTypeId2 = UUID.randomUUID().toString();

        addCourtDocument(caseId, docId1, defendantId,
                prepareAddCourtDocumentWithDocTypePayload(docTypeId2, docId1, caseId, defendantId, "progression.add-court-document-doctype-level.json"));

        //Doc Type Ref Data
        ReferenceDataStub.stubQueryDocumentTypeData("/restResource/ref-data-document-type-legal-advisor.json", docTypeId);
        ReferenceDataStub.stubQueryDocumentTypeData("/restResource/ref-data-document-type-legal-advisor.json", docTypeId2);

        final String actualDocument = getCourtDocumentsByCase(USER_ID, caseId.toString());

        final String expectedPayload = "{\"documentIndices\":[]}";

        JSONAssert.assertEquals(expectedPayload, actualDocument, getCustomComparator());
    }

    @Test
    public void shouldGetAllCourtDocumentsForGivenCaseLevelDocsBasedOnRBAC() throws IOException {

        final String caseId = UUID.randomUUID().toString();
        final String docId = UUID.randomUUID().toString();
        final String defendantId = UUID.randomUUID().toString();
        final String docTypeId = UUID.randomUUID().toString();
        stubQueryDocumentTypeData("/restResource/ref-data-document-type-seqnum.json");
        //Case Level Document 1
        final String courtDocumentCaseLevel = addCourtDocument(caseId, docId, defendantId,
                prepareAddCourtDocumentWithDocTypePayload(docTypeId, docId, caseId, defendantId, "progression.add-court-document-doctype-level.json"));
        final JsonObject courtDocumentCaseLevelJson = new StringToJsonObjectConverter().convert(courtDocumentCaseLevel);
        final String caseLevelCourtDocumentId1 = courtDocumentCaseLevelJson.getJsonObject("courtDocument").getString("courtDocumentId");

        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");
        //Case Level Document 2
        final String docId1 = UUID.randomUUID().toString();
        final String docTypeId2 = UUID.randomUUID().toString();
        final String courtDocumentCaseLevel2 = addCourtDocument(caseId, docId1, defendantId,
                prepareAddCourtDocumentWithDocTypePayload(docTypeId2, docId1, caseId, defendantId, "progression.add-court-document-doctype-level.json"));
        final JsonObject courtDocumentCaseLevelJson2 = new StringToJsonObjectConverter().convert(courtDocumentCaseLevel2);
        final String caseLevelCourtDocumentId2 = courtDocumentCaseLevelJson2.getJsonObject("courtDocument").getString("courtDocumentId");

        final String actualDocument = getCourtDocumentsByCase(USER_ID, caseId.toString());

        final String expectedPayload = FileUtil.getPayload("expected/expected.progression.court-documents.json")
                .replace("CASE-LEVEL-COURT-DOCUMENT-ID", caseLevelCourtDocumentId2)
                .replace("COURT-DOCUMENT-ID2", caseLevelCourtDocumentId1)
                .replace("DOCUMENT-TYPE-ID1", docTypeId2)
                .replace("DOCUMENT-TYPE-ID2", docTypeId)
                .replace("CASE-ID", caseId.toString());
        JSONAssert.assertEquals(expectedPayload, actualDocument, getCustomComparator());
    }


    private CustomComparator getCustomComparator() {
        return new CustomComparator(STRICT,
                new Customization("documentIndices[0].document.materials[0].uploadDateTime", (o1, o2) -> true),
                new Customization("documentIndices[1].document.materials[0].uploadDateTime", (o1, o2) -> true),
                new Customization("documentIndices[0].document.documentTypeRBAC", (o1, o2) -> true),
                new Customization("documentIndices[1].document.documentTypeRBAC", (o1, o2) -> true)
        );
    }

    private String addCourtDocument(final String caseId, final String docId, final String defendantId, final String body) throws IOException {

        final Response writeResponse = postCommand(getWriteUrl("/courtdocument/" + docId),
                "application/vnd.progression.add-court-document+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        return getCourtDocumentFor(docId, allOf(
                withJsonPath("$.courtDocument.courtDocumentId", equalTo(docId)),
                withJsonPath("$.courtDocument.containsFinancialMeans", equalTo(true))
        ));
    }

    private String prepareAddCourtDocumentWithDocTypePayload(final String docTypeId, final String docId, final String caseId, final String defendantId, final String addCourtDocumentResource) throws IOException {
        String body = Resources.toString(Resources.getResource(addCourtDocumentResource),
                Charset.defaultCharset());
        body = body.replaceAll("%RANDOM_DOCUMENT_ID%", docId.toString())
                .replaceAll("%RANDOM_CASE_ID%", caseId.toString())
                .replaceAll("%RANDOM_DEFENDANT_ID%", defendantId.toString())
                .replaceAll("%RANDOM_DOC_TYPE%", docTypeId.toString());
        return body;
    }
}
