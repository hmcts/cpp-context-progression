package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.EventSelector.PUBLIC_EVENT_COURT_DOCUMENT_UPADTED;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addRemoveCourtDocument;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.verifyQueryResultsForbidden;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommandWithUserId;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryDocumentTypeData;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.setupAsAuthorisedUser;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.stubUserGroupDefenceClientPermission;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.stubUserGroupOrganisation;

import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.ReferenceDataStub;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.UUID;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

public class AddCourtDocumentIT extends AbstractIT {

    public static final String ACCESS_CONTROL_FAILED = "Access Control failed for json envelope";

    private String caseId;
    private String docId;
    private String defendantId;
    private String updatedDefendantId;
    public static final String USER_GROUP_NOT_PRESENT_DROOL = UUID.randomUUID().toString();
    public static final String USER_GROUP_NOT_PRESENT_RBAC = UUID.randomUUID().toString();
    public static final String CHAMBER_USER_ID = UUID.randomUUID().toString();
    private static final MessageConsumer PRIVATE_MESSAGE_CONSUMER = privateEvents.createConsumer("progression.event.court-document-update-failed");
    private static final MessageConsumer consumerForCourtDocumentUpdated = publicEvents.createConsumer(PUBLIC_EVENT_COURT_DOCUMENT_UPADTED);

    @BeforeClass
    public static void init() {

        setupAsAuthorisedUser(UUID.fromString(USER_GROUP_NOT_PRESENT_DROOL), "stub-data/usersgroups.get-invalid-groups-by-user.json");
        setupAsAuthorisedUser(UUID.fromString(USER_GROUP_NOT_PRESENT_RBAC), "stub-data/usersgroups.get-invalid-rbac-groups-by-user.json");
        setupAsAuthorisedUser(fromString(CHAMBER_USER_ID), "stub-data/usersgroups.get-chamber-groups-by-user.json");

    }


    @Before
    public void setup() {

        caseId = randomUUID().toString();
        docId = randomUUID().toString();
        defendantId = randomUUID().toString();
        updatedDefendantId = randomUUID().toString();
    }

    @Test
    public void shouldAddCourtDocument() throws IOException {
        verifyAddCourtDocument();

    }

    @Test
    public void shouldAddCourtDocumentAndThenUpdateIt() throws IOException {
        //Given
        verifyAddCourtDocument();

        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");


        //Given
        final String bodyForUpdate = prepareUpdateCourtDocumentPayload();
        //When
        final Response writeResponseForUpdate = postCommand(getWriteUrl("/courtdocument"),
                "application/vnd.progression.update-court-document+json",
                bodyForUpdate);
        assertThat(writeResponseForUpdate.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        final String actualDocumentAfterUpdate = getCourtDocumentFor(docId, allOf(
                withJsonPath("$.courtDocument.courtDocumentId", equalTo(docId)),
                withJsonPath("$.courtDocument.containsFinancialMeans", equalTo(true)),
                withJsonPath("$.courtDocument.documentTypeDescription", equalTo("Magistrate's Sending sheet"))
        ));


        final String expectedPayloadAfterUpdate = getPayload("expected/expected.progression.court-document-updated.json")
                .replace("COURT-DOCUMENT-ID", docId)
                .replace("DEFENDENT-ID", updatedDefendantId)
                .replace("CASE-ID", caseId);

        assertEquals(expectedPayloadAfterUpdate, actualDocumentAfterUpdate, getCustomComparator());

        verifyInPublicTopic();
    }

    @Test
    public void shouldAddCourtDocumentAndVerifyUpdateFailedEvent() throws IOException {
        //Given
        verifyAddCourtDocument();

        stubQueryDocumentTypeData("/restResource/ref-invalid-data-document-type.json");


        //Given
        final String bodyForUpdate = prepareUpdateCourtDocumentPayload();
        //When
        final Response writeResponseForUpdate = postCommand(getWriteUrl("/courtdocument"),
                "application/vnd.progression.update-court-document+json",
                bodyForUpdate);
        assertThat(writeResponseForUpdate.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        verifyUpdateFailedEvent();

        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");

    }

    @Test
    public void shouldAddCourtDocumentAndVerifyUpdateFailedEventWhenDocumentAlreadyDeleted() throws IOException {

        final String userId = "dd8dcdcf-58d1-4e45-8450-40b0f569a7e7";
        final String materialId = "5e1cc18c-76dc-47dd-99c1-d6f87385edf1";
        //Given
        verifyAddCourtDocument();

        setupAsAuthorisedUser(UUID.fromString(userId), "stub-data/usersgroups.get-support-groups-by-user.json");

        addRemoveCourtDocument(docId, materialId, true, UUID.fromString(userId));

        //Given
        final String bodyForUpdate = prepareUpdateCourtDocumentPayload();
        //When
        final Response writeResponseForUpdate = postCommand(getWriteUrl("/courtdocument"),
                "application/vnd.progression.update-court-document+json",
                bodyForUpdate);
        assertThat(writeResponseForUpdate.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        verifyUpdateFailedEventForRemovedDocument();

    }

    @Test
    public void shouldAddCourtDocumentAndForbidToQueryWhenNoRbacMatches() throws IOException {

        //Given
        final String body = prepareAddCourtDocumentPayload();
        //When
        final Response writeResponse = postCommand(getWriteUrl("/courtdocument/" + docId),
                "application/vnd.progression.add-court-document+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));


        //Group Not Present in Drool rule

        verifyQueryResultsForbidden(docId, USER_GROUP_NOT_PRESENT_DROOL, withJsonPath("$.error", StringContains.containsString(ACCESS_CONTROL_FAILED)));


        //Group Not Present in RBAC
        //Then
        verifyQueryResultsForbidden(docId, USER_GROUP_NOT_PRESENT_RBAC, withJsonPath("$.error", StringContains.containsString(ACCESS_CONTROL_FAILED)));


    }


    @Test
    public void shouldGetForbiddenExceptionWhenAddCourtDocumentAndNoRBACRulesMatches() throws IOException {

        //Given
        final String body = prepareAddCourtDocumentPayload();
        //When
        //postCommand()
        final Response writeResponse = postCommandWithUserId(getWriteUrl("/courtdocument/" + docId),
                "application/vnd.progression.add-court-document+json",
                body, USER_GROUP_NOT_PRESENT_RBAC);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_FORBIDDEN));

    }

    @Test
    public void shouldThrowBadRequestWhenDefendantIdNotMatching() throws IOException {
        final String caseId = UUID.randomUUID().toString();
        final String docId = UUID.randomUUID().toString();
        final String defendantId = UUID.randomUUID().toString();
        final String docTypeId = UUID.randomUUID().toString();
        final String organisationId = UUID.randomUUID().toString();

        final String organisation = getPayload("stub-data/usersgroups.get-organisation-details.json")
                .replace("%ORGANISATION_ID%", organisationId);


        final String permission = getPayload("stub-data/usersgroups.get-permission-for-user-by-defendant.json")
                .replace("%USER_ID%", randomUUID().toString())
                .replace("%DEFENDANT_ID%", defendantId)
                .replace("%ORGANISATION_ID%", randomUUID().toString());

        stubUserGroupOrganisation(CHAMBER_USER_ID, organisation);
        stubUserGroupDefenceClientPermission(defendantId, permission);


        stubQueryDocumentTypeData("/restResource/ref-data-document-type-for-defence.json");
        ReferenceDataStub.stubQueryDocumentTypeData("/restResource/ref-data-document-type-for-defence.json", docTypeId);


        addCourtDocumentForDefence(CHAMBER_USER_ID, docId, randomUUID().toString(),
                prepareAddCourtDocumentWithDocTypePayload(docTypeId, docId, caseId, defendantId, "progression.add-court-document-def-level.json"), HttpStatus.SC_BAD_REQUEST);
        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");


    }

    @Test
    public void shouldThrowForbiddenInCaseDefenceUserIsNotAssociatedOrGranted() throws IOException {
        final String caseId = UUID.randomUUID().toString();
        final String docId = UUID.randomUUID().toString();
        final String defendantId = UUID.randomUUID().toString();
        final String docTypeId = UUID.randomUUID().toString();
        final String organisationId = UUID.randomUUID().toString();

        final String organisation = getPayload("stub-data/usersgroups.get-organisation-details.json")
                .replace("%ORGANISATION_ID%", organisationId);


        final String permission = getPayload("stub-data/usersgroups.get-permission-for-user-by-defendant.json")
                .replace("%USER_ID%", randomUUID().toString())
                .replace("%DEFENDANT_ID%", defendantId)
                .replace("%ORGANISATION_ID%", randomUUID().toString());

        stubUserGroupOrganisation(CHAMBER_USER_ID, organisation);
        stubUserGroupDefenceClientPermission(defendantId, permission);


        stubQueryDocumentTypeData("/restResource/ref-data-document-type-for-defence.json");
        ReferenceDataStub.stubQueryDocumentTypeData("/restResource/ref-data-document-type-for-defence.json", docTypeId);


       addCourtDocumentForDefence(CHAMBER_USER_ID, docId, defendantId,
                prepareAddCourtDocumentWithDocTypePayload(docTypeId, docId, caseId, defendantId, "progression.add-court-document-def-level.json"), HttpStatus.SC_FORBIDDEN);
        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");


    }


    @Test
    public void shouldAddDocumentIfTheLoggedInUserIsGranted() throws IOException {
        final String caseId = UUID.randomUUID().toString();
        final String docId = UUID.randomUUID().toString();
        final String defendantId = UUID.randomUUID().toString();
        final String docTypeId = UUID.randomUUID().toString();
        final String organisationId = UUID.randomUUID().toString();

        final String organisation = getPayload("stub-data/usersgroups.get-organisation-details.json")
                .replace("%ORGANISATION_ID%", organisationId);


        final String permission = getPayload("stub-data/usersgroups.get-permission-for-user-by-defendant.json")
                .replace("%USER_ID%", CHAMBER_USER_ID)
                .replace("%DEFENDANT_ID%", defendantId)
                .replace("%ORGANISATION_ID%", organisationId);

        stubUserGroupOrganisation(CHAMBER_USER_ID, organisation);
        stubUserGroupDefenceClientPermission(defendantId, permission);


        stubQueryDocumentTypeData("/restResource/ref-data-document-type-for-defence.json");
        ReferenceDataStub.stubQueryDocumentTypeData("/restResource/ref-data-document-type-for-defence.json", docTypeId);


        addCourtDocumentForDefence(CHAMBER_USER_ID, docId, defendantId,
                prepareAddCourtDocumentWithDocTypePayload(docTypeId, docId, caseId, defendantId, "progression.add-court-document-def-level.json"), HttpStatus.SC_ACCEPTED);
        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");


    }

    private String prepareAddCourtDocumentPayload() {
        String body = getPayload("progression.add-court-document.json");
        body = body.replaceAll("%RANDOM_DOCUMENT_ID%", docId.toString())
                .replaceAll("%RANDOM_CASE_ID%", caseId.toString())
                .replaceAll("%RANDOM_DEFENDANT_ID%", defendantId.toString());
        return body;
    }

    private String prepareUpdateCourtDocumentPayload() {

        String body = getPayload("progression.update-court-document.json");
        body = body.replaceAll("%RANDOM_DOCUMENT_ID%", docId.toString())
                .replaceAll("%CASE_ID%", caseId)
                .replaceAll("%DEFENDENT-ID2%", updatedDefendantId);

        return body;
    }

    private CustomComparator getCustomComparator() {
        return new CustomComparator(STRICT,
                new Customization("courtDocument.materials[0].uploadDateTime", (o1, o2) -> true)
        );
    }


    private void verifyAddCourtDocument() throws IOException {
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
                withJsonPath("$.courtDocument.containsFinancialMeans", equalTo(true)))
        );

        final String expectedPayload = getPayload("expected/expected.progression.add-court-document.json")
                .replace("COURT-DOCUMENT-ID", docId.toString())
                .replace("DEFENDENT-ID", defendantId.toString())
                .replace("CASE-ID", caseId.toString());

        assertEquals(expectedPayload, actualDocument, getCustomComparator());
    }


    private void verifyUpdateFailedEvent() {
        final JsonPath jsonResponse = retrieveMessage(PRIVATE_MESSAGE_CONSUMER);
        assertThat(jsonResponse, is(notNullValue()));
        assertThat(jsonResponse.getString("courtDocumentId"), is(docId.toString()));
        assertThat(jsonResponse.getString("failureReason"), is("Update document is not supported for this Document Category Test level"));
    }

    private void verifyUpdateFailedEventForRemovedDocument() {
        final JsonPath jsonResponse = retrieveMessage(PRIVATE_MESSAGE_CONSUMER);
        assertThat(jsonResponse, is(notNullValue()));
        assertThat(jsonResponse.getString("courtDocumentId"), is(docId.toString()));
        assertThat(jsonResponse.getString("failureReason"), is(format("Document is deleted. Could not update the given court document id: %s", docId)));
    }


    public void verifyInPublicTopic() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(consumerForCourtDocumentUpdated);

        assertThat(message, notNullValue());
    }

    private String addCourtDocumentForDefence(final String userId, final String docId, final String defendantId, final String body, final int status) throws IOException {

        final Response writeResponse = postCommandWithUserId(getWriteUrl(format("/defendant/%s/courtdocument/%s", defendantId, docId)),
                "application/vnd.progression.add-court-document-for-defence+json",
                body ,userId);
        assertThat(writeResponse.getStatusCode(), equalTo(status));

        if(status == 202) {
            return getCourtDocumentFor(docId, allOf(
                    withJsonPath("$.courtDocument.courtDocumentId", equalTo(docId)),
                    withJsonPath("$.courtDocument.containsFinancialMeans", equalTo(true))
            ));
        }
        return null;
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
