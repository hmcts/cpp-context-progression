package uk.gov.moj.cpp.progression.helper;

import static com.google.common.io.Resources.getResource;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getMaterialContentResponse;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommandWithUserId;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.moj.cpp.progression.helper.CourtApplicationsHelper.CourtApplicationRandomValues;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.core.MultivaluedMap;

import com.google.common.io.Resources;
import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreAndPostConditionHelper {

    private static final String CROWN_COURT_EXTRACT = "CrownCourtExtract";
    private static final Logger LOGGER = LoggerFactory.getLogger(PreAndPostConditionHelper.class);


    public static Response addCaseToCrownCourt(final String caseId) throws IOException {
        return addCaseToCrownCourt(caseId, randomUUID().toString(), randomUUID().toString());
    }

    public static Response addProsecutionCaseToCrownCourtForIngestion(final String caseId, final String defendantId, final String materialIdOne,
                                                                      final String materialIdTwo, final String courtDocumentId, final String referralId,
                                                                      final String caseReference, final String commandPayload,final String initialOffenceId1,final String initialOffenceId2,final String initialOffenceId3,final String initialOffenceId4) throws IOException {
        return postCommand(getWriteUrl("/refertocourt"),
                "application/vnd.progression.refer-cases-to-court+json",
                createReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId, materialIdOne, materialIdTwo, courtDocumentId, referralId, caseReference, commandPayload,initialOffenceId1,initialOffenceId2,initialOffenceId3,initialOffenceId4
                ));
    }

    public static Response addProsecutionCaseToCrownCourtForIngestion(final String caseId, final String defendantId, final String materialIdOne,
                                                                      final String materialIdTwo, final String courtDocumentId, final String referralId,
                                                                      final String caseReference, final String commandPayload) throws IOException {
        return postCommand(getWriteUrl("/refertocourt"),
                "application/vnd.progression.refer-cases-to-court+json",
                createReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId, materialIdOne, materialIdTwo, courtDocumentId, referralId, caseReference, commandPayload));
    }



    public static Response addProsecutionCaseToCrownCourt(final String caseId, final String defendantId, final String materialIdOne,
                                                          final String materialIdTwo, final String courtDocumentId, final String referralId) throws IOException {
        return postCommand(getWriteUrl("/refertocourt"),
                "application/vnd.progression.refer-cases-to-court+json",
                createReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId, materialIdOne, materialIdTwo, courtDocumentId, referralId, generateUrn()));
    }

    public static Response addRemoveCourtDocument(final String courtDocumentId, final String materialId, final boolean isRemoved, final UUID userId) throws IOException {
        return postCommandWithUserId(getWriteUrl(String.format("/courtdocument/%s/material/%s", courtDocumentId, materialId)),
                "application/vnd.progression.remove-court-document+json",
                Json.createObjectBuilder().add("isRemoved", isRemoved).build().toString(), userId.toString());
    }

    public static Response recordLAAReference(final String caseId, final String defendantId, final String offenceId, final String statusCode) throws IOException {
        return postCommand(getWriteUrl(String.format("/laaReference/cases/%s/defendants/%s/offences/%s", caseId, defendantId, offenceId)),
                "application/vnd.progression.command.record-laareference-for-offence+json",
                getLAAReferenceForOffenceJsonBody(statusCode));
    }


    public static javax.ws.rs.core.Response recordLAAReferenceWithUserId(final String caseId, final String defendantId, final String offenceId, final String statusCode, final String statusDescription, final String userId) throws IOException {
        final RestClient restClient = new RestClient();
        final javax.ws.rs.core.Response response =
                restClient.postCommand(getWriteUrl(String.format("/laaReference/cases/%s/defendants/%s/offences/%s", caseId, defendantId, offenceId)),
                        "application/vnd.progression.command.record-laareference-for-offence+json",
                        getLAAReferenceForOffenceJsonBodyWithStatus(statusCode, statusDescription),
                        createHttpHeaders(userId));
        return response;
    }

    public static javax.ws.rs.core.Response receiveRepresentationOrder(final String caseId, final String defendantId, final String offenceId, final String statusCode, final String laaContractNumber, final String userId) throws IOException {
        final RestClient restClient = new RestClient();
        final javax.ws.rs.core.Response response =
                restClient.postCommand(getWriteUrl(String.format("/representationOrder/cases/%s/defendants/%s/offences/%s", caseId, defendantId, offenceId)),
                        "application/vnd.progression.command.receive-representationorder-for-defendant+json",
                        getReceiveRepresentationOrderJsonBody(statusCode, laaContractNumber),
                        createHttpHeaders(userId));
        return response;

    }

    public static MultivaluedMap<String, Object> createHttpHeaders(final String userId) {
        final MultivaluedMap<String, Object> headers = new MultivaluedMapImpl<>();
        headers.add(HeaderConstants.USER_ID, userId);
        return headers;
    }

    public static Response addProsecutionCaseToCrownCourt(final String caseId, final String defendantId) throws IOException {
        return addProsecutionCaseToCrownCourt(caseId, defendantId, generateUrn());
    }

    public static Response addProsecutionCaseToCrownCourtNullPostCode(final String caseId, final String defendantId) throws IOException {
        return addProsecutionCaseToCrownCourtNullPostCode(caseId, defendantId, generateUrn());
    }

    public static Response addProsecutionCaseToCrownCourt(final String caseId, final String defendantId, final String caseUrn) throws IOException {
        final JSONObject jsonPayload = new JSONObject(createReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId, randomUUID().toString(),
                randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), caseUrn));
        jsonPayload.getJSONObject("courtReferral").remove("courtDocuments");
        return postCommand(getWriteUrl("/refertocourt"),
                "application/vnd.progression.refer-cases-to-court+json",
                jsonPayload.toString());
    }

    public static Response updateDefendantListingStatusChanged(final String hearingId, final String filePath) throws IOException {
        final JSONObject jsonPayload = new JSONObject(createUpdateDefendantListingStatusJsonBody(hearingId, filePath));
        return postCommand(getWriteUrl("/hearing/" + hearingId),
                "application/vnd.progression.update-defendant-listing-status+json",
                jsonPayload.toString());
    }


    public static Response addProsecutionCaseToCrownCourtNullPostCode(final String caseId, final String defendantId, final String caseUrn) throws IOException {
        final JSONObject jsonPayload = new JSONObject(
                createReferProsecutionCaseToCrownCourtJsonBodyNullPostCode(
                        caseId,
                        defendantId,
                        randomUUID().toString(),
                        randomUUID().toString(),
                        randomUUID().toString(),
                        randomUUID().toString(),
                        caseUrn));
        jsonPayload.getJSONObject("courtReferral").remove("courtDocuments");
        return postCommand(getWriteUrl("/refertocourt"),
                "application/vnd.progression.refer-cases-to-court+json",
                jsonPayload.toString());
    }

    public static Response initiateCourtProceedingsForDefendantMatching(final String caseId,
                                                                        final String defendantId,
                                                                        final String masterDefendantId,
                                                                        final String materialIdOne,
                                                                        final String materialIdTwo,
                                                                        final String referralId,
                                                                        final String listedStartDateTime, final String earliestStartDateTime, final String dob) throws IOException {
        return postCommand(getWriteUrl("/initiatecourtproceedings"),
                "application/vnd.progression.initiate-court-proceedings+json",
                getInitiateCourtProceedingsJsonBodyForDefendantMatching(caseId, defendantId, masterDefendantId, materialIdOne, materialIdTwo, referralId, generateUrn(), listedStartDateTime, earliestStartDateTime, dob));

    }

    public static Response initiateCourtProceedingsForPartialOrExactMatchDefendants(final String caseId,
                                                                                    final String defendantId,
                                                                                    final String caseReceivedDate) throws IOException {
        return postCommand(getWriteUrl("/initiatecourtproceedings"),
                "application/vnd.progression.initiate-court-proceedings+json",
                getInitiateCourtProceedingsJsonBodyForPartialOrExactMatching(caseId, defendantId, caseReceivedDate));

    }

    public static Response initiateCourtProceedingsForExactMatchDefendants(final String caseId,
                                                                                    final String defendantId,
                                                                                    final String caseReceivedDate,
                                                                                    final String channel) throws IOException {
        return postCommand(getWriteUrl("/initiatecourtproceedings"),
                "application/vnd.progression.initiate-court-proceedings+json",
                getInitiateCourtProceedingsJsonBodyForExactMatching(caseId, defendantId, caseReceivedDate, channel));

    }

    public static Response initiateCourtProceedingsForPartialMatchDefendants(final String caseId,
                                                                           final String defendantId,
                                                                           final String caseReceivedDate,
                                                                           final String channel) throws IOException {
        return postCommand(getWriteUrl("/initiatecourtproceedings"),
                "application/vnd.progression.initiate-court-proceedings+json",
                getInitiateCourtProceedingsJsonBodyForPartialMatching(caseId, defendantId, caseReceivedDate, channel));

    }

    public static Response initiateCourtProceedings(final String caseId, final String defendantId, final String materialIdOne,
                                                    final String materialIdTwo,
                                                    final String referralId,
                                                    final String listedStartDateTime, final String earliestStartDateTime, final String dob) throws IOException {
        return postCommand(getWriteUrl("/initiatecourtproceedings"),
                "application/vnd.progression.initiate-court-proceedings+json",
                getInitiateCourtProceedingsJsonBody(caseId, defendantId, materialIdOne, materialIdTwo, referralId, generateUrn(), listedStartDateTime, earliestStartDateTime, dob));

    }

    public static Response initiateCourtProceedings(final String resourceLocation, final String caseId, final String defendantId, final String materialIdOne,
                                                    final String materialIdTwo, final String referralId,
                                                    final String caseUrn,
                                                    final String listedStartDateTime, final String earliestStartDateTime, final String dob) throws IOException {
        return postCommand(getWriteUrl("/initiatecourtproceedings"),
                "application/vnd.progression.initiate-court-proceedings+json",
                getInitiateCourtProceedingsJsonFromResource(resourceLocation, caseId, defendantId, materialIdOne, materialIdTwo, referralId, caseUrn, listedStartDateTime, earliestStartDateTime, dob));

    }

    public static Response initiateCourtProceedings(final String resourceLocation, final String caseId, final String defendantId, final String defendantId2, final String materialIdOne,
                                                    final String materialIdTwo, final String referralId,
                                                    final String caseUrn,
                                                    final String listedStartDateTime, final String earliestStartDateTime, final String dob) throws IOException {
        return postCommand(getWriteUrl("/initiatecourtproceedings"),
                "application/vnd.progression.initiate-court-proceedings+json",
                getInitiateCourtProceedingsJsonFromResource(resourceLocation, caseId, defendantId, defendantId2, materialIdOne, materialIdTwo, referralId, caseUrn, listedStartDateTime, earliestStartDateTime, dob));

    }

    public static Response initiateCourtProceedings(final String resourceLocation, final String caseId, final String defendantId, final String materialIdOne,
                                                    final String materialIdTwo, final String referralId,
                                                    final String listedStartDateTime, final String earliestStartDateTime, final String dob) throws IOException {

        return postCommand(getWriteUrl("/initiatecourtproceedings"),
                "application/vnd.progression.initiate-court-proceedings+json",
                getInitiateCourtProceedingsJsonFromResource(resourceLocation, caseId, defendantId, materialIdOne, materialIdTwo, referralId, generateUrn(), listedStartDateTime, earliestStartDateTime, dob));

    }

    public static Response initiateCourtProceedingsWithoutCourtDocument(final String caseId, final String defendantId,
                                                                        final String listedStartDateTime, final String earliestStartDateTime, final String dob) throws IOException {
        final JSONObject jsonPayload = new JSONObject(getInitiateCourtProceedingsJsonBody(caseId, defendantId, randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), generateUrn(), listedStartDateTime, earliestStartDateTime, dob));
        jsonPayload.getJSONObject("initiateCourtProceedings").remove("courtDocuments");
        return postCommand(getWriteUrl("/initiatecourtproceedings"),
                "application/vnd.progression.initiate-court-proceedings+json", jsonPayload.toString());
    }

    public static Response addProsecutionCaseToCrownCourtWithMinimumAttributes(final String caseId, final String defendantId) throws IOException {
        final JSONObject jsonPayload = new JSONObject(getReferProsecutionCaseToCrownCourtWithMinimumAttribute(caseId, defendantId, generateUrn()));
        jsonPayload.getJSONObject("courtReferral").remove("courtDocuments");
        return postCommand(getWriteUrl("/refertocourt"),
                "application/vnd.progression.refer-cases-to-court+json",
                jsonPayload.toString());
    }

    public static Response addProsecutionCaseWithUrn(final String caseId, final String defendantId, final String urn) throws IOException {
        final JSONObject jsonPayload = new JSONObject(createReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId, randomUUID().toString(),
                randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), urn));
        jsonPayload.getJSONObject("courtReferral").remove("courtDocuments");
        return postCommand(getWriteUrl("/refertocourt"),
                "application/vnd.progression.refer-cases-to-court+json",
                jsonPayload.toString());
    }

    public static Response addCaseToCrownCourt(final String caseId, final String firstDefendantId, final String secondDefendantId) throws IOException {
        return postCommand(getWriteUrl("/cases/" + caseId),
                "application/vnd.progression.command.add-case-to-crown-court+json",
                getAddCaseToCrownCourtJsonBody(caseId, firstDefendantId, secondDefendantId));
    }

    public static Response matchDefendant(final String prosecutionCaseId_2, final String defendantId_2, final String prosecutionCaseId_1, final String defendantId_1, final String masterDefendantId) throws IOException {
        return postCommand(getWriteUrl(String.format("/prosecutioncases/%s/defendants/%s/match", prosecutionCaseId_2, defendantId_2)),
                "application/vnd.progression.match-defendant+json",
                getDefendantPartialMatchJsonBody(prosecutionCaseId_1, defendantId_1, masterDefendantId));

    }

    public static Response unmatchDefendant(final String prosecutionCaseId_2, final String defendantId_2, final String prosecutionCaseId_1, final String defendantId_1, final String masterDefendantId) throws IOException {
        return postCommand(getWriteUrl(String.format("/prosecutioncases/%s/defendants/%s/match", prosecutionCaseId_2, defendantId_2)),
                "application/vnd.progression.unmatch-defendant+json",
                getDefendantUnmatchJsonBody(prosecutionCaseId_1, defendantId_1, masterDefendantId));

    }

    private static String getDefendantPartialMatchJsonBody(final String matchedProsecutionCaseId, final String matchedDefendantId, final String matchedMasterDefendantId) throws IOException {
        return Resources.toString(Resources.getResource("progression.match-defendant.json"), Charset.defaultCharset())
                .replace("PROSECUTION_CASE_ID", matchedProsecutionCaseId)
                .replace("DEFENDANT_ID", matchedDefendantId)
                .replace("MASTER_DEF_ID", matchedMasterDefendantId);
    }

    private static String getDefendantUnmatchJsonBody(final String matchedProsecutionCaseId, final String matchedDefendantId, final String matchedDefendantId2) throws IOException {
        return Resources.toString(Resources.getResource("progression.unmatch-defendant.json"), Charset.defaultCharset())
                .replace("PROSECUTION_CASE_ID_1", matchedProsecutionCaseId)
                .replace("DEFENDANT_ID_1", matchedDefendantId);
    }

    public static Response addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(final String caseId, final String defendantId) throws IOException {
        final JSONObject jsonPayload = new JSONObject(createReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId, randomUUID().toString(),
                randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), generateUrn(), "progression.command.prosecution-case-refer-to-court-one-defendant-two-offences.json"));
        jsonPayload.getJSONObject("courtReferral").remove("courtDocuments");
        return postCommand(getWriteUrl("/refertocourt"),
                "application/vnd.progression.refer-cases-to-court+json",
                jsonPayload.toString());
    }

    public static Response addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants(final String caseId, final String defendantId1, final String defendantId2) throws IOException {
        final JSONObject jsonPayload = new JSONObject(createReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId1, defendantId2, randomUUID().toString(),
                randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), generateUrn(), "progression.command.prosecution-case-refer-to-court-one-case-two-defendants.json"));
        jsonPayload.getJSONObject("courtReferral").remove("courtDocuments");
        return postCommand(getWriteUrl("/refertocourt"),
                "application/vnd.progression.refer-cases-to-court+json",
                jsonPayload.toString());
    }

    public static Response addProsecutionCaseToCrownCourtWithTwoProsecutionCases(final String caseId1, final String caseId2, final String defendantId1, final String defendantId2) throws IOException {
        final JSONObject jsonPayload = new JSONObject(createReferProsecutionCaseToCrownCourtJsonBody(caseId1, caseId2, defendantId1, defendantId2, randomUUID().toString(),
                randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), generateUrn(), "progression.command.prosecution-case-refer-to-court-two-cases-one-defendant.json"));
        jsonPayload.getJSONObject("courtReferral").remove("courtDocuments");
        return postCommand(getWriteUrl("/refertocourt"),
                "application/vnd.progression.refer-cases-to-court+json",
                jsonPayload.toString());
    }

    private static String getAddCaseToCrownCourtJsonBody(final String caseId, final String firstDefendantId, final String secondDefendantId) throws IOException {
        return Resources.toString(getResource("progression.command.add-case-to-crown-court.json"), Charset.defaultCharset())
                .replace("RANDOM_CASE_ID", caseId)
                .replace("DEF_ID_1", firstDefendantId)
                .replace("DEF_ID_2", secondDefendantId)
                .replace("TODAY", LocalDate.now().toString());
    }

    public static String createReferProsecutionCaseToCrownCourtJsonBody(final String caseId, final String defendantId1, final String defendantId2,
                                                                        final String materialIdOne, final String materialIdTwo, final String courtDocumentId,
                                                                        final String referralId, final String caseUrn, final String filePath) throws IOException {
        final URL resource = getResource(filePath);
        return Resources.toString(resource, Charset.defaultCharset())
                .replace("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_REFERENCE", caseUrn)
                .replaceAll("RANDOM_DEFENDANT_ID_ONE", defendantId1)
                .replaceAll("RANDOM_DEFENDANT_ID_TWO", defendantId2)
                .replace("RANDOM_DOC_ID", courtDocumentId)
                .replace("RANDOM_MATERIAL_ID_ONE", materialIdOne)
                .replace("RANDOM_MATERIAL_ID_TWO", materialIdTwo)
                .replace("RANDOM_REFERRAL_ID", referralId);
    }

    public static String createReferProsecutionCaseToCrownCourtJsonBody(final String caseId, final String defendantId, final String materialIdOne,
                                                                        final String materialIdTwo, final String courtDocumentId, final String referralId,
                                                                        final String caseUrn, final String filePath,final String initialOffenceId1,final String initialOffenceId2,final String initialOffenceId3,final String initialOffenceId4) throws IOException {
        final URL resource = getResource(filePath);
        return Resources.toString(resource, Charset.defaultCharset())
                .replace("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_REFERENCE", caseUrn)
                .replaceAll("RANDOM_DEFENDANT_ID", defendantId)
                .replace("RANDOM_DOC_ID", courtDocumentId)
                .replace("RANDOM_MATERIAL_ID_ONE", materialIdOne)
                .replace("RANDOM_MATERIAL_ID_TWO", materialIdTwo)
                .replace("RANDOM_REFERRAL_ID", referralId)
                .replace("INITIAL_OFFENCEID_1",initialOffenceId1)
                .replace("INITIAL_OFFENCEID_2",initialOffenceId2)
                .replace("INITIAL_OFFENCEID_3",initialOffenceId3)
                .replace("INITIAL_OFFENCEID_4",initialOffenceId4)
                ;

    }

    public static String createReferProsecutionCaseToCrownCourtJsonBody(final String caseId, final String defendantId, final String materialIdOne,
                                                                        final String materialIdTwo, final String courtDocumentId, final String referralId,
                                                                        final String caseUrn, final String filePath) throws IOException {
        final URL resource = getResource(filePath);
        return Resources.toString(resource, Charset.defaultCharset())
                .replace("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_REFERENCE", caseUrn)
                .replaceAll("RANDOM_DEFENDANT_ID", defendantId)
                .replace("RANDOM_DOC_ID", courtDocumentId)
                .replace("RANDOM_MATERIAL_ID_ONE", materialIdOne)
                .replace("RANDOM_MATERIAL_ID_TWO", materialIdTwo)
                .replace("RANDOM_REFERRAL_ID", referralId);
    }

    public static String createUpdateDefendantListingStatusJsonBody(final String hearingId,
                                                                    final String filePath) throws IOException {
        final URL resource = getResource(filePath);
        return Resources.toString(resource, Charset.defaultCharset())
                .replace("RANDOM_HEARING_ID", hearingId);
    }

    public static String createReferProsecutionCaseToCrownCourtJsonBody(final String caseId1, final String caseId2, final String defendantId1, final String defendantId2,
                                                                        final String materialIdOne, final String materialIdTwo, final String courtDocumentId,
                                                                        final String referralId, final String caseUrn, final String filePath) throws IOException {
        final URL resource = getResource(filePath);
        return Resources.toString(resource, Charset.defaultCharset())
                .replace("RANDOM_CASE_ID_ONE", caseId1)
                .replace("RANDOM_CASE_ID_TWO", caseId2)
                .replace("RANDOM_REFERENCE", caseUrn)
                .replaceAll("RANDOM_DEFENDANT_ID_ONE", defendantId1)
                .replaceAll("RANDOM_DEFENDANT_ID_TWO", defendantId2)
                .replace("RANDOM_DOC_ID", courtDocumentId)
                .replace("RANDOM_MATERIAL_ID_ONE", materialIdOne)
                .replace("RANDOM_MATERIAL_ID_TWO", materialIdTwo)
                .replace("RANDOM_REFERRAL_ID", referralId);
    }

    private static String createReferProsecutionCaseToCrownCourtJsonBody(final String caseId, final String defendantId, final String materialIdOne,
                                                                         final String materialIdTwo, final String courtDocumentId, final String referralId,
                                                                         final String caseUrn) throws IOException {
        return createReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId, materialIdOne,
                materialIdTwo, courtDocumentId, referralId, caseUrn, "progression.command.prosecution-case-refer-to-court.json");
    }

    private static String createReferProsecutionCaseToCrownCourtJsonBodyNullPostCode(
            final String caseId,
            final String defendantId,
            final String materialIdOne,
            final String materialIdTwo, final String courtDocumentId,
            final String referralId,
            final String caseUrn) throws IOException {

        return createReferProsecutionCaseToCrownCourtJsonBody(caseId,
                defendantId,
                materialIdOne,
                materialIdTwo,
                courtDocumentId,
                referralId,
                caseUrn,
                "progression.command.prosecution-case-refer-to-court-null-postcode.json");
    }

    private static String getLAAReferenceForOffenceJsonBody(final String statusCode) throws IOException {
        return Resources.toString(Resources.getResource("progression.command-record-laareference.json"), Charset.defaultCharset())
                .replace("RANDOM_STATUS_CODE", statusCode);
    }

    private static String getLAAReferenceForOffenceJsonBodyWithStatus(final String statusCode, final String statusDescription) throws IOException {
        return Resources.toString(Resources.getResource("progression.command-record-laareference-with-status-description.json"), Charset.defaultCharset())
                .replace("RANDOM_STATUS_CODE", statusCode)
                .replace("RANDOM_STATUS_DESCRIPTION", statusDescription);
    }

    private static String getReceiveRepresentationOrderJsonBody(final String statusCode, final String laaContractNumber) throws IOException {
        return Resources.toString(Resources.getResource("progression.command-receive-representationorder.json"), Charset.defaultCharset())
                .replace("RANDOM_STATUS_CODE", statusCode)
                .replace("RANDOM_LAA_CONTRACT_NUMBER", laaContractNumber);
    }

    private static String getInitiateCourtProceedingsJsonFromResourceForDefendantMatching(final String resourceLocation,
                                                                                          final String caseId,
                                                                                          final String defendantId,
                                                                                          final String masterDefendantId,
                                                                                          final String materialIdOne, final String materialIdTwo,
                                                                                          final String referralId, final String caseUrn,
                                                                                          final String listedStartDateTime, final String earliestStartDateTime,
                                                                                          final String dob) throws IOException {
        return Resources.toString(Resources.getResource(resourceLocation), Charset.defaultCharset())
                .replace("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_REFERENCE", caseUrn)
                .replace("RANDOM_DEFENDANT_ID", defendantId)
                .replace("RANDOM_MASTER_DEFENDANT_ID", masterDefendantId)
                .replaceAll("RANDOM_DEFENDANT_ID", defendantId)
                .replace("RANDOM_MATERIAL_ID_ONE", materialIdOne)
                .replace("RANDOM_MATERIAL_ID_TWO", materialIdTwo)
                .replace("RANDOM_REFERRAL_ID", referralId)
                .replace("LISTED_START_DATE_TIME", listedStartDateTime)
                .replace("EARLIEST_START_DATE_TIME", earliestStartDateTime)
                .replace("DOB", dob);

    }

    private static String getInitiateCourtProceedingsJsonFromResourceForPartialOrExactMatchDefendant(final String resourceLocation,
                                                                                                     final String caseId,
                                                                                                     final String defendantId,
                                                                                                     final String caseReceivedDate) throws IOException {
        return Resources.toString(Resources.getResource(resourceLocation), Charset.defaultCharset())
                .replace("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_REFERENCE", generateUrn())
                .replace("RANDOM_DEFENDANT_ID", defendantId)
                .replaceAll("RANDOM_DEFENDANT_ID", defendantId)
                .replace("CASE_RECEIVED_DATE", caseReceivedDate);

    }

    private static String getInitiateCourtProceedingsJsonFromResource(final String resourceLocation, final String caseId, final String defendantId, final String materialIdOne,
                                                                      final String materialIdTwo,
                                                                      final String referralId, final String caseUrn,
                                                                      final String listedStartDateTime, final String earliestStartDateTime,
                                                                      final String dob) throws IOException {
        return Resources.toString(Resources.getResource(resourceLocation), Charset.defaultCharset())
                .replace("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_REFERENCE", caseUrn)
                .replaceAll("RANDOM_DEFENDANT_ID", defendantId)
                .replace("RANDOM_MATERIAL_ID_ONE", materialIdOne)
                .replace("RANDOM_MATERIAL_ID_TWO", materialIdTwo)
                .replace("RANDOM_REFERRAL_ID", referralId)
                .replace("LISTED_START_DATE_TIME", listedStartDateTime)
                .replace("EARLIEST_START_DATE_TIME", earliestStartDateTime)
                .replace("DOB", dob);

    }

    private static String getInitiateCourtProceedingsJsonFromResource(final String resourceLocation, final String caseId, final String defendantId, final String defendantId2, final String materialIdOne,
                                                                      final String materialIdTwo,
                                                                      final String referralId, final String caseUrn,
                                                                      final String listedStartDateTime, final String earliestStartDateTime,
                                                                      final String dob) throws IOException {
        return Resources.toString(Resources.getResource(resourceLocation), Charset.defaultCharset())
                .replaceAll("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_REFERENCE", caseUrn)
                .replaceAll("RANDOM_DEFENDANT_ID", defendantId)
                .replaceAll("RANDOM_DEFENDANT2_ID", defendantId2)
                .replace("RANDOM_MATERIAL_ID_ONE", materialIdOne)
                .replace("RANDOM_MATERIAL_ID_TWO", materialIdTwo)
                .replace("RANDOM_REFERRAL_ID", referralId)
                .replace("LISTED_START_DATE_TIME", listedStartDateTime)
                .replace("EARLIEST_START_DATE_TIME", earliestStartDateTime)
                .replace("DOB", dob);

    }

    private static String getInitiateCourtProceedingsJsonBody(final String caseId, final String defendantId, final String materialIdOne,
                                                              final String materialIdTwo,
                                                              final String referralId, final String caseUrn,
                                                              final String listedStartDateTime, final String earliestStartDateTime,
                                                              final String dob) throws IOException {
        return getInitiateCourtProceedingsJsonFromResource("progression.command.initiate-court-proceedings.json", caseId,
                defendantId, materialIdOne, materialIdTwo, referralId, caseUrn, listedStartDateTime, earliestStartDateTime, dob);

    }

    private static String getInitiateCourtProceedingsJsonBodyForDefendantMatching(final String caseId, final String defendantId, final String masterDefendantId, final String materialIdOne,
                                                                                  final String materialIdTwo,
                                                                                  final String referralId, final String caseUrn,
                                                                                  final String listedStartDateTime, final String earliestStartDateTime,
                                                                                  final String dob) throws IOException {
        return getInitiateCourtProceedingsJsonFromResourceForDefendantMatching("progression.command.initiate-court-proceedings-for-defendant-matching.json", caseId,
                defendantId, masterDefendantId, materialIdOne, materialIdTwo, referralId, caseUrn, listedStartDateTime, earliestStartDateTime, dob);

    }

    private static String getInitiateCourtProceedingsJsonBodyForPartialOrExactMatching(final String caseId, final String defendantId, final String caseReceivedDate) throws IOException {
        return getInitiateCourtProceedingsJsonFromResourceForPartialOrExactMatchDefendant("progression.command.initiate-court-proceedings-for-partial-or-exact-match-defendants.json",
                caseId, defendantId, caseReceivedDate);
    }

    private static String getInitiateCourtProceedingsJsonBodyForExactMatching(final String caseId, final String defendantId, final String caseReceivedDate, final String channel) throws IOException {
        String jsonString = getInitiateCourtProceedingsJsonFromResourceForPartialOrExactMatchDefendant("progression.command.initiate-court-proceedings-for-exact-match-defendants.json",
                caseId, defendantId, caseReceivedDate);
        if (channel.equalsIgnoreCase("CPPI")) {
            jsonString = getInitiateCourtProceedingsJsonFromResourceForPartialOrExactMatchDefendant("progression.command.initiate-court-proceedings-for-exact-match-defendants-2.json",
                    caseId, defendantId, caseReceivedDate);
        }
        return jsonString;
    }


    private static String getInitiateCourtProceedingsJsonBodyForPartialMatching(final String caseId, final String defendantId, final String caseReceivedDate, final String channel) throws IOException {
        String jsonString = getInitiateCourtProceedingsJsonFromResourceForPartialOrExactMatchDefendant("progression.command.initiate-court-proceedings-for-partial-match-defendants.json",
                caseId, defendantId, caseReceivedDate);
        if (channel.equalsIgnoreCase("CPPI")) {
            jsonString = getInitiateCourtProceedingsJsonFromResourceForPartialOrExactMatchDefendant("progression.command.initiate-court-proceedings-for-partial-match-defendants-2.json",
                    caseId, defendantId, caseReceivedDate);
        }
        return jsonString;
    }

    private static String getReferProsecutionCaseToCrownCourtWithMinimumAttribute(final String caseId, final String defendantId, final String caseUrn) throws IOException {
        return Resources.toString(getResource("progression.command.prosecution-case-refer-to-court-minimal-payload.json"), Charset.defaultCharset())
                .replace("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_REFERENCE", caseUrn)
                .replaceAll("RANDOM_DEFENDANT_ID", defendantId);
    }


    public static String getProsecutionCaseDefendantUpdatedEvent(final String caseId, final String defendantId,
                                                                 final String caseUrn,
                                                                 final String filePath) throws IOException {
        return Resources.toString(getResource(filePath), Charset.defaultCharset())
                .replace("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_REFERENCE", caseUrn)
                .replaceAll("RANDOM_DEFENDANT_ID", defendantId);
    }

    // Progression Test DSL for preconditions and assertions
    public static void givenCaseAddedToCrownCourt(final String caseId, final String firstDefendantId, final String secondDefendantId) throws IOException {
        final Response writeResponse = addCaseToCrownCourt(caseId, firstDefendantId, secondDefendantId);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
    }

    public static String pollCaseProgressionFor(final String caseId, final Matcher... matchers) {
        return pollForResponse(join("", "/cases/", caseId), "application/vnd.progression.query.caseprogressiondetail+json", matchers);
    }

    public static String pollProsecutionCasesProgressionFor(final String caseId) {
        return pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.id", equalTo(caseId)));

    }

    public static String getCaseLsmInfoFor(final String caseId, final Matcher[] matchers) {
        return poll(requestParams(getReadUrl(format("/prosecutioncases/%s/lsm-info", caseId)), "application/vnd.progression.query.case-lsm-info+json").withHeader(USER_ID, UUID.randomUUID()))
                .timeout(60L, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                matchers
                        ))).getPayload();
    }

    public static String getHearingForDefendant(final String hearingId) {
        return getHearingForDefendant(hearingId, new Matcher[]{withJsonPath("$.hearing.id", equalTo(hearingId))});
    }

    public static String getHearingForDefendant(final String hearingId, final Matcher[] matchers) {
        return poll(requestParams(getReadUrl("/hearingSearch/" + hearingId), "application/vnd.progression.query.hearing+json").withHeader(USER_ID, UUID.randomUUID()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                matchers
                        ))).getPayload();
    }

    public static String pollProsecutionCasesProgressionFor(final String caseId, final Matcher... matchers) {
        return pollForResponse("/prosecutioncases/" + caseId, "application/vnd.progression.query.prosecutioncase+json", matchers);
    }

    public static String pollProsecutionCasesProgressionForCAAG(final String caseId, final Matcher... matchers) {
        return pollForResponse("/prosecutioncases/" + caseId, "application/vnd.progression.query.prosecutioncase.caag+json", matchers);
    }

    public static String pollProsecutionCasesProgressionAndReturnHearingId(final String caseId, final String defendantId, final Matcher... matchers) {
        final JsonObject prosecutionCaseJson = getJsonObject(pollProsecutionCasesProgressionFor(caseId, matchers));

        return extractHearingIdFromProsecutionCasesProgression(prosecutionCaseJson, defendantId);
    }

    public static String extractHearingIdFromProsecutionCasesProgression(final JsonObject prosecutionCaseJson, final String defendantId) {
        final Optional<JsonValue> defendantHearing = prosecutionCaseJson.getJsonObject("hearingsAtAGlance")
                .getJsonArray("defendantHearings")
                .stream().filter(def1 -> ((JsonObject) def1).getString("defendantId").equals(defendantId))
                .findFirst();

        return ((JsonObject) defendantHearing.get()).getJsonArray("hearingIds").get(0).toString().replaceAll("\"", "");
    }

    public static String pollPartialMatchDefendantFor(final Matcher... matchers) {
        return pollForResponse("/partial-match-defendants?sortOrder=DESC&sortField=caseReceivedDate", "application/vnd.progression.query.partial-match-defendant+json", matchers);
    }

    public static String getApplicationFor(final String applicationId) {
        return pollForResponse(join("", "/applications/", applicationId), "application/vnd.progression.query.application+json");
    }

    public static void verifyCasesForSearchCriteria(final String searchCriteria, final Matcher[] matchers) {
        poll(requestParams(getReadUrl(join("", "/search?q=", searchCriteria)), "application/vnd.progression.query.search-cases+json").withHeader(USER_ID, UUID.randomUUID()))
                .timeout(RestHelper.TIMEOUT, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                matchers
                        ))).getPayload();
    }

    public static String getCourtDocuments(final String userId, final String... args) {
        return pollForResponse(MessageFormat.format("/courtdocumentsearch?caseId={0}&defendantId={1}&hearingId={2}", args), "application/vnd.progression.query.courtdocuments+json", userId);
    }

    public static String getCourtDocumentsByCase(final String userId, final String caseId) {
        return pollForResponse(MessageFormat.format("/courtdocumentsearch?caseId={0}", new String[]{caseId}), "application/vnd.progression.query.courtdocuments+json", userId);
    }

    public static String getUploadCourtDocumentsByCase(final String userId, final String caseId) {
        return pollForResponse(MessageFormat.format("/courtdocumentsearch?caseId={0}", new String[]{caseId}), "application/vnd.progression.query.courtdocuments+json", userId, status().is(OK), withJsonPath("$.documentIndices[0].caseIds[0]", CoreMatchers.is(caseId)));
    }

    public static String getCourtDocumentsByCaseWithMatchers(final String userId, final String caseDocumentId, final String caseId) {
        final Matcher[] hearingMatchers = {
                withJsonPath("$.documentIndices[0].document.courtDocumentId", is(caseDocumentId))
        };
        return pollForResponse(MessageFormat.format("/courtdocumentsearch?caseId={0}", new String[]{caseId}), "application/vnd.progression.query.courtdocuments+json", userId, hearingMatchers);
    }

    public static String getCourtDocumentsByDefendant(final String userId, final String defendantId) {
        return pollForResponse(MessageFormat.format("/courtdocumentsearch?defendantId={0}", new String[]{defendantId}), "application/vnd.progression.query.courtdocuments+json", userId);
    }

    public static String getCourtDocumentsByApplication(final String userId, final String applicationid) {
        return pollForResponse(MessageFormat.format("/courtdocumentsearch?applicationId={0}", new String[]{applicationid}), "application/vnd.progression.query.courtdocuments+json", userId);
    }

    public static String getCourtDocumentsByCaseAndDefendant(final String userId, final String caseId, final String defendantId) {
        return pollForResponse(MessageFormat.format("/courtdocumentsearch?caseId={0}&defendantId={1}", new String[]{caseId, defendantId}), "application/vnd.progression.query.courtdocuments+json", userId);
    }

    public static javax.ws.rs.core.Response getMaterialContent(final UUID materialId, final UUID userId) {
        return getMaterialContentResponse("/material/" + materialId.toString() + "/content", userId, "application/vnd.progression.query.material-content+json");

    }

    public static javax.ws.rs.core.Response getMaterialContent(final UUID materialId, final UUID userId, final UUID defendantId) {
        return getMaterialContentResponse("/material/" + materialId.toString() + "/content?defendantId=" + defendantId, userId, "application/vnd.progression.query.material-content-for-defence+json");

    }

    public static String getCourtDocumentsByDefendantForDefenceWithNoCaseAndDefenceId(final String userId, final ResponseStatusMatcher responseStatusMatcher) {
        return pollForResponse(MessageFormat.format("/courtdocumentsearch", new String[]{}), "application/vnd.progression.query.courtdocuments.for.defence+json", userId, responseStatusMatcher);
    }

    public static String getCourtDocumentsByDefendantForDefenceWithNoCaseId(final String userId, final String defendantId, final ResponseStatusMatcher responseStatusMatcher) {
        return pollForResponse(MessageFormat.format("/courtdocumentsearch?defendantId={0}", new String[]{defendantId}), "application/vnd.progression.query.courtdocuments.for.defence+json", userId, responseStatusMatcher);
    }

    public static String getCourtDocumentsByDefendantForDefenceWithNoDefendantId(final String userId, final String caseId, final ResponseStatusMatcher responseStatusMatcher) {
        return pollForResponse(MessageFormat.format("/courtdocumentsearch?caseId={0}", new String[]{caseId}), "application/vnd.progression.query.courtdocuments.for.defence+json", userId, responseStatusMatcher);
    }

    public static String getCourtDocumentsByDefendantForDefence(final String userId, final String caseId, final String defendantId, final ResponseStatusMatcher responseStatusMatcher) {
        return pollForResponse(MessageFormat.format("/courtdocumentsearch?caseId={0}&defendantId={1}", new String[]{caseId, defendantId}), "application/vnd.progression.query.courtdocuments.for.defence+json", userId, responseStatusMatcher);
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

    public static String ejectCaseExtractPdf(final String caseId, final String defendantId) {
        return pollForResponse(join("", "/prosecutioncases/", caseId, "/defendants/", defendantId, "/ejectcase/"), "application/vnd.progression.query.eject-case+json");
    }

    public static String getApplicationExtractPdf(final String applicationId, final String hearingIds) {
        final String queryParam = "?hearingIds=" + hearingIds;
        return pollForResponse(join("", "/applications/", applicationId, "/extract", queryParam), "application/vnd.progression.query.court-extract-application+json");
    }

    public static Response addCourtApplication(final String caseId, final String applicationId, final String fileName) throws IOException {
        return addCourtApplication(caseId, applicationId, generateUrn(), fileName);
    }

    public static Response addLinkedCourtApplication(final String caseId, final String defendantId, final String applicationId, final String fileName) throws IOException {
        return postCommand(getWriteUrl("/application"),
                "application/vnd.progression.create-court-application+json",
                getCourtApplicationJsonBody(caseId, defendantId, applicationId, generateUrn(), fileName));
    }

    public static Response addCourtApplication(final String caseId, final String applicationId, final String caseUrn, final String fileName) throws IOException {
        return postCommand(getWriteUrl("/application"),
                "application/vnd.progression.create-court-application+json",
                getCourtApplicationJsonBody(caseId, applicationId, caseUrn, fileName));
    }

    public static Response shareCourtDocument(final String courtDocumentId, final String hearingId, final String userGroup, final String fileName) throws IOException {
        return postCommand(getWriteUrl("/sharecourtdocument"),
                "application/vnd.progression.share-court-document+json",
                getShareCourtDocumentJsonBody(courtDocumentId, hearingId, userGroup, fileName));
    }

    public static Response addCourtApplicationWithDefendant(final String caseId, final String applicationId, final String defendantId, final String fileName) throws IOException {
        return postCommand(getWriteUrl("/application"),
                "application/vnd.progression.create-court-application+json",
                getCourtApplicationWithDefendantJsonBody(caseId, applicationId, defendantId, generateUrn(), fileName));
    }

    public static Response addCourtApplicationForApplicationAtAGlance(final String caseId,
                                                                      final String applicationId,
                                                                      final String particulars,
                                                                      final String applicantReceivedDate,
                                                                      final String applicationType,
                                                                      final Boolean appeal,
                                                                      final String paymentReference,
                                                                      final String applicantSynonym,
                                                                      final String applicantFirstName,
                                                                      final String applicantLastName,
                                                                      final String applicantNationality,
                                                                      final String applicantRemandStatus,
                                                                      final String applicantRepresentation,
                                                                      final String interpreterLanguageNeeds,
                                                                      final LocalDate applicantDoB,
                                                                      final String applicantAddress1,
                                                                      final String applicantAddress2,
                                                                      final String applicantAddress3,
                                                                      final String applicantAddress4,
                                                                      final String applicantAddress5,
                                                                      final String applicantPostCode,
                                                                      final String applicationReference,
                                                                      final String respondentOrganisationName,
                                                                      final String respondentOrganisationAddress1,
                                                                      final String respondentOrganisationAddress2,
                                                                      final String respondentOrganisationAddress3,
                                                                      final String respondentOrganisationAddress4,
                                                                      final String respondentOrganisationAddress5,
                                                                      final String respondentOrganisationPostcode,
                                                                      final String respondentRepresentativeFirstName,
                                                                      final String respondentRepresentativeLastName,
                                                                      final String respondentRepresentativePosition,
                                                                      final String parentApplicationId,
                                                                      final String fileName)
            throws IOException {
        final String body = Resources.toString(getResource(fileName), Charset.defaultCharset())
                .replace("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_APPLICATION_ID", applicationId)
                .replace("RANDOM_PARENT_APPLICATION_ID", parentApplicationId)
                .replaceAll("RANDOM_PARTICULARS", particulars)
                .replaceAll("\"applicationReceivedDate\": \"2019-01-01\"", format("\"applicationReceivedDate\": \"%s\"", applicantReceivedDate))
                .replaceAll("RANDOM_APPLICATION_TYPE", applicationType)
                .replaceAll("\"RANDOM_APPLICATION_APPEAL\"", appeal.toString())
                .replaceAll("RANDOM_PAYMENT_REFERENCE", paymentReference)
                .replaceAll("RANDOM_APPLICANT_SYNONYM", applicantSynonym)
                .replaceAll("RANDOM_FIRST_NAME", applicantFirstName)
                .replaceAll("RANDOM_LAST_NAME", applicantLastName)
                .replaceAll("RANDOM_NATIONALITY_DESCRIPTION", applicantNationality)
                .replaceAll("RANDOM_BAIL_STATUS_DESCRIPTION", applicantRemandStatus)
                .replaceAll("RANDOM_REPRESENTATION_ORGANISATION_NAME", applicantRepresentation)
                .replaceAll("RANDOM_INTERPRETER_LANGUAGE_NEEDS", interpreterLanguageNeeds)
                .replaceAll("RANDOM_DATE_OF_BIRTH", applicantDoB.toString())
                .replaceAll("RANDOM_ADDRESS1", applicantAddress1)
                .replaceAll("RANDOM_ADDRESS2", applicantAddress2)
                .replaceAll("RANDOM_ADDRESS3", applicantAddress3)
                .replaceAll("RANDOM_ADDRESS4", applicantAddress4)
                .replaceAll("RANDOM_ADDRESS5", applicantAddress5)
                .replaceAll("RANDOM_POSTCODE", applicantPostCode)
                .replaceAll("RANDOM_RESPONDENT_ORGANISATION_NAME", respondentOrganisationName)
                .replaceAll("RANDOM_RESPONDENT_ORGANISATION_ADDRESS1", respondentOrganisationAddress1)
                .replaceAll("RANDOM_RESPONDENT_ORGANISATION_ADDRESS2", respondentOrganisationAddress2)
                .replaceAll("RANDOM_RESPONDENT_ORGANISATION_ADDRESS3", respondentOrganisationAddress3)
                .replaceAll("RANDOM_RESPONDENT_ORGANISATION_ADDRESS4", respondentOrganisationAddress4)
                .replaceAll("RANDOM_RESPONDENT_ORGANISATION_ADDRESS5", respondentOrganisationAddress5)
                .replaceAll("RANDOM_RESPONDENT_ORGANISATION_POSTCODE", respondentOrganisationPostcode)
                .replaceAll("RANDOM_RESPONDENT_REPRESENTATIVE_FIRST_NAME", respondentRepresentativeFirstName)
                .replaceAll("RANDOM_RESPONDENT_REPRESENTATIVE_LAST_NAME", respondentRepresentativeLastName)
                .replaceAll("RANDOM_RESPONDENT_REPRESENTATIVE_POSITION", respondentRepresentativePosition)


                .replaceAll("RANDOM_REFERENCE", applicationReference);

        LOGGER.info("applicationId={}, parentApplicationId={}, body={}", applicationId, parentApplicationId, body);
        return postCommand(getWriteUrl("/application"),
                "application/vnd.progression.create-court-application+json", body);
    }

    public static Response addCourtApplicationForIngestion(final String caseId,
                                                           final String applicationId,
                                                           final String applicantId,
                                                           final String applicantDefendantId,
                                                           final String respondantId,
                                                           final String respondantDefendantId,
                                                           final String applicationReference,
                                                           final String fileName)
            throws IOException {
        final String body = Resources.toString(getResource(fileName), Charset.defaultCharset())
                .replaceAll("RANDOM_APPLICATION_ID", applicationId)
                .replaceAll("RANDOM_CASE_ID", caseId)
                .replaceAll("RANDOM_APPLICANT_ID", applicantId)
                .replaceAll("RANDOM_APPLICANT_DEFENDANT_ID", applicantDefendantId)
                .replaceAll("RANDOM_RESPONDANT_ID", respondantId)
                .replaceAll("RANDOM_RESPONDANT_DEFENDANT_ID", respondantDefendantId)
                .replaceAll("RANDOM_REFERENCE", applicationReference);

        LOGGER.info(body);
        return postCommand(getWriteUrl("/application"),
                "application/vnd.progression.create-court-application+json", body);
    }

    public static Response addCourtApplicationForIngestion(final String caseId,
                                                           final String applicationId,
                                                           final String applicantId,
                                                           final String applicantDefendantId,
                                                           final String respondantId,
                                                           final String respondantDefendantId,
                                                           final String fileName) throws IOException {
        return addCourtApplicationForIngestion(caseId, applicationId, applicantId, applicantDefendantId,
                respondantId, respondantDefendantId, randomUUID().toString(), fileName);
    }

    public static Response updateCourtApplicationForIngestion(final String caseId,
                                                              final String applicationId,
                                                              final String applicantId,
                                                              final String applicantDefendantId,
                                                              final String respondantId,
                                                              final String respondantDefendantId,
                                                              final String applicationReference,
                                                              final String fileName)
            throws IOException {
        final String body = Resources.toString(getResource(fileName), Charset.defaultCharset())
                .replaceAll("RANDOM_CASE_ID", caseId)
                .replaceAll("RANDOM_APPLICATION_ID", applicationId)
                .replaceAll("RANDOM_APPLICANT_ID", applicantId)
                .replaceAll("RANDOM_APPLICANT_DEFENDANT_ID", applicantDefendantId)
                .replaceAll("RANDOM_RESPONDANT_ID", respondantId)
                .replaceAll("RANDOM_RESPONDANT_DEFENDANT_ID", respondantDefendantId)
                .replaceAll("RANDOM_REFERENCE", applicationReference);

        return postCommand(getWriteUrl("/application"),
                "application/vnd.progression.update-court-application+json", body);
    }

    public static Response updateCourtApplication(final String applicationId, final String applicantId, final String caseId, final String defendantId, final String fileName) throws IOException {
        return postCommand(getWriteUrl("/application"),
                "application/vnd.progression.update-court-application+json",
                getUpdateCourtApplicationJsonBody(applicationId, applicantId, caseId, defendantId, fileName));
    }

    public static Response addStandaloneCourtApplication(final String applicationId, final String parentApplicationId, final CourtApplicationRandomValues randomValues, final String fileName) throws IOException {
        return postCommand(getWriteUrl("/application"),
                "application/vnd.progression.create-court-application+json",
                getStandaloneCourtApplicationJsonBody(applicationId, parentApplicationId, generateUrn(), randomValues, fileName));
    }

    public static Response linkCases(final String prosecutionCaseId, final String caseUrn2, final String caseUrn3, final String fileName) throws IOException {
        return postCommand(getWriteUrl(String.format("/prosecutioncases/%s/link", prosecutionCaseId)),

                "application/vnd.progression.link-cases+json",
                getLSMCasesJsonBody(prosecutionCaseId, caseUrn2, caseUrn3, fileName));
    }

    public static Response splitCase(final String prosecutionCaseId, final String caseUrn2, final String caseUrn3, final String fileName) throws IOException {
        return postCommand(getWriteUrl(String.format("/prosecutioncases/%s/link", prosecutionCaseId)),

                "application/vnd.progression.link-cases+json",
                getLSMCasesJsonBody(prosecutionCaseId, caseUrn2, caseUrn3, fileName));
    }

    public static Response mergeCase(final String prosecutionCaseId1, final String caseUrn2, final String fileName) throws IOException {
        return postCommand(getWriteUrl(String.format("/prosecutioncases/%s/link", prosecutionCaseId1)),

                "application/vnd.progression.link-cases+json",
                getLSMCasesJsonBody(prosecutionCaseId1, caseUrn2, fileName));
    }

    public static Response mergeCases(final String prosecutionCaseId1, final String caseUrn2, final String caseUrn3, final String fileName) throws IOException {
        return postCommand(getWriteUrl(String.format("/prosecutioncases/%s/link", prosecutionCaseId1)),

                "application/vnd.progression.link-cases+json",
                getLSMCasesJsonBody(prosecutionCaseId1, caseUrn2, caseUrn3, fileName));
    }

    public static Response unlinkCases(final String prosecutionCaseId, final String prosecutionCaseUrn, final String caseId, final String caseUrn, final String linkGroupId, final String fileName) throws IOException {
        return postCommand(getWriteUrl(String.format("/prosecutioncases/%s/link", prosecutionCaseId)),

                "application/vnd.progression.unlink-cases+json",
                getUnlinkCasesJsonBody(prosecutionCaseId, prosecutionCaseUrn, caseId, caseUrn, linkGroupId, fileName));
    }

    private static String getStandaloneCourtApplicationJsonBody(final String applicationId, final String parentApplicationId, final String applicationReference, final CourtApplicationRandomValues randomValues, final String fileName) throws IOException {

        return Resources.toString(getResource(fileName), Charset.defaultCharset())
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
        return Resources.toString(getResource(fileName), Charset.defaultCharset())
                .replace("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_APPLICATION_ID", applicationId)
                .replace("RANDOM_REFERENCE", applicationReference);
    }

    private static String getCourtApplicationJsonBody(final String caseId, final String defendantId, final String applicationId, final String applicationReference, final String fileName) throws IOException {
        return Resources.toString(getResource(fileName), Charset.defaultCharset())
                .replace("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_DEFENDANT_ID", defendantId)
                .replace("RANDOM_APPLICATION_ID", applicationId)
                .replace("RANDOM_REFERENCE", applicationReference);
    }

    private static String getShareCourtDocumentJsonBody(final String courtDocumentId, final String hearingId, final String userGroup, final String fileName) throws IOException {
        return Resources.toString(Resources.getResource(fileName), Charset.defaultCharset())
                .replace("COURT_DOCUMENT_ID", courtDocumentId)
                .replace("HEARING_ID", hearingId)
                .replace("USER_GROUP", userGroup);
    }

    private static String getLSMCasesJsonBody(final String prosecutionCaseId1, final String caseUrn2, final String caseUrn3, final String fileName) throws IOException {
        return Resources.toString(Resources.getResource(fileName), Charset.defaultCharset())
                .replace("PROSECUTION_CASE_ID", prosecutionCaseId1)
                .replace("CASE_URN2", caseUrn2)
                .replace("CASE_URN3", caseUrn3);
    }

    private static String getUnlinkCasesJsonBody(final String prosecutionCaseId, final String prosecutionCaseUrn, final String caseId, final String caseUrn, final String linkGroupId, final String fileName) throws IOException {
        return Resources.toString(Resources.getResource(fileName), Charset.defaultCharset())
                .replace("PROSECUTION_CASE_ID", prosecutionCaseId)
                .replace("PROSECUTION_CASE_URN", prosecutionCaseUrn)
                .replace("CASE_ID", caseId)
                .replace("CASE_URN", caseUrn)
                .replace("LINK_GROUP_ID", linkGroupId);
    }

    private static String getLSMCasesJsonBody(final String prosecutionCaseId1, final String caseUrn2, final String fileName) throws IOException {
        return Resources.toString(Resources.getResource(fileName), Charset.defaultCharset())
                .replace("PROSECUTION_CASE_ID", prosecutionCaseId1)
                .replace("CASE_URN2", caseUrn2);
    }

    private static String getCourtApplicationWithDefendantJsonBody(final String caseId, final String applicationId, final String defendantId, final String applicationReference, final String fileName) throws IOException {
        return Resources.toString(getResource(fileName), Charset.defaultCharset())
                .replace("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_APPLICATION_ID", applicationId)
                .replace("RANDOM_DEFENDANT_ID", defendantId)
                .replace("RANDOM_REFERENCE", applicationReference);
    }

    private static String getUpdateCourtApplicationJsonBody(final String applicationId, final String applicantId, final String caseId, final String defendantId, final String fileName) throws IOException {
        return Resources.toString(getResource(fileName), Charset.defaultCharset())
                .replace("APPLICATION_ID", applicationId)
                .replace("RANDOM_CASE_ID", caseId)
                .replaceAll("RANDOM_DEFENDANT_ID", defendantId)
                .replace("APPLICANT_ID", applicantId);
    }

    public static Response referCourtApplication(final String applicationId, final String caseId, final String hearingId, final String fileName) throws IOException {
        return postCommand(getWriteUrl("/referapplicationtocourt"),
                "application/vnd.progression.refer-application-to-court+json",
                getReferApplicationToCourtJsonBody(applicationId, caseId, hearingId, fileName));

    }

    public static Response bookSlotsForApplication(final String applicationId, final String hearingRequestId, final String caseId, final String applicationReference, final String fileName) throws IOException {
        return postCommand(getWriteUrl("/referapplicationtocourt"),
                "application/vnd.progression.book-slots-for-application+json",
                getBookSlotsForApplicationJsonBody(applicationId, hearingRequestId, caseId, applicationReference, fileName));

    }

    public static Response referBoxWorkApplication(final String applicationId, final String hearingId, final String fileName) throws IOException {
        return postCommand(getWriteUrl("/refertoboxwork"),
                "application/vnd.progression.refer-box-work-application+json",
                getReferApplicationToCourtJsonBody(applicationId, UUID.randomUUID().toString(), hearingId, fileName));

    }

    public static Response extendHearing(final String applicationId, final String hearingId, final String fileName) throws IOException {
        return postCommand(getWriteUrl("/referapplicationtocourt"),
                "application/vnd.progression.extend-hearing+json",
                getExtendHearingJsonBody(applicationId, hearingId, fileName));

    }

    public static String getCourtDocumentFor(final String courtDocumentId) {
        return pollForResponse(join("", "/courtdocuments/", courtDocumentId), "application/vnd.progression.query.courtdocument+json");
    }

    public static Response ejectCaseApplication(final String applicationId, final String caseId, final String removalReason, final String fileName) throws IOException {
        return postCommand(getWriteUrl("/eject"),
                "application/vnd.progression.eject-case-or-application+json",
                getEjectCaseOrApplicationCommandBody(applicationId, caseId, removalReason, fileName));
    }

    private static String getEjectCaseOrApplicationCommandBody(final String applicationId, final String caseId,
                                                               final String removalReason, final String fileName) throws IOException {
        return Resources.toString(getResource(fileName), Charset.defaultCharset())
                .replace("RANDOM_APPLICATION_ID", applicationId)
                .replace("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_REMOVAL_REASON", removalReason);
    }

    public static String getCourtDocumentFor(final String courtDocumentId, final Matcher... matchers) {
        return poll(requestParams(getReadUrl(join("", "/courtdocuments/", courtDocumentId)), "application/vnd.progression.query.courtdocument+json").withHeader(USER_ID, UUID.randomUUID()))
                .timeout(40, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                matchers
                        ))).getPayload();
    }

    public static String verifyQueryResultsForbidden(final String courtDocumentId, final String userId, final Matcher... matchers) {
        return poll(requestParams(getReadUrl(join("", "/courtdocuments/", courtDocumentId)), "application/vnd.progression.query.courtdocument+json")
                .withHeader(USER_ID, userId))
                .timeout(40, TimeUnit.SECONDS)
                .until(
                        status().is(FORBIDDEN), payload().isJson(allOf(
                                matchers))).getPayload();
    }

    private static String getReferApplicationToCourtJsonBody(final String applicationId, final String caseId, final String hearingId, final String fileName) throws IOException {
        return Resources.toString(getResource(fileName), Charset.defaultCharset())
                .replace("RANDOM_APPLICATION_ID", applicationId)
                .replaceAll("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_HEARING_ID", hearingId);

    }

    private static String getBookSlotsForApplicationJsonBody(final String applicationId, final String hearingRequestId, final String caseId, final String applicationReference, final String fileName) throws IOException {
        return Resources.toString(getResource(fileName), Charset.defaultCharset())
                .replaceAll("RANDOM_APPLICATION_ID", applicationId)
                .replaceAll("RANDOM_HEARING_ID", hearingRequestId)
                .replaceAll("RANDOM_CASE_ID", caseId)
                .replaceAll("RANDOM_REFERENCE", applicationReference);

    }

    private static String getExtendHearingJsonBody(final String applicationId, final String hearingId, final String fileName) throws IOException {
        return Resources.toString(getResource(fileName), Charset.defaultCharset())
                .replace("RANDOM_APPLICATION_ID", applicationId)
                .replace("RANDOM_HEARING_ID", hearingId);

    }

    public static void pollForApplicationStatus(final String applicationId, final String status) {
        pollForApplication(applicationId,
                withJsonPath("$.courtApplication.id", equalTo(applicationId)),
                withJsonPath("$.courtApplication.applicationStatus", equalTo(status))
        );

    }

    public static void pollForApplication(final String applicationId) {
        poll(requestParams(getReadUrl("/applications/" + applicationId),
                "application/vnd.progression.query.application+json").withHeader(USER_ID, randomUUID()))
                .until(status().is(OK));

    }

    public static void pollForApplication(final String applicationId, final Matcher... matchers) {
        poll(requestParams(getReadUrl("/applications/" + applicationId),
                "application/vnd.progression.query.application+json").withHeader(USER_ID, randomUUID()))
                .until(status().is(OK),
                        payload().isJson(allOf(matchers)
                        ));

    }


}
