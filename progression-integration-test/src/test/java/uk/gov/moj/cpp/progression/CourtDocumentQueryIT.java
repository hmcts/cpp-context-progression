package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentsByCase;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentsByCaseAndDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentsByCaseIdForProsecution;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentsByCaseIdForProsecutionWithNoCaseId;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentsByDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentsByDefendantForDefence;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentsByDefendantForDefenceWithNoCaseAndDefenceId;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentsByDefendantForDefenceWithNoCaseId;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommandWithUserId;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubGetDocumentsTypeAccess;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryDocumentTypeData;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.setupAsAuthorisedUser;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.stubAdvocateRoleInCaseByCaseId;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.stubUserGroupDefenceClientPermission;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.stubUserGroupOrganisation;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.moj.cpp.progression.util.FileUtil;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.io.Resources;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

public class CourtDocumentQueryIT {

    private static final String USER_ID = "07e9cd55-0eff-4eb3-961f-0d83e259e415";
    private static final String CHAMBER_USER_ID = "f5966b76-6e73-4be8-8780-ce542a46c8a4";
    private static final String ADVOCATE_USER_ID = "8d365984-0643-4b1a-81c6-3a0f7b750ddf";
    private String caseId;
    private String docId;
    private String defendantId;

    protected static final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @BeforeAll
    public static void setup() {
        setupAsAuthorisedUser(fromString(USER_ID), "stub-data/usersgroups.get-specific-groups-by-user.json");
        setupAsAuthorisedUser(fromString(CHAMBER_USER_ID), "stub-data/usersgroups.get-chamber-groups-by-user.json");
        setupAsAuthorisedUser(fromString(ADVOCATE_USER_ID), "stub-data/usersgroups.get-advocateuser-groups-by-user.json");
    }

    @BeforeEach
    public void setUp() throws IOException, JSONException {
        caseId = UUID.randomUUID().toString();
        docId = UUID.randomUUID().toString();
        defendantId = UUID.randomUUID().toString();
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.id", is(caseId)))));

    }

    @Test
    public void shouldGetCourtDocumentsForGivenDefendantLevelDocsBasedOnRBAC() throws IOException, JSONException {
        final String docTypeId = "460f7ec0-c002-11e8-a355-529269fb1459";

        //Doc Type Ref Data
        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json", docTypeId);
        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");
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

        assertEquals(expectedPayload, actualDocument, getCustomComparator());
    }


    @Test
    public void shouldGetCourtDocumentsForGivenCaseLevelAndDefendantLevelDocsBasedOnRBAC() throws IOException, JSONException {
        final String docTypeId = "460f7ec0-c002-11e8-a355-529269fb1459";

        stubQueryDocumentTypeData("/restResource/ref-data-document-type-seqnum.json");
        //Defendant Level Document 1
        final String courtDocumentDefendantLevel = addCourtDocument(caseId, docId, defendantId,
                prepareAddCourtDocumentWithDocTypePayload(docTypeId, docId, caseId, defendantId, "progression.add-court-document-def-level.json"));
        final JsonObject courtDocumentDefendantLevelJson = new StringToJsonObjectConverter().convert(courtDocumentDefendantLevel);
        final String defendantLevelId = courtDocumentDefendantLevelJson.getJsonObject("courtDocument").getString("courtDocumentId");

        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");
        //Case Level Document 2
        final String docId1 = UUID.randomUUID().toString();
        final String docTypeId2 = "460f8154-c002-11e8-a355-529269fb1459";
        final String courtDocumentCaseLevel2 = addCourtDocument(caseId, docId1, defendantId,
                prepareAddCourtDocumentWithDocTypePayload(docTypeId2, docId1, caseId, defendantId, "progression.add-court-document-doctype-level.json"));
        final JsonObject courtDocumentCaseLevelJson2 = new StringToJsonObjectConverter().convert(courtDocumentCaseLevel2);
        final String caseLevelCourtDocumentId2 = courtDocumentCaseLevelJson2.getJsonObject("courtDocument").getString("courtDocumentId");

        //Doc Type Ref Data
        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json", docTypeId);
        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");

        final String actualDocument = getCourtDocumentsByCaseAndDefendant(USER_ID, caseId, defendantId);

        final String expectedPayload = FileUtil.getPayload("expected/expected.progression.court-document-case-def-level.json")
                .replace("COURT-DOCUMENT-ID1", caseLevelCourtDocumentId2)
                .replace("COURT-DOCUMENT-ID2", defendantLevelId)
                .replace("DOCUMENT-TYPE-ID1", docTypeId2)
                .replace("DOCUMENT-TYPE-ID2", docTypeId)
                .replace("CASE-ID", caseId)
                .replace("DEFENDANT-ID", defendantId);

        assertEquals(expectedPayload, actualDocument, getCustomComparator());
    }

    @Test
    public void shouldGetCourtDocumentsForGivenCaseLevelAndDefendantLevelDocsGetOnlyDefendantBasedOnRBAC() throws IOException, JSONException {
        final String docTypeId = "460f7ec0-c002-11e8-a355-529269fb1459";

        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");
        //Defendant Level Document 1
        final String courtDocumentDefendantLevel = addCourtDocument(caseId, docId, defendantId,
                prepareAddCourtDocumentWithDocTypePayload(docTypeId, docId, caseId, defendantId, "progression.add-court-document-def-level.json"));
        final JsonObject courtDocumentDefendantLevelJson = new StringToJsonObjectConverter().convert(courtDocumentDefendantLevel);
        final String defendantLevelId = courtDocumentDefendantLevelJson.getJsonObject("courtDocument").getString("courtDocumentId");

        stubQueryDocumentTypeData("/restResource/ref-data-document-type-seqnum.json");

        //Case Level Document 2
        final String docId1 = UUID.randomUUID().toString();
        final String docTypeId2 = "460fb0ca-c002-11e8-a355-529269fb1459";
        addCourtDocument(caseId, docId1, defendantId,
                prepareAddCourtDocumentWithDocTypePayload(docTypeId2, docId1, caseId, defendantId, "progression.add-court-document-def-level.json"));

        //Doc Type Ref Data
        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json", docTypeId);
        stubQueryDocumentTypeData("/restResource/ref-data-document-type-legal-advisor.json", docTypeId2);
        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");

        final String actualDocument = getCourtDocumentsByCaseAndDefendant(USER_ID, caseId, defendantId);

        final String expectedPayload = FileUtil.getPayload("expected/expected.progression.court-document-def-level.json")
                .replace("COURT-DOCUMENT-ID", defendantLevelId)
                .replace("DOCUMENT-TYPE-ID", docTypeId)
                .replace("CASE-ID", caseId)
                .replace("DEFENDANT-ID", defendantId);

        assertEquals(expectedPayload, actualDocument, getCustomComparator());
    }


    @Test
    public void shouldGetCourtDocumentsForGivenCaseLevelDocsBasedOnRBAC() throws IOException, JSONException {
        final String docTypeId = UUID.randomUUID().toString();

        stubQueryDocumentTypeData("/restResource/ref-data-document-type-seqnum.json");
        //Case Level Document 1
        addCourtDocument(caseId, docId, defendantId,
                prepareAddCourtDocumentWithDocTypePayload(docTypeId, docId, caseId, defendantId, "progression.add-court-document-doctype-level.json"));

        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");
        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");

        //Case Level Document 2
        final String docId1 = randomUUID().toString();
        final String docTypeId2 = "460f8154-c002-11e8-a355-529269fb1459";
        final String courtDocumentCaseLevel2 = addCourtDocument(caseId, docId1, defendantId,
                prepareAddCourtDocumentWithDocTypePayload(docTypeId2, docId1, caseId, defendantId, "progression.add-court-document-doctype-level.json"));
        final JsonObject courtDocumentCaseLevelJson2 = new StringToJsonObjectConverter().convert(courtDocumentCaseLevel2);
        final String caseLevelCourtDocumentId2 = courtDocumentCaseLevelJson2.getJsonObject("courtDocument").getString("courtDocumentId");

        //Doc Type Ref Data
        stubQueryDocumentTypeData("/restResource/ref-data-document-type-legal-advisor.json", docTypeId);
        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json", docTypeId2);


        final String actualDocument = getCourtDocumentsByCase(USER_ID, caseId);

        final String expectedPayload = FileUtil.getPayload("expected/expected.progression.court-document.json")
                .replace("CASE-LEVEL-COURT-DOCUMENT-ID", caseLevelCourtDocumentId2)
                .replace("DOCUMENT-TYPE-ID1", docTypeId2)
                .replace("CASE-ID", caseId);

        assertEquals(expectedPayload, actualDocument, getCustomComparator());
    }


    @Test
    public void shouldNotGetCourtDocumentsForGivenCaseLevelDocsBasedOnRBAC() throws IOException, JSONException {
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
        stubQueryDocumentTypeData("/restResource/ref-data-document-type-legal-advisor.json", docTypeId);
        stubQueryDocumentTypeData("/restResource/ref-data-document-type-legal-advisor.json", docTypeId2);

        final String actualDocument = getCourtDocumentsByCase(USER_ID, caseId);

        final String expectedPayload = "{\"documentIndices\":[]}";

        assertEquals(expectedPayload, actualDocument, getCustomComparator());
    }

    @Test
    public void shouldGetAllCourtDocumentsForGivenCaseLevelDocsBasedOnRBAC() throws IOException, JSONException {

        final String docTypeId = "460f7ec0-c002-11e8-a355-529269fb1459";
        stubQueryDocumentTypeData("/restResource/ref-data-document-type-seqnum.json");
        //Case Level Document 1
        final String courtDocumentCaseLevel = addCourtDocument(caseId, docId, defendantId,
                prepareAddCourtDocumentWithDocTypePayload(docTypeId, docId, caseId, defendantId, "progression.add-court-document-doctype-level.json"));
        final JsonObject courtDocumentCaseLevelJson = new StringToJsonObjectConverter().convert(courtDocumentCaseLevel);
        final String caseLevelCourtDocumentId1 = courtDocumentCaseLevelJson.getJsonObject("courtDocument").getString("courtDocumentId");

        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");
        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");
        //Case Level Document 2
        final String docId1 = UUID.randomUUID().toString();
        final String docTypeId2 = "460f8154-c002-11e8-a355-529269fb1459";
        final String courtDocumentCaseLevel2 = addCourtDocument(caseId, docId1, defendantId,
                prepareAddCourtDocumentWithDocTypePayload(docTypeId2, docId1, caseId, defendantId, "progression.add-court-document-doctype-level.json"));
        final JsonObject courtDocumentCaseLevelJson2 = new StringToJsonObjectConverter().convert(courtDocumentCaseLevel2);
        final String caseLevelCourtDocumentId2 = courtDocumentCaseLevelJson2.getJsonObject("courtDocument").getString("courtDocumentId");

        final String actualDocument = getCourtDocumentsByCase(USER_ID, caseId);

        final String expectedPayload = FileUtil.getPayload("expected/expected.progression.court-documents.json")
                .replace("CASE-LEVEL-COURT-DOCUMENT-ID", caseLevelCourtDocumentId2)
                .replace("COURT-DOCUMENT-ID2", caseLevelCourtDocumentId1)
                .replace("DOCUMENT-TYPE-ID1", docTypeId2)
                .replace("DOCUMENT-TYPE-ID2", docTypeId)
                .replace("CASE-ID", caseId);
        assertEquals(expectedPayload, actualDocument, getCustomComparator());
    }

    @Test
    public void shouldGetAllCourtDocumentsForGivenCaseAndDefendantToDefenceUser() throws IOException, JSONException {
        final String docTypeId = "460fbc00-c002-11e8-a355-529269fb1459";
        final String organisationId = UUID.randomUUID().toString();

        final String organisation = getPayload("stub-data/usersgroups.get-organisation-details.json")
                .replace("%ORGANISATION_ID%", organisationId);

        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");

        final String permission = getPayload("stub-data/usersgroups.get-permission-for-user-by-defendant.json")
                .replace("%USER_ID%", CHAMBER_USER_ID)
                .replace("%DEFENDANT_ID%", defendantId)
                .replace("%ORGANISATION_ID%", organisationId);

        stubUserGroupOrganisation(CHAMBER_USER_ID, organisation);
        stubUserGroupDefenceClientPermission(permission);


        stubQueryDocumentTypeData("/restResource/ref-data-document-type-for-defence.json");
        stubQueryDocumentTypeData("/restResource/ref-data-document-type-for-defence.json", docTypeId);
        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");
        //Defendant Level Document 1
        final String courtDocumentDefendantLevel = addCourtDocumentForDefence(caseId, docId, defendantId,
                prepareAddCourtDocumentWithDocTypePayload(docTypeId, docId, caseId, defendantId, "progression.add-court-document-def-level.json"));
        final JsonObject courtDocumentDefendantLevelJson = new StringToJsonObjectConverter().convert(courtDocumentDefendantLevel);
        final String defendantLevelId = courtDocumentDefendantLevelJson.getJsonObject("courtDocument").getString("courtDocumentId");

        //Doc Type Ref Data

        stubAdvocateRoleInCaseByCaseId(caseId, getRoleInCasePermissionPayload(caseId, defendantId, "defending"));

        final String actualDocument = getCourtDocumentsByDefendantForDefence(CHAMBER_USER_ID, caseId, status().is(OK));

        final String expectedPayload = FileUtil.getPayload("expected/expected.progression.court-document-case-def-level1.json")
                .replace("COURT-DOCUMENT-ID1", defendantLevelId)
                .replace("DOCUMENT-TYPE-ID1", docTypeId)
                .replace("CASE-ID", caseId)
                .replace("DEFENDANT-ID", defendantId);

        assertEquals(expectedPayload, actualDocument, getCustomComparator());
    }

    private String getRoleInCasePermissionPayload(final String caseId, final String defendantId, final String role) {
        return getPayload("stub-data/defence.advocate.query.role-in-case-by-caseid-with-defendant-list.json")
                .replace("%CASE_ID%", caseId)
                .replace("%DEFENDANT_ID%", defendantId)
                .replace("%USER_ROLE_IN_CASE%", role);
    }

    @Test
    public void shouldBadRequestWhenNoCaseAndDefendantForCourtDocumentsForGivenCaseAndDefendantToDefenceUser() {
        getCourtDocumentsByDefendantForDefenceWithNoCaseAndDefenceId(CHAMBER_USER_ID, status().is(BAD_REQUEST));
    }

    @Test
    public void shouldBadRequestWhenNoCaseForCourtDocumentsForGivenCaseAndDefendantToDefenceUser() {
        getCourtDocumentsByDefendantForDefenceWithNoCaseId(CHAMBER_USER_ID, UUID.randomUUID().toString(), status().is(BAD_REQUEST));
    }

    @Test
    public void shouldReturnBadRequestWhenNoCaseIdForCourtDocumentsForGivenCaseToTheUserInProsecutorRole() {
        getCourtDocumentsByCaseIdForProsecutionWithNoCaseId(ADVOCATE_USER_ID, status().is(BAD_REQUEST));
    }

    @Test
    public void shouldBeForbiddenWhenUserRoleInCaseIsDefending() {

        final String userRoleInCase = getPayload("stub-data/defence.advocate.query.role-in-case-by-caseid.json")
                .replace("%CASE_ID%", caseId)
                .replace("%USER_ROLE_IN_CASE%", "defending");

        stubAdvocateRoleInCaseByCaseId(caseId, userRoleInCase);

        getCourtDocumentsByCaseIdForProsecution(ADVOCATE_USER_ID, caseId, status().is(FORBIDDEN));
    }

    @Test
    public void shouldSearchCourtDocumentByCaseIDWhenTheUserRoleInCaseIsProsecutorOrBothProsecutorAndDefence() throws IOException {
        final String userRoleInCase = getPayload("stub-data/defence.advocate.query.role-in-case-by-caseid.json")
                .replace("%CASE_ID%", caseId)
                .replace("%USER_ROLE_IN_CASE%", "prosecuting");

        stubAdvocateRoleInCaseByCaseId(caseId, userRoleInCase);

        final String docTypeId = "460f8974-c002-11e8-a355-529269fb1459";
        stubQueryDocumentTypeData("/restResource/ref-data-document-type-with-advocates.json");

        //Case Level Document 1
        final String courtDocumentCaseLevel = addCourtDocument(caseId, docId, defendantId,
                prepareAddCourtDocumentWithDocTypePayload(docTypeId, docId, caseId, defendantId, "progression.add-court-document-doctype-level.json"));
        final JsonObject courtDocumentCaseLevelJson = new StringToJsonObjectConverter().convert(courtDocumentCaseLevel);
        final String caseLevelCourtDocumentId1 = courtDocumentCaseLevelJson.getJsonObject("courtDocument").getString("courtDocumentId");

        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");
        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");

        final String actualDocument = getCourtDocumentsByCaseIdForProsecution(ADVOCATE_USER_ID, caseId, status().is(OK));

        JsonObject json = stringToJsonObjectConverter.convert(actualDocument);
        assertThat(json.getJsonArray("documentIndices").getJsonObject(0).getJsonArray("caseIds").getString(0), is(caseId));
        assertThat(json.getJsonArray("documentIndices").getJsonObject(0).getJsonObject("document").getString("courtDocumentId"), is(caseLevelCourtDocumentId1));
    }

    private CustomComparator getCustomComparator() {
        return new CustomComparator(STRICT,
                new Customization("documentIndices[0].document.materials[0].uploadDateTime", (o1, o2) -> true),
                new Customization("documentIndices[1].document.materials[0].uploadDateTime", (o1, o2) -> true),
                new Customization("documentIndices[0].document.documentTypeRBAC", (o1, o2) -> true),
                new Customization("documentIndices[1].document.documentTypeRBAC", (o1, o2) -> true)
        );
    }

    private String addCourtDocument(final String caseId, final String docId, final String defendantId, final String body) {

        final Response writeResponse = postCommand(getWriteUrl("/courtdocument/" + docId),
                "application/vnd.progression.add-court-document+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        return getCourtDocumentFor(docId, allOf(
                withJsonPath("$.courtDocument.courtDocumentId", equalTo(docId)),
                withJsonPath("$.courtDocument.containsFinancialMeans", equalTo(true))
        ));
    }

    private String addCourtDocumentForDefence(final String caseId, final String docId, final String defendantId, final String body) {

        final Response writeResponse = postCommandWithUserId(getWriteUrl(format("/defendant/%s/courtdocument/%s", defendantId, docId)),
                "application/vnd.progression.add-court-document-for-defence+json",
                body, CHAMBER_USER_ID);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        return getCourtDocumentFor(docId, allOf(
                withJsonPath("$.courtDocument.courtDocumentId", equalTo(docId)),
                withJsonPath("$.courtDocument.containsFinancialMeans", equalTo(true))
        ));
    }

    private String prepareAddCourtDocumentWithDocTypePayload(final String docTypeId, final String docId, final String caseId, final String defendantId, final String addCourtDocumentResource) throws IOException {
        String body = Resources.toString(Resources.getResource(addCourtDocumentResource),
                Charset.defaultCharset());
        body = body.replaceAll("%RANDOM_DOCUMENT_ID%", docId)
                .replaceAll("%RANDOM_CASE_ID%", caseId)
                .replaceAll("%RANDOM_DEFENDANT_ID%", defendantId)
                .replaceAll("%RANDOM_DOC_TYPE%", docTypeId);
        return body;
    }
}
