package uk.gov.moj.cpp.progression.helper;

import com.google.common.io.Resources;
import com.jayway.restassured.response.Response;

import uk.gov.moj.cpp.progression.helper.CourtApplicationsHelper.CourtApplicationRandomValues;

import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.json.JSONObject;

import javax.json.Json;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.UUID;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.join;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getCommand;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getCommandUri;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getMaterialContentResponse;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getQueryUri;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;


public class PreAndPostConditionHelper {

    private static final String CROWN_COURT_EXTRACT = "CrownCourtExtract";

    public static String addDefendant(final String caseId) {
        String request = null;
        try (AddDefendantHelper addDefendantHelper = new AddDefendantHelper(caseId)) {
            request = addDefendantHelper.addMinimalDefendant();
            addDefendantHelper.verifyInActiveMQ();
            addDefendantHelper.verifyInPublicTopic();
            addDefendantHelper.verifyMinimalDefendantAdded();
        }
        return request;
    }

    public static void addDefendant(final String caseId, final String defendantId) {
        try (AddDefendantHelper addDefendantHelper = new AddDefendantHelper(caseId)) {
            addDefendantHelper.addFullDefendant(defendantId, randomUUID().toString().substring(0, 11));
        }
    }

    public static Response addCaseToCrownCourt(final String caseId) throws IOException {
        return addCaseToCrownCourt(caseId, randomUUID().toString(), randomUUID().toString());
    }

    public static Response addProsecutionCaseToCrownCourt(final String caseId, final String defendantId, final String materialIdOne,
                                                          final String materialIdTwo, final String courtDocumentId, final String referralId) throws IOException {
        return postCommand(getCommandUri("/refertocourt"),
                "application/vnd.progression.refer-cases-to-court+json",
                getReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId, materialIdOne, materialIdTwo, courtDocumentId, referralId, generateUrn()));
    }

    public static Response addRemoveCourtDocument(final String courtDocumentId, final String materialId, final boolean isRemoved) throws IOException {
        return postCommand(getCommandUri(String.format("/courtdocument/%s/material/%s", courtDocumentId, materialId)),
                "application/vnd.progression.remove-court-document+json",
                Json.createObjectBuilder().add("isRemoved", isRemoved).build().toString());
    }

    public static Response addProsecutionCaseToCrownCourt(final String caseId, final String defendantId) throws IOException {
        final JSONObject jsonPayload = new JSONObject(getReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId, randomUUID().toString(),
                randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), generateUrn()));
        jsonPayload.getJSONObject("courtReferral").remove("courtDocuments");
        return postCommand(getCommandUri("/refertocourt"),
                "application/vnd.progression.refer-cases-to-court+json",
                jsonPayload.toString());
    }

    public static Response initiateCourtProceedings(final String caseId, final String defendantId, final String materialIdOne,
                                                    final String materialIdTwo,
                                                    final String courtDocumentId, final String referralId,
                                                    final String listedStartDateTime, final String earliestStartDateTime, final String dob) throws IOException {
        return postCommand(getCommandUri("/initiatecourtproceedings"),
                "application/vnd.progression.initiate-court-proceedings+json",
                getInitiateCourtProceedingsJsonBody(caseId, defendantId, materialIdOne, materialIdTwo, courtDocumentId, referralId, generateUrn(), listedStartDateTime, earliestStartDateTime, dob));

    }

    public static Response initiateCourtProceedingsWithoutCourtDocument(final String caseId, final String defendantId,
                                                                        final String listedStartDateTime, final String earliestStartDateTime, final String dob) throws IOException {
        final JSONObject jsonPayload = new JSONObject(getInitiateCourtProceedingsJsonBody(caseId, defendantId, randomUUID().toString(),
                randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), generateUrn(), listedStartDateTime, earliestStartDateTime, dob));
        jsonPayload.getJSONObject("initiateCourtProceedings").remove("courtDocuments");
        return postCommand(getCommandUri("/initiatecourtproceedings"),
                "application/vnd.progression.initiate-court-proceedings+json", jsonPayload.toString());
    }

    public static Response addProsecutionCaseToCrownCourtWithMinimumAttributes(final String caseId, final String defendantId) throws IOException {
        final JSONObject jsonPayload = new JSONObject(getReferProsecutionCaseToCrownCourtWithMinimumAttribute(caseId, defendantId, generateUrn()));
        jsonPayload.getJSONObject("courtReferral").remove("courtDocuments");
        return postCommand(getCommandUri("/refertocourt"),
                "application/vnd.progression.refer-cases-to-court+json",
                jsonPayload.toString());
    }

    public static Response addProsecutionCaseWithUrn(final String caseId, final String defendantId, final String urn) throws IOException {
        final JSONObject jsonPayload = new JSONObject(getReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId, randomUUID().toString(),
                randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), urn));
        jsonPayload.getJSONObject("courtReferral").remove("courtDocuments");
        return postCommand(getCommandUri("/refertocourt"),
                "application/vnd.progression.refer-cases-to-court+json",
                jsonPayload.toString());
    }


    public static Response addCaseToCrownCourt(final String caseId, final String firstDefendantId, final String secondDefendantId) throws IOException {
        return postCommand(getCommandUri("/cases/" + caseId),
                "application/vnd.progression.command.add-case-to-crown-court+json",
                getAddCaseToCrownCourtJsonBody(caseId, firstDefendantId, secondDefendantId));
    }

    public static Response getDefendants(final String caseId) throws IOException {
        return getCommand(getQueryUri("/cases/" + caseId + "/defendants"), "application/vnd.progression.query.defendants+json");
    }

    public static Response getMagistrateCourts() throws IOException {
        return getCommand(getQueryUri("/crown-court/LCC/magistrate-courts"),
                "application/vnd.progression.query.crown-court.magistrate-courts+json");
    }

    public static Response getCaseProgression(final String uri, final String mediaType) throws IOException {
        return getCommand(uri, mediaType);
    }


    private static String getAddCaseToCrownCourtJsonBody(final String caseId, final String firstDefendantId, final String secondDefendantId) throws IOException {
        return Resources.toString(Resources.getResource("progression.command.add-case-to-crown-court.json"), Charset.defaultCharset())
                .replace("RANDOM_CASE_ID", caseId)
                .replace("DEF_ID_1", firstDefendantId)
                .replace("DEF_ID_2", secondDefendantId)
                .replace("TODAY", LocalDate.now().toString());
    }

    private static String getReferProsecutionCaseToCrownCourtJsonBody(final String caseId, final String defendantId, final String materialIdOne,
                                                                      final String materialIdTwo, final String courtDocumentId, final String referralId, final String caseUrn) throws IOException {
        return Resources.toString(Resources.getResource("progression.command.prosecution-case-refer-to-court.json"), Charset.defaultCharset())
                .replace("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_REFERENCE", caseUrn)
                .replace("RANDOM_DEFENDANT_ID", defendantId)
                .replace("RANDOM_DOC_ID", courtDocumentId)
                .replace("RANDOM_MATERIAL_ID_ONE", materialIdOne)
                .replace("RANDOM_MATERIAL_ID_TWO", materialIdTwo)
                .replace("RANDOM_REFERRAL_ID", referralId);
    }

    private static String getInitiateCourtProceedingsJsonBody(final String caseId, final String defendantId, final String materialIdOne,
                                                              final String materialIdTwo, final String courtDocumentId,
                                                              final String referralId, final String caseUrn,
                                                              final String listedStartDateTime, final String earliestStartDateTime,
                                                              final String dob) throws IOException {
        return Resources.toString(Resources.getResource("progression.command.initiate-court-proceedings.json"), Charset.defaultCharset())
                .replace("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_REFERENCE", caseUrn)
                .replace("RANDOM_DEFENDANT_ID", defendantId)
                .replace("RANDOM_DOC_ID", courtDocumentId)
                .replace("RANDOM_MATERIAL_ID_ONE", materialIdOne)
                .replace("RANDOM_MATERIAL_ID_TWO", materialIdTwo)
                .replace("RANDOM_REFERRAL_ID", referralId)
                .replace("LISTED_START_DATE_TIME", listedStartDateTime)
                .replace("EARLIEST_START_DATE_TIME", earliestStartDateTime)
                .replace("DOB", dob);

    }

    private static String getReferProsecutionCaseToCrownCourtWithMinimumAttribute(final String caseId, final String defendantId, final String caseUrn) throws IOException {
        return Resources.toString(Resources.getResource("progression.command.prosecution-case-refer-to-court-minimal-payload.json"), Charset.defaultCharset())
                .replace("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_REFERENCE", caseUrn)
                .replace("RANDOM_DEFENDANT_ID", defendantId);
    }


    // Progression Test DSL for preconditions and assertions
    public static void givenCaseAddedToCrownCourt(final String caseId, final String firstDefendantId, final String secondDefendantId) throws IOException {
        final Response writeResponse = addCaseToCrownCourt(caseId, firstDefendantId, secondDefendantId);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
    }

    public static void givenCaseAddedToCrownCourt(final String caseId) throws IOException {
        givenCaseAddedToCrownCourt(caseId, randomUUID().toString(), randomUUID().toString());
    }

    public static void givenCaseProgressionDetail(final String caseId) {
        pollForResponse(join("", "/cases/", caseId), "application/vnd.progression.query.caseprogressiondetail+json");
    }

    public static String getCaseProgressionFor(final String caseId) {
        return pollForResponse(join("", "/cases/", caseId), "application/vnd.progression.query.caseprogressiondetail+json");
    }

    public static String getProsecutioncasesProgressionFor(final String caseId) {
        return getProsecutioncasesProgressionFor(caseId, new Matcher[]{withJsonPath("$.prosecutionCase.id", equalTo(caseId))});

    }

    public static String getProsecutioncasesProgressionFor(final String caseId, final Matcher[] matchers) {
        return poll(requestParams(getQueryUri("/prosecutioncases/" + caseId), "application/vnd.progression.query.prosecutioncase+json").withHeader(USER_ID, UUID.randomUUID()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                matchers
                        ))).getPayload();
    }

    public static String getApplicationFor(final String applicationId) {
        return pollForResponse(join("", "/applications/", applicationId), "application/vnd.progression.query.application+json");
    }

    public static void verifyCasesForSearchCriteria(final String searchCriteria, final Matcher[] matchers) {
        poll(requestParams(getQueryUri(join("", "/search?q=", searchCriteria)), "application/vnd.progression.query.search-cases+json").withHeader(USER_ID, UUID.randomUUID()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                matchers
                        ))).getPayload();
    }

    public static String getUsergroupsByMaterialId(final String materialId) {
        return pollForResponse(join("", "/search?q=", materialId), "application/vnd.progression.query.usergroups-by-material-id+json");
    }

    public static String getMaterialMetadata(final String materialId) {
        return pollForResponse("/progression-query-api/query/api/rest/progression", join("", "/material/", materialId, "/metadata"), "application/vnd.progression.query.material-metadata+json");
    }

    public static javax.ws.rs.core.Response getMaterialContent(final UUID materialId, final UUID userId) {
        return getMaterialContentResponse("/material/" + materialId.toString() + "/content", userId, "application/vnd.progression.query.material-content+json");

    }

    public static String getDefendantRequestFor(final String defendantId) {
        return pollForResponse(join("", "/defendant-requests/", defendantId), "application/vnd.progression.query.defendant-request+json");
    }

    public static String generateUrn() {
        return randomUUID().toString().replace("-", "").substring(0, 8);
    }

    public static String getCourtExtractPdf(final String caseId, final String defendantId, final String hearingId, final String extractType) {
        String queryParam = "";
        if (CROWN_COURT_EXTRACT.equals(extractType)) {
            queryParam = "?hearingIds=" + hearingId;
        }
        return pollForResponse(join("", "/prosecutioncases/", caseId, "/defendants/", defendantId, "/extract/", extractType, queryParam), "application/vnd.progression.query.court-extract+json");
    }

    public static String getApplicationExtractPdf(final String applicationId, final String hearingIds) {
        String queryParam = "?hearingIds=" + hearingIds;
        return pollForResponse(join("", "/applications/", applicationId, "/extract", queryParam), "application/vnd.progression.query.court-extract-application+json");
    }

    public static Response addCourtApplication(final String caseId, final String applicationId, final String fileName) throws IOException {
        return postCommand(getCommandUri("/application"),
                "application/vnd.progression.create-court-application+json",
                getCourtApplicationJsonBody(caseId, applicationId, generateUrn(), fileName));
    }

    public static Response updateCourtApplication(final String applicationId, final String applicantId, final String caseId, final String defendantId, final String fileName) throws IOException {
        return postCommand(getCommandUri("/application"),
                "application/vnd.progression.update-court-application+json",
                getUpdateCourtApplicationJsonBody(applicationId, applicantId, caseId, defendantId, fileName));
    }

    public static Response addStandaloneCourtApplication(final String applicationId, final String parentApplicationId, final CourtApplicationRandomValues randomValues, final String fileName) throws IOException {
        return postCommand(getCommandUri("/application"),
                "application/vnd.progression.create-court-application+json",
                getStandaloneCourtApplicationJsonBody(applicationId, parentApplicationId, generateUrn(), randomValues, fileName));
    }

    private static String getStandaloneCourtApplicationJsonBody(final String applicationId, final String parentApplicationId, final String applicationReference, CourtApplicationRandomValues randomValues, final String fileName) throws IOException {

        return Resources.toString(Resources.getResource(fileName), Charset.defaultCharset())
                .replace("RANDOM_APPLICATION_ID", applicationId)
                .replace("RANDOM_PARENT_APPLICATION_ID", parentApplicationId)
                .replace("RANDOM_REFERENCE", applicationReference)
                .replace("RANDOM_APPLICANT_FIRSTNAME", randomValues.APPLICANT_FIRSTNAME)
                .replace("RANDOM_APPLICANT_MIDDLENAME", randomValues.APPLICANT_MIDDLENAME)
                .replace("RANDOM_APPLICANT_LASTNAME", randomValues.APPLICANT_LASTNAME)
                .replace("RANDOM_RESPONDENT_FIRSTNAME", randomValues.RESPONDENT_FIRSTNAME)
                .replace("RANDOM_RESPONDENT_MIDDLENAME", randomValues.RESPONDENT_MIDDLENAME)
                .replace("RANDOM_RESPONDENT_LASTNAME", randomValues.RESPONDENT_LASTNAME)
                .replace("RANDOM_RESPONDENT_ORGANISATION_NAME", randomValues.RESPONDENT_ORGANISATION_NAME);
    }

    private static String getCourtApplicationJsonBody(final String caseId, final String applicationId, final String applicationReference, final String fileName) throws IOException {
        return Resources.toString(Resources.getResource(fileName), Charset.defaultCharset())
                .replace("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_APPLICATION_ID", applicationId)
                .replace("RANDOM_REFERENCE", applicationReference);
    }

    private static String getUpdateCourtApplicationJsonBody(final String applicationId, final String applicantId, final String caseId, final String defendantId, final String fileName) throws IOException {
        return Resources.toString(Resources.getResource(fileName), Charset.defaultCharset())
                .replace("APPLICATION_ID", applicationId)
                .replace("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_DEFENDANT_ID",defendantId)
                .replace("APPLICANT_ID", applicantId);
    }

    public static Response referCourtApplication(final String applicationId, final String hearingId, final String fileName) throws IOException {
        return postCommand(getCommandUri("/referapplicationtocourt"),
                "application/vnd.progression.refer-application-to-court+json",
                getReferApplicationToCourtJsonBody(applicationId, hearingId, fileName));

    }

    public static Response extendHearing(final String applicationId, final String hearingId, final String fileName) throws IOException {
        return postCommand(getCommandUri("/referapplicationtocourt"),
                "application/vnd.progression.extend-hearing+json",
                getExtendHearingJsonBody(applicationId, hearingId, fileName));

    }

    public static String getCourtDocumentFor(final String courtDocumentId) {
        return pollForResponse(join("", "/courtdocuments/", courtDocumentId), "application/vnd.progression.query.courtdocument+json");
    }

    private static String getReferApplicationToCourtJsonBody(final String applicationId, final String hearingId, final String fileName) throws IOException {
        return Resources.toString(Resources.getResource(fileName), Charset.defaultCharset())
                .replace("RANDOM_APPLICATION_ID", applicationId)
                .replace("RANDOM_HEARING_ID", hearingId);

    }

    private static String getExtendHearingJsonBody(final String applicationId, final String hearingId, final String fileName) throws IOException {
        return Resources.toString(Resources.getResource(fileName), Charset.defaultCharset())
                .replace("RANDOM_APPLICATION_ID", applicationId)
                .replace("RANDOM_HEARING_ID", hearingId);

    }
}
