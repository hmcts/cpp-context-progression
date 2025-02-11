package uk.gov.moj.cpp.progression;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.EventSelector.PUBLIC_EVENT_COURT_DOCUMENT_UPADTED;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentsByCase;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedingsWithoutCourtDocument;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedingsWithoutCourtDocumentAndCpsOrganisation;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.verifyCasesForSearchCriteria;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.verifyQueryResultsForbidden;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommandWithUserId;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubGetDocumentsTypeAccess;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryCpsProsecutorData;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryDocumentTypeData;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.setupAsAuthorisedUser;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.stubUserGroupDefenceClientPermission;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.stubUserGroupOrganisation;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.progression.util.CaseProsecutorUpdateHelper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.io.Resources;
import com.jayway.jsonpath.ReadContext;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.hamcrest.core.StringContains;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

@SuppressWarnings({"squid:S1607"})
public class AddCourtDocumentIT extends AbstractIT {

    private static final String USER_ID = "07e9cd55-0eff-4eb3-961f-0d83e259e415";
    public static final String ACCESS_CONTROL_FAILED = "Access Control failed for json envelope";
    public static final String USER_GROUP_NOT_PRESENT_DROOL = randomUUID().toString();
    public static final String USER_GROUP_NOT_PRESENT_RBAC = randomUUID().toString();
    public static final String CHAMBER_USER_ID = randomUUID().toString();
    private final JmsMessageConsumerClient publicEventConsumerForCourtDocumentUpdated = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_EVENT_COURT_DOCUMENT_UPADTED).getMessageConsumerClient();
    private String caseId;
    private String docId;
    private String defendantId;
    private String updatedDefendantId;
    private CaseProsecutorUpdateHelper caseProsecutorUpdateHelper;

    @BeforeAll
    public static void init() {
        setupAsAuthorisedUser(UUID.fromString(USER_GROUP_NOT_PRESENT_DROOL), "stub-data/usersgroups.get-invalid-groups-by-user.json");
        setupAsAuthorisedUser(UUID.fromString(USER_GROUP_NOT_PRESENT_RBAC), "stub-data/usersgroups.get-invalid-rbac-groups-by-user.json");
        setupAsAuthorisedUser(fromString(CHAMBER_USER_ID), "stub-data/usersgroups.get-chamber-groups-by-user.json");
        setupAsAuthorisedUser(fromString(USER_ID), "stub-data/usersgroups.get-specific-groups-by-user.json");
    }


    @BeforeEach
    public void setup() throws JSONException {
        caseId = randomUUID().toString();
        docId = randomUUID().toString();
        defendantId = randomUUID().toString();
        updatedDefendantId = randomUUID().toString();
        caseProsecutorUpdateHelper = new CaseProsecutorUpdateHelper(caseId);
    }

    @Test
    public void shouldAddCourtDocumentAndThenUpdateIt() throws IOException, JSONException {
        //Given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.id", is(caseId)))));

        addCourtDocumentAndVerify(null, "460f7ec0-c002-11e8-a355-529269fb1459");

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
    public void shouldAddCourtDocumentAndForbidToQueryWhenNoRbacMatches() throws IOException, JSONException {

        //Given
        final String body = prepareAddCourtDocumentPayload(null);
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

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
    public void shouldAddCourtDocumentV2AndForbidToQueryWhenNoRbacMatches() throws IOException, JSONException {

        //Given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        final String body = prepareAddCourtDocumentPayloadV2();
        //When
        final Response writeResponse = postCommand(getWriteUrl("/courtdocument/" + docId),
                "application/vnd.progression.add-court-document-v2+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        //Group Not Present in Drool rule
        verifyQueryResultsForbidden(docId, USER_GROUP_NOT_PRESENT_DROOL, withJsonPath("$.error", StringContains.containsString(ACCESS_CONTROL_FAILED)));

        //Group Not Present in RBAC
        //Then
        verifyQueryResultsForbidden(docId, USER_GROUP_NOT_PRESENT_RBAC, withJsonPath("$.error", StringContains.containsString(ACCESS_CONTROL_FAILED)));
    }

    @Test
    public void shouldGetForbiddenExceptionWhenAddCourtDocumentAndNoRBACRulesMatches() throws IOException, JSONException {

        //Given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        final String body = prepareAddCourtDocumentPayload(null);
        //When
        //postCommand()
        final Response writeResponse = postCommandWithUserId(getWriteUrl("/courtdocument/" + docId),
                "application/vnd.progression.add-court-document+json",
                body, USER_GROUP_NOT_PRESENT_RBAC);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_FORBIDDEN));
    }

    @Test
    public void shouldAddDocumentIfTheLoggedInUserIsGranted() throws IOException, JSONException {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        final String docTypeId = randomUUID().toString();
        final String organisationId = randomUUID().toString();

        final String organisation = getPayload("stub-data/usersgroups.get-organisation-details.json")
                .replace("%ORGANISATION_ID%", organisationId);

        final String permission = getPayload("stub-data/usersgroups.get-permission-for-user-by-defendant.json")
                .replace("%USER_ID%", CHAMBER_USER_ID)
                .replace("%DEFENDANT_ID%", defendantId)
                .replace("%ORGANISATION_ID%", organisationId);

        stubUserGroupOrganisation(CHAMBER_USER_ID, organisation);
        stubUserGroupDefenceClientPermission(permission);

        stubQueryDocumentTypeData("/restResource/ref-data-document-type-for-defence.json");

        addCourtDocumentForDefence(CHAMBER_USER_ID, docId, defendantId,
                prepareAddCourtDocumentWithDocTypePayload(docTypeId, docId, caseId, defendantId, "progression.add-court-document-def-level.json"), HttpStatus.SC_ACCEPTED);
        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");
    }

    @Test
    public void shouldUpdateCPSCaseAddDefendantCourtDocumentWithIsCpsCase() throws IOException, JSONException {
        stubQueryCpsProsecutorData("/restResource/referencedata.query.prosecutor.by.oucode.json", randomUUID(), HttpStatus.SC_OK);
        initiateCourtProceedingsWithoutCourtDocument(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        addCourtDocumentAndVerify(true, "0bb7b276-9dc0-4af2-83b9-f4acef0c7898");

        List<Matcher<? super ReadContext>> matchers = newArrayList(
                withJsonPath("$.prosecutionCase.prosecutor.address.address1", is("6th Floor Windsor House")),
                withJsonPath("$.prosecutionCase.prosecutor.address.address2", is("42-50 Victoria Street")),
                withJsonPath("$.prosecutionCase.prosecutor.address.address3", is("London")),
                withJsonPath("$.prosecutionCase.prosecutor.address.postcode", is("SW1H 0TL")),
                withJsonPath("$.prosecutionCase.prosecutor.isCps", is(true)),
                withJsonPath("$.prosecutionCase.prosecutor.prosecutorCode", is("TFL")),
                withJsonPath("$.prosecutionCase.prosecutor.prosecutorId", is("2daefec3-2f76-8109-82d9-2e60544a6c02")),
                withJsonPath("$.prosecutionCase.prosecutor.prosecutorName", is("Transport for London")),
                withJsonPath("$.prosecutionCase.isCpsOrgVerifyError", is(false))
        );

        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId, matchers));
    }

    @Test
    public void shouldNotUpdateCPSCaseAddDefendantCourtDocumentWithoutIsCpsCase() throws IOException, JSONException {
        initiateCourtProceedingsWithoutCourtDocument(caseId, defendantId);

        addCourtDocumentAndVerify(false, "0bb7b276-9dc0-4af2-83b9-f4acef0c7898");

        List<Matcher<? super ReadContext>> matchers = newArrayList(
                withoutJsonPath("$.prosecutionCase.isCpsOrgVerifyError"),
                withoutJsonPath("$.prosecutionCase.prosecutor"),
                withJsonPath("$.prosecutionCase.prosecutionCaseIdentifier.prosecutionAuthorityCode", is("TFL"))
        );

        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId, matchers));
    }

    @Test
    public void shouldUpdateCPSCaseAddDefendantCourtDocumentWithIsCpsCaseButCpsOrganisationIsNotInReferenceData() throws IOException, JSONException {
        stubQueryCpsProsecutorData("/restResource/referencedata.query.prosecutor.by.oucode.json", randomUUID(), HttpStatus.SC_NOT_FOUND);
        initiateCourtProceedingsWithoutCourtDocument(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        addCourtDocumentAndVerify(true, "0bb7b276-9dc0-4af2-83b9-f4acef0c7898");

        List<Matcher<? super ReadContext>> matchers = newArrayList(
                withJsonPath("$.prosecutionCase.isCpsOrgVerifyError", is(true))
        );

        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId, matchers));

    }

    @Test
    public void shouldNotUpdateCPSCaseAddDefendantCourtDocumentWithIsCpsCaseButGUIUpdateOnce() throws IOException, JSONException {
        stubQueryCpsProsecutorData("/restResource/referencedata.query.prosecutor.by.oucode.json", randomUUID(), HttpStatus.SC_NOT_FOUND);
        initiateCourtProceedingsWithoutCourtDocument("progression.command.initiate-court-proceedings-with-name.json", caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        addCourtDocumentAndVerify(true, "0bb7b276-9dc0-4af2-83b9-f4acef0c7898");

        List<Matcher<? super ReadContext>> matchers = newArrayList(
                withJsonPath("$.prosecutionCase.isCpsOrgVerifyError", is(true))
        );

        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId, matchers));

        caseProsecutorUpdateHelper.updateCaseProsecutor();

        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        addCourtDocumentAndVerify(true, "0bb7b276-9dc0-4af2-83b9-f4acef0c7898");

        verifyCasesForSearchCriteria("Billy", new Matcher[]{
                withJsonPath("$.searchResults.[*].defendantName", hasItem(containsString("Billy"))),
                withJsonPath("$.searchResults.[*].cpsProsecutor", hasItem(containsString("CPS-EM")))
        });
    }

    @Test
    public void shouldUpdateCPSCaseAddDefendantCourtDocumentWithIsCpsCaseButCpsOrganisationNotExist() throws IOException, JSONException {
        stubQueryCpsProsecutorData("/restResource/referencedata.query.prosecutor.by.oucode.json", randomUUID(), HttpStatus.SC_OK);
        initiateCourtProceedingsWithoutCourtDocumentAndCpsOrganisation(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        addCourtDocumentAndVerify(true, "0bb7b276-9dc0-4af2-83b9-f4acef0c7898");

        List<Matcher<? super ReadContext>> matchers = newArrayList(
                withJsonPath("$.prosecutionCase.isCpsOrgVerifyError", is(true))
        );

        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId, matchers));
    }

    private String prepareAddCourtDocumentPayload(Boolean isCpsCase) {
        String body;
        if (isCpsCase == null) {
            body = getPayload("progression.add-court-document.json");
        } else {
            body = getPayload("progression.add-court-document-with-cpscase.json");
        }
        body = body.replaceAll("%RANDOM_DOCUMENT_ID%", docId)
                .replaceAll("%RANDOM_CASE_ID%", caseId)
                .replaceAll("%RANDOM_DEFENDANT_ID1%", defendantId);
        if (isCpsCase != null && isCpsCase == true) {
            body = body.replaceAll("\"isCpsCase\": false", "\"isCpsCase\": true");
        }
        return body;
    }

    private String prepareAddCourtDocumentPayloadV2() {
        String body = getPayload("progression.add-court-document-v2.json");
        body = body.replaceAll("%RANDOM_DOCUMENT_ID%", docId)
                .replaceAll("%RANDOM_CASE_ID%", caseId)
                .replaceAll("%RANDOM_DEFENDANT_ID1%", defendantId);
        return body;
    }

    private String prepareUpdateCourtDocumentPayload() {

        String body = getPayload("progression.update-court-document.json");
        body = body.replaceAll("%RANDOM_DOCUMENT_ID%", docId)
                .replaceAll("%CASE_ID%", caseId)
                .replaceAll("%DEFENDENT-ID2%", updatedDefendantId);

        return body;
    }

    private CustomComparator getCustomComparator() {
        return new CustomComparator(STRICT,
                new Customization("courtDocument.materials[0].uploadDateTime", (o1, o2) -> true)
        );
    }

    private void addCourtDocumentAndVerify(Boolean isCpsCase, final String documentTypeId) throws IOException, JSONException {
        //Given
        final String body = prepareAddCourtDocumentPayload(isCpsCase);
        //When
        final Response writeResponse = postCommand(getWriteUrl("/courtdocument/" + docId),
                "application/vnd.progression.add-court-document+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        //Then
        final String actualDocument = getCourtDocumentFor(docId, allOf(
                withJsonPath("$.courtDocument.courtDocumentId", equalTo(docId)),
                withJsonPath("$.courtDocument.containsFinancialMeans", equalTo(true)),
                withJsonPath("$.courtDocument.sendToCps", equalTo(true)))
        );

        final String expectedPayload = getPayload("expected/expected.progression.add-court-document.json")
                .replace("COURT-DOCUMENT-ID", docId)
                .replace("DEFENDENT-ID", defendantId)
                .replace("DOCUMENT-TYPE-ID", documentTypeId)
                .replace("CASE-ID", caseId);

        assertEquals(expectedPayload, actualDocument, getCustomComparator());
    }

    private void verifyInPublicTopic() {
        final Optional<JsonObject> message = retrieveMessageBody(publicEventConsumerForCourtDocumentUpdated);
        assertThat(message, notNullValue());
    }

    private String addCourtDocumentForDefence(final String userId, final String docId, final String defendantId, final String body, final int status) {

        final Response writeResponse = postCommandWithUserId(getWriteUrl(format("/defendant/%s/courtdocument/%s", defendantId, docId)),
                "application/vnd.progression.add-court-document-for-defence+json",
                body, userId);
        assertThat(writeResponse.getStatusCode(), equalTo(status));

        if (status == 202) {
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
        body = body.replaceAll("%RANDOM_DOCUMENT_ID%", docId)
                .replaceAll("%RANDOM_CASE_ID%", caseId)
                .replaceAll("%RANDOM_DEFENDANT_ID%", defendantId)
                .replaceAll("%RANDOM_DOC_TYPE%", docTypeId);
        return body;
    }

    @Test
    public void shouldUpdateSendToCpsToViewStore() throws IOException, JSONException {
        stubFor(post(urlPathEqualTo("/notification-cms/v1/transformAndSendCms"))
                .willReturn(aResponse().withStatus(SC_OK)));

        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");

        initiateCourtProceedingsWithoutCourtDocument(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        //Given
        addCourtDocumentAndVerify(null, "460f7ec0-c002-11e8-a355-529269fb1459");

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

        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");
        getCourtDocumentsByCase(USER_ID, caseId);
    }
}
