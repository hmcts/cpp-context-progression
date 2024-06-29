package uk.gov.moj.cpp.progression.helper;

import static com.google.common.io.Resources.getResource;
import static com.jayway.jsonassert.JsonAssert.emptyCollection;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.RestHelper.TIMEOUT;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getMaterialContentResponse;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommandWithUserId;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.moj.cpp.progression.helper.CourtApplicationsHelper.CourtApplicationRandomValues;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.jms.MessageConsumer;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.core.MultivaluedMap;

import com.google.common.io.Resources;
import com.jayway.jsonpath.ReadContext;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import org.apache.commons.lang3.StringUtils;
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
                                                                      final String caseReference, final String commandPayload, final String initialOffenceId1, final String initialOffenceId2, final String initialOffenceId3, final String initialOffenceId4) throws IOException {
        return postCommand(getWriteUrl("/refertocourt"),
                "application/vnd.progression.refer-cases-to-court+json",
                createReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId, materialIdOne, materialIdTwo, courtDocumentId, referralId, caseReference, commandPayload, initialOffenceId1, initialOffenceId2, initialOffenceId3, initialOffenceId4
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
        return addProsecutionCaseToCrownCourt(caseId, defendantId, materialIdOne, materialIdTwo, courtDocumentId, referralId, generateUrn());
    }

    public static Response addProsecutionCaseToCrownCourt(final String caseId, final String defendantId, final String materialIdOne,
                                                          final String materialIdTwo, final String courtDocumentId, final String referralId, final String caseUrn) throws IOException {
        return postCommand(getWriteUrl("/refertocourt"),
                "application/vnd.progression.refer-cases-to-court+json",
                createReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId, materialIdOne, materialIdTwo, courtDocumentId, referralId, caseUrn));
    }

    public static Response addProsecutionCaseToMagsCourt(final String caseId, final String defendantId, final String referralId, final String caseUrn, final String postCode) throws IOException {
        return postCommand(getWriteUrl("/refertocourt"),
                "application/vnd.progression.refer-cases-to-court+json",
                createReferProsecutionCaseToMagsCourtJsonBody(caseId, defendantId, referralId, caseUrn, postCode));
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


    public static javax.ws.rs.core.Response recordLAAReferenceWithUserId(final String caseId, final String defendantId, final String offenceId, final String statusCode, final String statusDescription, final String userId) {
        final RestClient restClient = new RestClient();
        return restClient.postCommand(getWriteUrl(String.format("/laaReference/cases/%s/defendants/%s/offences/%s", caseId, defendantId, offenceId)),
                        "application/vnd.progression.command.record-laareference-for-offence+json",
                        getLAAReferenceForOffenceJsonBodyWithStatus(statusCode, statusDescription),
                        createHttpHeaders(userId));
    }

    public static javax.ws.rs.core.Response receiveRepresentationOrder(final String caseId, final String defendantId, final String offenceId, final String statusCode, final String laaContractNumber, final String userId) {
        final RestClient restClient = new RestClient();
        return restClient.postCommand(getWriteUrl(String.format("/representationOrder/cases/%s/defendants/%s/offences/%s", caseId, defendantId, offenceId)),
                        "application/vnd.progression.command.receive-representationorder-for-defendant+json",
                        getReceiveRepresentationOrderJsonBody(statusCode, laaContractNumber),
                        createHttpHeaders(userId));
    }

    public static MultivaluedMap<String, Object> createHttpHeaders(final String userId) {
        final MultivaluedMap<String, Object> headers = new MultivaluedMapImpl<>();
        headers.add(HeaderConstants.USER_ID, userId);
        return headers;
    }

    public static Response addProsecutionCaseToCrownCourt(final String caseId, final String defendantId) throws IOException {
        return addProsecutionCaseToCrownCourt(caseId, defendantId, generateUrn());
    }
    public static Response addProsecutionCaseToCrownCourtFirstHearing(final String caseId, final String defendantId, final boolean isYouth) throws IOException {
        return addProsecutionCaseToCrownCourtFirstHearing(caseId, defendantId, generateUrn(), isYouth);
    }

    public static Response addProsecutionCaseToMagsCourt(final String caseId, final String defendantId) throws IOException {
        return addProsecutionCaseToMagsCourt(caseId, defendantId, generateUrn());
    }

    public static Response addProsecutionCaseToCrownCourtNullPostCode(final String caseId, final String defendantId) throws IOException {
        return addProsecutionCaseToCrownCourtNullPostCode(caseId, defendantId, generateUrn());
    }

    public static Response referSJPCaseToMagsCourt(final String caseId, final String defendantId, final String courtCentreId) throws IOException {
        return referSJPCaseToMagsCourt(caseId, defendantId, generateUrn(), courtCentreId);
    }

    public static Response addProsecutionCaseToCrownCourtWithDefendantAsAdult(final String caseId, final String defendantId) throws IOException {
        return addProsecutionCaseToCrownCourtWithDefendantAsAdult(caseId, defendantId, generateUrn());
    }

    public static Response addProsecutionCaseToCrownCourt(final String caseId, final String defendantId, final String caseUrn) throws IOException {
        final JSONObject jsonPayload = new JSONObject(createReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId, randomUUID().toString(),
                randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), caseUrn));
        jsonPayload.getJSONObject("courtReferral").remove("courtDocuments");
        return postCommand(getWriteUrl("/refertocourt"),
                "application/vnd.progression.refer-cases-to-court+json",
                jsonPayload.toString());
    }

    public static Response addProsecutionCaseToCrownCourtFirstHearing(final String caseId, final String defendantId, final String caseUrn, final boolean isYouth) throws IOException {
        final JSONObject jsonPayload = new JSONObject(createReferProsecutionCaseToCrownCourtFirstHearingJsonBody(caseId, defendantId, randomUUID().toString(),
                randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), caseUrn, isYouth));
        jsonPayload.getJSONObject("courtReferral").remove("courtDocuments");
        return postCommand(getWriteUrl("/refertocourt"),
                "application/vnd.progression.refer-cases-to-court+json",
                jsonPayload.toString());
    }

    public static Response addProsecutionCaseToMagsCourt(final String caseId, final String defendantId, final String caseUrn) throws IOException {
        final JSONObject jsonPayload = new JSONObject(createReferProsecutionCaseToMagsCourtJsonBody(caseId, defendantId, randomUUID().toString(),
                randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), caseUrn));
        jsonPayload.getJSONObject("courtReferral").remove("courtDocuments");
        return postCommand(getWriteUrl("/refertocourt"),
                "application/vnd.progression.refer-cases-to-court+json",
                jsonPayload.toString());
    }

    public static Response addProsecutionCaseToMagsCourt(final String caseId, final String defendantId, final String caseUrn, final String postCode) throws IOException {
        final JSONObject jsonPayload = new JSONObject(createReferProsecutionCaseToMagsCourtJsonBody(caseId, defendantId, randomUUID().toString(), caseUrn, postCode));
        jsonPayload.getJSONObject("courtReferral").remove("courtDocuments");
        return postCommand(getWriteUrl("/refertocourt"),
                "application/vnd.progression.refer-cases-to-court+json",
                jsonPayload.toString());
    }

    public static Response referSJPCaseToMagsCourt(final String caseId, final String defendantId, final String caseUrn, final String courtCentreId) throws IOException {
        final JSONObject jsonPayload = new JSONObject(createReferSJPCaseToMagsCourtJsonBody(caseId, defendantId, randomUUID().toString(),
                randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), caseUrn, courtCentreId));
        jsonPayload.getJSONObject("courtReferral").remove("courtDocuments");
        return postCommand(getWriteUrl("/refertocourt"),
                "application/vnd.progression.refer-cases-to-court+json",
                jsonPayload.toString());
    }

    public static Response addDefenceCounsel(final String hearingId, final String defenceCounselId, final List<String> defendants, final List<String> attendanceDays, final String filePath) throws IOException {
        final JSONObject jsonPayload = new JSONObject(createDefenseCounselRequestJsonBody(hearingId, defenceCounselId, defendants, attendanceDays, filePath));
        return postCommand(getWriteUrl(format("/hearing/%s/defence-counsel", hearingId)),
                "application/vnd.progression.add-hearing-defence-counsel+json",
                jsonPayload.toString());
    }

    public static Response updateDefenceCounsel(final String hearingId, final String defenceCounselId, final List<String> defendants, final List<String> attendanceDays, final String filePath) throws IOException {
        final JSONObject jsonPayload = new JSONObject(createDefenseCounselRequestJsonBody(hearingId, defenceCounselId, defendants, attendanceDays, filePath));
        return postCommand(getWriteUrl(format("/hearing/%s/defence-counsel", hearingId)),
                "application/vnd.progression.update-hearing-defence-counsel+json",
                jsonPayload.toString());
    }

    public static Response removeDefenceCounsel(final String hearingId, final String defenceCounselId, final List<String> defendants, final List<String> attendanceDays, final String filePath) throws IOException {
        final JSONObject jsonPayload = new JSONObject(createDefenseCounselRequestJsonBody(hearingId, defenceCounselId, defendants, attendanceDays, filePath));
        return postCommand(getWriteUrl(format("/hearing/%s/defence-counsel", hearingId)),
                "application/vnd.progression.remove-hearing-defence-counsel+json",
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

    public static Response addProsecutionCaseToCrownCourtWithDefendantAsAdult(final String caseId, final String defendantId, final String caseUrn) throws IOException {
        final JSONObject jsonPayload = new JSONObject(
                createReferProsecutionCaseToCrownCourtWithDefendantAsAdult(
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

    public static Response initiateCourtProceedingsForLegalEntityDefendantMatching(final String caseId,
                                                                                   final String defendantId,
                                                                                   final String masterDefendantId,
                                                                                   final String materialIdOne,
                                                                                   final String materialIdTwo,
                                                                                   final String referralId,
                                                                                   final String listedStartDateTime, final String earliestStartDateTime) throws IOException {
        return postCommand(getWriteUrl("/initiatecourtproceedings"),
                "application/vnd.progression.initiate-court-proceedings+json",
                getInitiateCourtProceedingsJsonBodyForLegalEntityDefendantMatching(caseId, defendantId, masterDefendantId, materialIdOne, materialIdTwo, referralId, generateUrn(), listedStartDateTime, earliestStartDateTime));

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

    public static Response initiateCourtProceedings(final String commandPayload) throws IOException {
        return postCommand(getWriteUrl("/initiatecourtproceedings"), "application/vnd.progression.initiate-court-proceedings+json", commandPayload);

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

    public static Response initiateCourtProceedingsWithCommittingCourt(final String caseId, final String defendantId, final String listedStartDateTime, final String earliestStartDateTime) throws IOException {
        return postCommand(getWriteUrl("/initiatecourtproceedings"),
                "application/vnd.progression.initiate-court-proceedings+json",
                getInitiateCourtProceedingsWithCommittingCourtJsonBody(caseId, defendantId, listedStartDateTime, earliestStartDateTime));

    }

    public static Response initiateCourtProceedingsWithPoliceBailInfo(final String caseId, final String defendantId, final String listedStartDateTime, final String earliestStartDateTime, final String policeBailStatusId, final String policeBailStatusDesc, final String policeBailConditions) throws IOException {
        return postCommand(getWriteUrl("/initiatecourtproceedings"),
                "application/vnd.progression.initiate-court-proceedings+json",
                getInitiateCourtProceedingsWithPoliceBailInfoJsonBody(caseId, defendantId, listedStartDateTime, earliestStartDateTime, policeBailStatusId, policeBailStatusDesc, policeBailConditions));

    }

    public static Response initiateCourtProceedingsWithoutCourtDocument(final String caseId, final String defendantId,
                                                                        final String listedStartDateTime, final String earliestStartDateTime, final String dob) throws IOException {
        final JSONObject jsonPayload = new JSONObject(getInitiateCourtProceedingsJsonBody(caseId, defendantId, randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), generateUrn(), listedStartDateTime, earliestStartDateTime, dob));
        jsonPayload.getJSONObject("initiateCourtProceedings").remove("courtDocuments");
        return postCommand(getWriteUrl("/initiatecourtproceedings"),
                "application/vnd.progression.initiate-court-proceedings+json", jsonPayload.toString());
    }

    public static Response initiateCourtProceedingsWithoutCourtDocument(final String resourceLocation, final String caseId, final String defendantId,
                                                                        final String listedStartDateTime, final String earliestStartDateTime, final String dob) throws IOException {
        final JSONObject jsonPayload = new JSONObject(getInitiateCourtProceedingsJsonFromResource(resourceLocation, caseId, defendantId, randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), generateUrn(), listedStartDateTime, earliestStartDateTime, dob));
        jsonPayload.getJSONObject("initiateCourtProceedings").remove("courtDocuments");
        return postCommand(getWriteUrl("/initiatecourtproceedings"),
                "application/vnd.progression.initiate-court-proceedings+json", jsonPayload.toString());
    }



    public static Response initiateCourtProceedingsWithoutCourtDocumentAndCpsOrganisation(final String caseId, final String defendantId,
                                                                                          final String listedStartDateTime, final String earliestStartDateTime, final String dob) throws IOException {
        final JSONObject jsonPayload = new JSONObject(getInitiateCourtProceedingsJsonBody(caseId, defendantId, randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), generateUrn(), listedStartDateTime, earliestStartDateTime, dob));
        jsonPayload.getJSONObject("initiateCourtProceedings").remove("courtDocuments");
        jsonPayload.getJSONObject("initiateCourtProceedings").getJSONArray("prosecutionCases").getJSONObject(0).remove("cpsOrganisation");
        return postCommand(getWriteUrl("/initiatecourtproceedings"),
                "application/vnd.progression.initiate-court-proceedings+json", jsonPayload.toString());
    }

    public static Response initiateCourtProceedingsWithoutCourtDocument(final String caseId, final String defendantId) throws IOException {
        final String listedStartDateTime = ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z").toString();
        final String earliestStartDateTime = ZonedDateTimes.fromString("2019-05-30T18:32:04.238Z").toString();
        final String defendantDOB = LocalDate.now().minusYears(15).toString();
        return initiateCourtProceedingsWithoutCourtDocument(caseId, defendantId, listedStartDateTime, earliestStartDateTime, defendantDOB);
    }

    public static Response initiateCourtProceedingsWithoutCourtDocument(final String resource, final String caseId, final String defendantId) throws IOException {
        final String listedStartDateTime = ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z").toString();
        final String earliestStartDateTime = ZonedDateTimes.fromString("2019-05-30T18:32:04.238Z").toString();
        final String defendantDOB = LocalDate.now().minusYears(15).toString();
        return initiateCourtProceedingsWithoutCourtDocument(resource, caseId, defendantId, listedStartDateTime, earliestStartDateTime, defendantDOB);
    }

    public static Response initiateCourtProceedingsWithoutCourtDocumentAndCpsOrganisation(final String caseId, final String defendantId) throws IOException {
        final String listedStartDateTime = ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z").toString();
        final String earliestStartDateTime = ZonedDateTimes.fromString("2019-05-30T18:32:04.238Z").toString();
        final String defendantDOB = LocalDate.now().minusYears(15).toString();
        return initiateCourtProceedingsWithoutCourtDocumentAndCpsOrganisation(caseId, defendantId, listedStartDateTime, earliestStartDateTime, defendantDOB);
    }

    public static Response listNewHearing(final String caseId, final String defendantId) throws IOException {
        final JSONObject jsonPayload = new JSONObject(getListNewHearingJsonBody(caseId, defendantId));
        return postCommand(getWriteUrl("/listnewhearing"),
                "application/vnd.progression.list-new-hearing+json", jsonPayload.toString());
    }

    public static Response addProsecutionCaseToCrownCourtWithMinimumAttributes(final String caseId, final String defendantId) throws IOException {
        final JSONObject jsonPayload = new JSONObject(getReferProsecutionCaseToCrownCourtWithMinimumAttribute(caseId, defendantId, generateUrn()));
        jsonPayload.getJSONObject("courtReferral").remove("courtDocuments");
        return postCommand(getWriteUrl("/refertocourt"),
                "application/vnd.progression.refer-cases-to-court+json",
                jsonPayload.toString());
    }

    public static Response addProsecutionCaseToCrownCourtWithReportingRestrictions(final String caseId, final String defendantId, final String reportingRestrictionId) throws IOException {
        final JSONObject jsonPayload = new JSONObject(getReferProsecutionCaseToCrownCourtWithReportingRestrictions(caseId, defendantId, generateUrn(), reportingRestrictionId));
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

    private static String getDefendantPartialMatchJsonBody(final String matchedProsecutionCaseId, final String matchedDefendantId, final String matchedMasterDefendantId) {
        return getPayload("progression.match-defendant.json")
                .replace("PROSECUTION_CASE_ID", matchedProsecutionCaseId)
                .replace("DEFENDANT_ID", matchedDefendantId)
                .replace("MASTER_DEF_ID", matchedMasterDefendantId);
    }

    private static String getDefendantUnmatchJsonBody(final String matchedProsecutionCaseId, final String matchedDefendantId, final String matchedDefendantId2) {

        return getPayload("progression.unmatch-defendant.json")
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

    public static Response addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences1(final String caseId, final String defendantId) throws IOException {
        final JSONObject jsonPayload = new JSONObject(createReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId, randomUUID().toString(),
                randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), generateUrn(), "progression.command.prosecution-case-refer-to-court-one-defendant-two-offences1.json"));
        jsonPayload.getJSONObject("courtReferral").remove("courtDocuments");
        return postCommand(getWriteUrl("/refertocourt"),
                "application/vnd.progression.refer-cases-to-court+json",
                jsonPayload.toString());
    }

    public static Response addProsecutionCaseToCrownCourtWithOneGrownDefendantAndTwoOffences(final String caseId, final String defendantId) throws IOException {
        final JSONObject jsonPayload = new JSONObject(createReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId, randomUUID().toString(),
                randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), generateUrn(), "progression.command.prosecution-case-refer-to-court-one-grown-defendant-two-offences.json"));
        jsonPayload.getJSONObject("courtReferral").remove("courtDocuments");
        return postCommand(getWriteUrl("/refertocourt"),
                "application/vnd.progression.refer-cases-to-court+json",
                jsonPayload.toString());
    }

    public static Response sendCurrentOffencesToUpdateOffencesCommand (final String caseId, final String defendantId)  throws  IOException{
        final String jsonString = getPayload("progression.command.update-offences-for-prosecutioncase-after-defendant-dob-change.json")
                .replaceAll("DEFENDANT_ID",defendantId)
                .replaceAll("CASE_ID",caseId);
        final JSONObject jsonObjectPayload = new JSONObject(jsonString);
        final String request = jsonObjectPayload.toString();
       return postCommand(getWriteUrl("/prosecutioncases/" + caseId + "/defendants/" + defendantId), "application/vnd.progression.update-offences-for-prosecution-case+json", request);
    }

    public static Response addProsecutionCaseToCrownCourtWithOneYouthDefendantAndTwoOffences(final String caseId, final String defendantId) throws IOException {
        final JSONObject jsonPayload = new JSONObject(createReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId, randomUUID().toString(),
                randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), generateUrn(), "progression.command.prosecution-case-refer-to-court-one-youth-defendant-two-offences.json"));
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

    private static String getAddCaseToCrownCourtJsonBody(final String caseId, final String firstDefendantId, final String secondDefendantId) {
        return getPayload("progression.command.add-case-to-crown-court.json")
                .replace("RANDOM_CASE_ID", caseId)
                .replace("DEF_ID_1", firstDefendantId)
                .replace("DEF_ID_2", secondDefendantId)
                .replace("TODAY", LocalDate.now().toString());
    }

    public static String createReferProsecutionCaseToCrownCourtJsonBody(final String caseId, final String defendantId1, final String defendantId2,
                                                                        final String materialIdOne, final String materialIdTwo, final String courtDocumentId,
                                                                        final String referralId, final String caseUrn, final String filePath) {
        return getPayload(filePath)
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
                                                                        final String caseUrn, final String filePath, final String initialOffenceId1,
                                                                        final String initialOffenceId2, final String initialOffenceId3, final String initialOffenceId4) {
        return getPayload(filePath)
                .replace("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_REFERENCE", caseUrn)
                .replaceAll("RANDOM_DEFENDANT_ID", defendantId)
                .replace("RANDOM_DOC_ID", courtDocumentId)
                .replace("RANDOM_MATERIAL_ID_ONE", materialIdOne)
                .replace("RANDOM_MATERIAL_ID_TWO", materialIdTwo)
                .replace("RANDOM_REFERRAL_ID", referralId)
                .replace("INITIAL_OFFENCEID_1", initialOffenceId1)
                .replace("INITIAL_OFFENCEID_2", initialOffenceId2)
                .replace("INITIAL_OFFENCEID_3", initialOffenceId3)
                .replace("INITIAL_OFFENCEID_4", initialOffenceId4);
    }

    public static String createReferProsecutionCaseToCrownCourtJsonBody(final String caseId, final String defendantId, final String materialIdOne,
                                                                        final String materialIdTwo, final String courtDocumentId, final String referralId,
                                                                        final String caseUrn, final String filePath) {
        return getPayload(filePath)
                .replaceAll("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_REFERENCE", caseUrn)
                .replaceAll("RANDOM_DEFENDANT_ID", defendantId)
                .replace("RANDOM_DOC_ID", courtDocumentId)
                .replace("RANDOM_MATERIAL_ID_ONE", materialIdOne)
                .replace("RANDOM_MATERIAL_ID_TWO", materialIdTwo)
                .replace("RANDOM_REFERRAL_ID", referralId)
                .replace("RR_ORDERED_DATE", LocalDate.now().toString());
    }

    public static String createReferProsecutionCaseToMagsCourtJsonBody(final String caseId, final String defendantId, final String referralId,
                                                                        final String caseUrn, final String postCode, final String filePath) {
        String payload = null;
        payload = getPayload(filePath)
                    .replaceAll("RANDOM_CASE_ID", caseId)
                    .replace("RANDOM_REFERENCE", caseUrn)
                    .replaceAll("RANDOM_DEFENDANT_ID", defendantId)
                    .replace("RANDOM_REFERRAL_ID", referralId).toString();

        if(nonNull(postCode)) {
            payload =  payload.replace("POST_CODE", postCode);
        }
        return payload;
    }

    public static String createUpdateDefendantListingStatusJsonBody(final String hearingId,
                                                                    final String filePath) {
        return getPayload(filePath)
                .replace("RANDOM_HEARING_ID", hearingId);
    }

    public static String createReferProsecutionCaseToCrownCourtJsonBody(final String caseId1, final String caseId2, final String defendantId1, final String defendantId2,
                                                                        final String materialIdOne, final String materialIdTwo, final String courtDocumentId,
                                                                        final String referralId, final String caseUrn, final String filePath) {
        return getPayload(filePath)
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
                                                                         final String caseUrn) {
        return createReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId, materialIdOne,
                materialIdTwo, courtDocumentId, referralId, caseUrn, "progression.command.prosecution-case-refer-to-court.json");
    }

    private static String createReferProsecutionCaseToCrownCourtFirstHearingJsonBody(final String caseId, final String defendantId, final String materialIdOne,
                                                                         final String materialIdTwo, final String courtDocumentId, final String referralId,
                                                                         final String caseUrn, final boolean isYouth) {
        String dateOfBirth;
        if(isYouth) {
            dateOfBirth = LocalDate.now().minusYears(13).toString();
        } else {
            dateOfBirth = LocalDate.now().minusYears(30).toString();
        }

        return createReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId, materialIdOne,
                materialIdTwo, courtDocumentId, referralId, caseUrn, "progression.command.prosecution-case-refer-to-court-first-hearing.json")
                .replaceAll("DOB", dateOfBirth);
    }
    private static String createReferProsecutionCaseToMagsCourtJsonBody(final String caseId, final String defendantId, final String materialIdOne,
                                                                         final String materialIdTwo, final String courtDocumentId, final String referralId,
                                                                         final String caseUrn) {
        return createReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId, materialIdOne,
                materialIdTwo, courtDocumentId, referralId, caseUrn, "progression.sjp.case-refer-to-court-with-verdict.json");
    }

    private static String createReferProsecutionCaseToMagsCourtJsonBody(final String caseId, final String defendantId, final String referralId,
                                                                          final String caseUrn, final String postCode) {
        String filePath = "W1T 1JY".equals(postCode) ? "progression.case-disqualification-refer-to-court.json" : "progression.case-disqualification-refer-to-court-welsh.json";
        return createReferProsecutionCaseToMagsCourtJsonBody(caseId, defendantId, referralId, caseUrn, postCode, filePath);
    }

    private static String createReferSJPCaseToMagsCourtJsonBody(final String caseId, final String defendantId, final String materialIdOne,
                                                                        final String materialIdTwo, final String courtDocumentId, final String referralId,
                                                                        final String caseUrn, final String courtCentreId) {
        return createReferSJPCaseToCrownCourtJsonBody(caseId, defendantId, materialIdOne,
                materialIdTwo, courtDocumentId, referralId, caseUrn, courtCentreId, "progression.sjp.case-refer-to-court-with-next-hearing.json");
    }

    public static String createDefenseCounselRequestJsonBody(final String hearingId, final String defenseCounselId, final List<String> defendants, final List<String> attendanceDays, final String filePath) {
        return getPayload(filePath)
                .replace("HEARING_ID", hearingId)
                .replace("DEFENSE_COUNSEL_ID", defenseCounselId)
                .replace("\"DEFENDANT_LIST\"", convertToJsonList(defendants))
                .replace("\"ATTENDANCE_DAYS\"", convertToJsonList(attendanceDays));
    }

    private static String convertToJsonList(final List<String> stringList) {
        final String join = StringUtils.join(stringList, "\",\"");
        return StringUtils.wrap(join, "\"");
    }


    private static String createReferProsecutionCaseToCrownCourtJsonBodyNullPostCode(
            final String caseId,
            final String defendantId,
            final String materialIdOne,
            final String materialIdTwo, final String courtDocumentId,
            final String referralId,
            final String caseUrn) {

        return createReferProsecutionCaseToCrownCourtJsonBody(caseId,
                defendantId,
                materialIdOne,
                materialIdTwo,
                courtDocumentId,
                referralId,
                caseUrn,
                "progression.command.prosecution-case-refer-to-court-null-postcode.json");
    }

    private static String createReferProsecutionCaseToCrownCourtWithDefendantAsAdult(
            final String caseId,
            final String defendantId,
            final String materialIdOne,
            final String materialIdTwo, final String courtDocumentId,
            final String referralId,
            final String caseUrn) {

        return createReferProsecutionCaseToCrownCourtJsonBody(caseId,
                defendantId,
                materialIdOne,
                materialIdTwo,
                courtDocumentId,
                referralId,
                caseUrn,
                "progression.command.prosecution-case-refer-to-court-adult-defendant.json");
    }

    private static String getLAAReferenceForOffenceJsonBody(final String statusCode) {
        return getPayload("progression.command-record-laareference.json")
                .replace("RANDOM_STATUS_CODE", statusCode);
    }

    private static String getLAAReferenceForOffenceJsonBodyWithStatus(final String statusCode, final String statusDescription) {
        return getPayload("progression.command-record-laareference-with-status-description.json")
                .replace("RANDOM_STATUS_CODE", statusCode)
                .replace("RANDOM_STATUS_DESCRIPTION", statusDescription);
    }

    private static String getReceiveRepresentationOrderJsonBody(final String statusCode, final String laaContractNumber) {
        return getPayload("progression.command-receive-representationorder.json")
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
                                                                                          final String dob) {
        String payload = getPayload(resourceLocation)
                .replace("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_REFERENCE", caseUrn)
                .replace("RANDOM_DEFENDANT_ID", defendantId)
                .replace("RANDOM_MASTER_DEFENDANT_ID", masterDefendantId)
                .replaceAll("RANDOM_DEFENDANT_ID", defendantId)
                .replace("RANDOM_MATERIAL_ID_ONE", materialIdOne)
                .replace("RANDOM_MATERIAL_ID_TWO", materialIdTwo)
                .replace("RANDOM_REFERRAL_ID", referralId)
                .replace("LISTED_START_DATE_TIME", listedStartDateTime)
                .replace("EARLIEST_START_DATE_TIME", earliestStartDateTime);

        if (Objects.nonNull(dob)){
            payload = payload.replace("DOB", dob);
        }

        return payload;

    }

    private static String getInitiateCourtProceedingsJsonFromResourceForPartialOrExactMatchDefendant(final String resourceLocation,
                                                                                                     final String caseId,
                                                                                                     final String defendantId,
                                                                                                     final String caseReceivedDate) {
        return getPayload(resourceLocation)
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
                                                                      final String dob,
                                                                      final String policeBailStatusId, final String policeBailStatusDesc, final String policeBailConditions) {
        return getPayload(resourceLocation)
                .replace("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_REFERENCE", caseUrn)
                .replaceAll("RANDOM_DEFENDANT_ID", defendantId)
                .replace("RANDOM_MATERIAL_ID_ONE", materialIdOne)
                .replace("RANDOM_MATERIAL_ID_TWO", materialIdTwo)
                .replace("RANDOM_REFERRAL_ID", referralId)
                .replace("LISTED_START_DATE_TIME", listedStartDateTime)
                .replace("EARLIEST_START_DATE_TIME", earliestStartDateTime)
                .replace("DOB", dob)
                .replace("POLICE_BAIL_STATUS_ID", policeBailStatusId)
                .replace("POLICE_BAIL_STATUS_DESC", policeBailStatusDesc)
                .replaceAll("POLICE_BAIL_CONDITIONS", policeBailConditions);

    }

    private static String getInitiateCourtProceedingsJsonFromResource(final String resourceLocation, final String caseId, final String defendantId, final String materialIdOne,
                                                                      final String materialIdTwo,
                                                                      final String referralId, final String caseUrn,
                                                                      final String listedStartDateTime, final String earliestStartDateTime,
                                                                      final String dob) {
        return getPayload(resourceLocation)
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
                                                                      final String dob) {
        return getPayload(resourceLocation)
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
                                                              final String dob) {
        return getInitiateCourtProceedingsJsonFromResource("progression.command.initiate-court-proceedings.json", caseId,
                defendantId, materialIdOne, materialIdTwo, referralId, caseUrn, listedStartDateTime, earliestStartDateTime, dob);

    }

    private static String getInitiateCourtProceedingsJsonBodyForDefendantMatching(final String caseId, final String defendantId, final String masterDefendantId, final String materialIdOne,
                                                                                  final String materialIdTwo,
                                                                                  final String referralId, final String caseUrn,
                                                                                  final String listedStartDateTime, final String earliestStartDateTime,
                                                                                  final String dob) {
        return getInitiateCourtProceedingsJsonFromResourceForDefendantMatching("progression.command.initiate-court-proceedings-for-defendant-matching.json", caseId,
                defendantId, masterDefendantId, materialIdOne, materialIdTwo, referralId, caseUrn, listedStartDateTime, earliestStartDateTime, dob);

    }

    private static String getInitiateCourtProceedingsJsonBodyForLegalEntityDefendantMatching(final String caseId, final String defendantId, final String masterDefendantId, final String materialIdOne,
                                                                                  final String materialIdTwo,
                                                                                  final String referralId, final String caseUrn,
                                                                                  final String listedStartDateTime, final String earliestStartDateTime) {
        return getInitiateCourtProceedingsJsonFromResourceForDefendantMatching("progression.command.initiate-court-proceedings-for-defendant-matching-legal-entity.json", caseId,
                defendantId, masterDefendantId, materialIdOne, materialIdTwo, referralId, caseUrn, listedStartDateTime, earliestStartDateTime, null);

    }

    private static String getInitiateCourtProceedingsJsonBodyForPartialOrExactMatching(final String caseId, final String defendantId, final String caseReceivedDate) {
        return getInitiateCourtProceedingsJsonFromResourceForPartialOrExactMatchDefendant("progression.command.initiate-court-proceedings-for-partial-or-exact-match-defendants.json",
                caseId, defendantId, caseReceivedDate);
    }

    private static String getInitiateCourtProceedingsJsonBodyForExactMatching(final String caseId, final String defendantId, final String caseReceivedDate, final String channel) {
        String jsonString = getInitiateCourtProceedingsJsonFromResourceForPartialOrExactMatchDefendant("progression.command.initiate-court-proceedings-for-exact-match-defendants.json",
                caseId, defendantId, caseReceivedDate);
        if (channel.equalsIgnoreCase("CPPI")) {
            jsonString = getInitiateCourtProceedingsJsonFromResourceForPartialOrExactMatchDefendant("progression.command.initiate-court-proceedings-for-exact-match-defendants-2.json",
                    caseId, defendantId, caseReceivedDate);
        }
        return jsonString;
    }


    private static String getInitiateCourtProceedingsJsonBodyForPartialMatching(final String caseId, final String defendantId, final String caseReceivedDate, final String channel) {
        String jsonString = getInitiateCourtProceedingsJsonFromResourceForPartialOrExactMatchDefendant("progression.command.initiate-court-proceedings-for-partial-match-defendants.json",
                caseId, defendantId, caseReceivedDate);
        if (channel.equalsIgnoreCase("CPPI")) {
            jsonString = getInitiateCourtProceedingsJsonFromResourceForPartialOrExactMatchDefendant("progression.command.initiate-court-proceedings-for-partial-match-defendants-2.json",
                    caseId, defendantId, caseReceivedDate);
        }
        return jsonString;
    }

    private static String getInitiateCourtProceedingsWithCommittingCourtJsonBody(final String caseId, final String defendantId, final String listedStartDateTime, final String earliestStartDateTime) {
        return getInitiateCourtProceedingsJsonFromResource("progression.command.initiate-court-proceedings-with-committing-court.json", caseId,
                defendantId, randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), generateUrn(),
                listedStartDateTime, earliestStartDateTime, LocalDate.now().minusYears(15).toString());
    }

    private static String getInitiateCourtProceedingsWithPoliceBailInfoJsonBody(final String caseId, final String defendantId, final String listedStartDateTime, final String earliestStartDateTime, final String policeBailStatusId, final String policeBailStatusDesc, final String policeBailConditions) {
        return getInitiateCourtProceedingsJsonFromResource("progression.command.initiate-court-proceedings-with-police-bail-info.json", caseId,
                defendantId, randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), generateUrn(),
                listedStartDateTime, earliestStartDateTime, LocalDate.now().minusYears(15).toString(),
                policeBailStatusId, policeBailStatusDesc, policeBailConditions);
    }

    private static String getListNewHearingJsonFromResource(final String resourceLocation, final String caseId, final String defendantId) {
        return getPayload(resourceLocation)
                .replaceAll("RANDOM_CASE_ID", caseId)
                .replaceAll("RANDOM_DEFENDANT_ID", defendantId);

    }

    private static String getListNewHearingJsonBody(final String caseId, final String defendantId) {
        return getListNewHearingJsonFromResource("progression.list-new-hearing.json", caseId, defendantId);

    }

    private static String getReferProsecutionCaseToCrownCourtWithMinimumAttribute(final String caseId, final String defendantId, final String caseUrn) {
        return getPayload("progression.command.prosecution-case-refer-to-court-minimal-payload.json")
                .replace("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_REFERENCE", caseUrn)
                .replaceAll("RANDOM_DEFENDANT_ID", defendantId);
    }

    private static String getReferProsecutionCaseToCrownCourtWithReportingRestrictions(final String caseId, final String defendantId, final String caseUrn, final String reportingRestrictionId) throws IOException {
        return Resources.toString(getResource("progression.command.prosecution-case-refer-to-court-with-reporting-restrictions.json"), Charset.defaultCharset())
                .replace("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_REFERENCE", caseUrn)
                .replace("RANDOM_RR_ID", reportingRestrictionId)
                .replaceAll("RANDOM_DEFENDANT_ID", defendantId);
    }

    private static String getReferProsecutionCaseToCourtWitCommittingCourt(final String caseId, final String defendantId, final String caseUrn) throws IOException {
        return Resources.toString(getResource("progression.command.prosecution-case-refer-to-court-with-committing-court.json"), Charset.defaultCharset())
                .replace("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_REFERENCE", caseUrn)
                .replaceAll("RANDOM_DEFENDANT_ID", defendantId);
    }

    public static String getProsecutionCaseDefendantUpdatedEvent(final String caseId, final String defendantId,
                                                                 final String caseUrn,
                                                                 final String filePath) {
        return getPayload(filePath)
                .replace("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_REFERENCE", caseUrn)
                .replaceAll("RANDOM_DEFENDANT_ID", defendantId);
    }

    // Progression Test DSL for preconditions and assertions
    public static void givenCaseAddedToCrownCourt(final String caseId, final String firstDefendantId, final String secondDefendantId) throws IOException {
        final Response writeResponse = addCaseToCrownCourt(caseId, firstDefendantId, secondDefendantId);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
    }

    public static String pollProsecutionCasesProgressionFor(final String caseId) {
        return pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.id", equalTo(caseId)));
    }


    public static void verifyInMessagingQueueForHearingPopulatedToProbationCaseWorker(final String hearingId, final MessageConsumer messageConsumerHearingPopulatedToProbationCaseWorker) {
        final JsonPath message = QueueUtil.retrieveMessage(messageConsumerHearingPopulatedToProbationCaseWorker, allOf(isJson(withJsonPath("$.hearing.id", CoreMatchers.is(hearingId)))));
        assertNotNull(message);
    }

    public static void verifyInMessagingQueueForHearingPopulatedToProbationCaseWorker(final String hearingId, final boolean isYouth, final MessageConsumer messageConsumerHearingPopulatedToProbationCaseWorker) {
        if (isYouth) {
            Optional<JsonPath> message;
            do {
                message = QueueUtil.retrieveMessage(messageConsumerHearingPopulatedToProbationCaseWorker, 1000L);
            } while (message.isPresent() && !message.get().getString("hearing.id").equals(hearingId));

            assertFalse("defendant is youth but event raised : " + hearingId, message.isPresent());
        } else {
            final JsonPath message = QueueUtil.retrieveMessage(messageConsumerHearingPopulatedToProbationCaseWorker, allOf(isJson(withJsonPath("$.hearing.id", CoreMatchers.is(hearingId)))));
            assertNotNull(message);
        }
    }

    public static String getCaseLsmInfoFor(final String caseId, final Matcher<? super ReadContext>[] matchers) {
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

    public static String getHearingForDefendant(final String hearingId, final Matcher<? super ReadContext>[] matchers) {
        return poll(requestParams(getReadUrl("/hearingSearch/" + hearingId), "application/vnd.progression.query.hearing+json").withHeader(USER_ID, UUID.randomUUID()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                matchers
                        ))).getPayload();
    }

    public static void verifyHearingIsEmpty(final String hearingId) {
        poll(requestParams(getReadUrl("/hearingSearch/" + hearingId), "application/vnd.progression.query.hearing+json").withHeader(USER_ID, UUID.randomUUID()))
                .until(
                        status().is(OK),
                        payload().isJson(
                                withJsonPath(".$", emptyCollection())
                        ));
    }

    @SafeVarargs
    public static String pollCasesProgressionFor(final String caseId, final Matcher<? super ReadContext>... matchers) {
        return pollForResponse("/prosecutioncases/" + caseId, "application/vnd.progression.query.case+json", matchers);
    }

    @SafeVarargs
    public static String pollProsecutionCasesProgressionFor(final String caseId, final Matcher<? super ReadContext>... matchers) {
        return pollForResponse("/prosecutioncases/" + caseId, "application/vnd.progression.query.prosecutioncase+json", matchers);
    }

    @SafeVarargs
    public static String pollProsecutionCasesProgressionForCAAG(final String caseId, final Matcher<? super ReadContext>... matchers) {
        return pollForResponse("/prosecutioncases/" + caseId, "application/vnd.progression.query.prosecutioncase.caag+json", matchers);
    }

    @SafeVarargs
    public static String pollForCotrTrialHearings(final String caseId, final Matcher<? super ReadContext>... matchers) {
        return pollForResponse("/prosecutioncases/" + caseId + "/cotr-trial-hearings", "application/vnd.progression.query.cotr-trial-hearings+json", matchers);
    }

    @SafeVarargs
    public static String pollForCotrDetails(final String caseId, final Matcher<? super ReadContext>... matchers) {
        return pollForResponse("/prosecutioncases/" + caseId + "/cotr-details", "application/vnd.progression.query.cotr-details+json", matchers);
    }

    @SafeVarargs
    public static String pollProsecutionCasesProgressionAndReturnHearingId(final String caseId, final String defendantId, final Matcher<? super ReadContext>... matchers) {
        final String prosecutionCaseAsString = pollProsecutionCasesProgressionFor(caseId, matchers);
        final JsonObject prosecutionCaseJson = getJsonObject(prosecutionCaseAsString);
        return extractHearingIdFromProsecutionCasesProgression(prosecutionCaseJson, defendantId);
    }

    public static String extractHearingIdFromProsecutionCasesProgression(final JsonObject prosecutionCaseJson, final String defendantId) {
        final Optional<JsonValue> defendantHearing = prosecutionCaseJson.getJsonObject("hearingsAtAGlance")
                .getJsonArray("defendantHearings")
                .stream().filter(def1 -> ((JsonObject) def1).getString("defendantId").equals(defendantId))
                .findFirst();

        return ((JsonObject) defendantHearing.get()).getJsonArray("hearingIds").get(0).toString().replaceAll("\"", "");
    }

    @SafeVarargs
    public static String pollPartialMatchDefendantFor(final Matcher<? super ReadContext>... matchers) {
        return pollForResponse("/partial-match-defendants?sortOrder=DESC&sortField=caseReceivedDate", "application/vnd.progression.query.partial-match-defendant+json", matchers);
    }

    public static String getApplicationFor(final String applicationId) {
        return pollForResponse(join("", "/applications/", applicationId), "application/vnd.progression.query.application+json");
    }

    public static void verifyCasesForSearchCriteria(final String searchCriteria, final Matcher<? super ReadContext>[] matchers) {
        poll(requestParams(getReadUrl(join("", "/search?q=", searchCriteria)), "application/vnd.progression.query.search-cases+json").withHeader(USER_ID, UUID.randomUUID()))
                .timeout(RestHelper.TIMEOUT, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                matchers
                        ))).getPayload();
    }

    public static void verifyCasesByCaseUrn(final String caseUrn, final Matcher<? super ReadContext>[] matchers) {
        poll(requestParams(getReadUrl(join("", "/search?caseUrn=", caseUrn)), "application/vnd.progression.query.search-cases-by-caseurn+json").withHeader(USER_ID, UUID.randomUUID()))
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
        return pollForResponse(MessageFormat.format("/courtdocumentsearch?caseId={0}", caseId), "application/vnd.progression.query.courtdocuments+json", userId);
    }

    public static String getCourtDocumentsPerCase(final String userId, final String... caseId) {
        return pollForResponse(MessageFormat.format("/courtdocumentsearch?caseId={0}", caseId), "application/vnd.progression.query.courtdocuments-all+json", userId);
    }

    public static String getCourtDocumentsPerCase(final String userId, final String caseId, final Matcher[] matchers) {
        return pollForResponse(MessageFormat.format("/courtdocumentsearch?caseId={0}", caseId), "application/vnd.progression.query.courtdocuments-all+json", userId, matchers);
    }

    public static String getUploadCourtDocumentsByCase(final String userId, final String caseId) {
        return pollForResponse(MessageFormat.format("/courtdocumentsearch?caseId={0}", caseId), "application/vnd.progression.query.courtdocuments+json", userId, status().is(OK), withJsonPath("$.documentIndices[0].caseIds[0]", CoreMatchers.is(caseId)));
    }

    public static String getCourtDocumentsByCaseWithMatchers(final String userId, final String caseDocumentId, final String caseId) {
        final Matcher[] matchers = {
                withJsonPath("$.documentIndices[0].document.courtDocumentId", is(caseDocumentId))
        };
        return getCourtDocumentsByCaseWithMatchers(userId, caseId, matchers);
    }

    public static String pollForSearchTrialReadiness(final String courtCentreId, final String startDate, final String endDate, final String trailWithOverdueDirection) {
        return pollForResponse(MessageFormat.format("/search-trial-readiness?courtCentreId={0}&startDate={1}&endDate={2}&trailWithOverdueDirection={3}", courtCentreId, startDate, endDate, trailWithOverdueDirection), "application/vnd.progression.query.search-trial-readiness+json");
    }

    public static String pollForSearchTrialReadinessByCluster(final String clusterId, final String startDate, final String endDate) {
        return pollForResponse(MessageFormat.format("/search-trial-readiness?clusterId={0}&startDate={1}&endDate={2}", clusterId, startDate, endDate), "application/vnd.progression.query.search-trial-readiness+json");
    }

    public static String pollForGetTrialReadinessHearingDetails(final String hearingId) {
        return pollForResponse(MessageFormat.format("/trial-readiness-hearings/{0}", hearingId), "application/vnd.progression.query.trial-readiness-details+json");
    }

    public static String getCourtDocumentsByCaseWithMatchers(final String userId, final String caseId, final Matcher[] matchers) {
        return pollForResponse(MessageFormat.format("/courtdocumentsearch?caseId={0}", caseId), "application/vnd.progression.query.courtdocuments+json", userId, matchers);
    }

    public static String getCourtDocumentsByDefendant(final String userId, final String defendantId) {
        return pollForResponse(MessageFormat.format("/courtdocumentsearch?defendantId={0}", defendantId), "application/vnd.progression.query.courtdocuments+json", userId);
    }

    public static String getCourtDocumentsByApplication(final String userId, final String applicationId) {
        return pollForResponse(MessageFormat.format("/courtdocumentsearch?applicationId={0}", applicationId), "application/vnd.progression.query.courtdocuments+json", userId);
    }

    public static String getCourtDocumentsByApplication(final String userId, final String applicationId, final Matcher[] matchers) {
        return pollForResponse(MessageFormat.format("/courtdocumentsearch?applicationId={0}", applicationId), "application/vnd.progression.query.courtdocuments+json", userId, matchers);
    }

    public static String getCourtDocumentsByCaseAndDefendant(final String userId, final String caseId, final String defendantId) {
        return pollForResponse(MessageFormat.format("/courtdocumentsearch?caseId={0}&defendantId={1}", caseId, defendantId), "application/vnd.progression.query.courtdocuments+json", userId);
    }

    public static javax.ws.rs.core.Response getMaterialContent(final UUID materialId, final UUID userId) {
        return getMaterialContentResponse("/material/" + materialId.toString() + "/content", userId, "application/vnd.progression.query.material-content+json");

    }

    public static javax.ws.rs.core.Response getMaterialContent(final UUID materialId, final UUID userId, final UUID defendantId) {
        return getMaterialContentResponse("/material/" + materialId.toString() + "/content?defendantId=" + defendantId, userId, "application/vnd.progression.query.material-content-for-defence+json");

    }

    public static String getCourtDocumentsByDefendantForDefenceWithNoCaseAndDefenceId(final String userId, final ResponseStatusMatcher responseStatusMatcher) {
        return pollForResponse("/courtdocumentsearch", "application/vnd.progression.query.courtdocuments.for.defence+json", userId, responseStatusMatcher);
    }

    public static String getCourtDocumentsByDefendantForDefenceWithNoCaseId(final String userId, final String defendantId, final ResponseStatusMatcher responseStatusMatcher) {
        return pollForResponse(MessageFormat.format("/courtdocumentsearch?defendantId={0}", defendantId), "application/vnd.progression.query.courtdocuments.for.defence+json", userId, responseStatusMatcher);
    }

    public static String getCourtDocumentsByDefendantForDefence(final String userId, final String caseId, final ResponseStatusMatcher responseStatusMatcher) {
        return pollForResponse(MessageFormat.format("/courtdocumentsearch?caseId={0}", caseId), "application/vnd.progression.query.courtdocuments.for.defence+json", userId, responseStatusMatcher);
    }

    public static String getCourtDocumentsByCaseIdForProsecutionWithNoCaseId(final String userId, final ResponseStatusMatcher responseStatusMatcher) {
        return pollForResponse("/courtdocumentsearch", "application/vnd.progression.query.courtdocuments.for.prosecution+json", userId, responseStatusMatcher);
    }

    public static String getCourtDocumentsByCaseIdForProsecution(final String userId, final String caseId, final ResponseStatusMatcher responseStatusMatcher) {
        return pollForResponse(MessageFormat.format("/courtdocumentsearch?caseId={0}", caseId), "application/vnd.progression.query.courtdocuments.for.prosecution+json", userId, responseStatusMatcher);
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
                                                                      final Boolean applicantAppellantFlag,
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
                                                                      final String respondentDefendantId,
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
                                                                      final String prosecutionCaseId,
                                                                      final String prosecutionAuthorityId,
                                                                      final String prosecutionAuthorityCode,
                                                                      final String prosecutionAuthorityReference,
                                                                      final String parentApplicationId,
                                                                      final String fileName)
            throws IOException {
        final String body = getPayload(fileName)
                .replace("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_APPLICATION_ID", applicationId)
                .replace("RANDOM_PARENT_APPLICATION_ID", parentApplicationId)
                .replaceAll("RANDOM_PARTICULARS", particulars)
                .replaceAll("\"applicationReceivedDate\": \"2019-01-01\"", format("\"applicationReceivedDate\": \"%s\"", applicantReceivedDate))
                .replaceAll("RANDOM_APPLICATION_TYPE", applicationType)
                .replaceAll("\"RANDOM_APPLICATION_APPEAL\"", appeal.toString())
                .replaceAll("\"RANDOM_APPLICATION_APPEALLANT_FLAG\"", applicantAppellantFlag.toString())
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
                .replaceAll("RANDOM_RESPONDENT_DEFENDANT_ID", respondentDefendantId)
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
                .replaceAll("RANDOM_PROSECUTION_CASE_ID", prosecutionCaseId)
                .replaceAll("RANDOM_PROSECUTION_AUTHORITY_ID", prosecutionAuthorityId)
                .replaceAll("RANDOM_PROSECUTION_AUTHORITY_CODE", prosecutionAuthorityCode)
                .replaceAll("RANDOM_PROSECUTION_AUTHORITY_REFERENCE", prosecutionAuthorityReference)
                .replaceAll("RANDOM_REFERENCE", applicationReference);

        LOGGER.info("applicationId={}, parentApplicationId={}, body={}", applicationId, parentApplicationId, body);
        return postCommand(getWriteUrl("/initiate-application"),
                "application/vnd.progression.initiate-court-proceedings-for-application+json", body);

    }

    public static Response sendNotification(final String caseId,
                                                                      final String applicationId,
                                                                      final String particulars,
                                                                      final String applicantReceivedDate,
                                                                      final String applicationType,
                                                                      final Boolean appeal,
                                                                      final Boolean applicantAppellantFlag,
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
                                                                      final String prosecutionCaseId,
                                                                      final String prosecutionAuthorityId,
                                                                      final String prosecutionAuthorityCode,
                                                                      final String prosecutionAuthorityReference,
                                                                      final String parentApplicationId,
                                                                      final String fileName,
                                                                      final Boolean isBoxWorkRequest,
                                                                      final Boolean isWelshTranslationRequired)
            throws IOException {
        final String body = getPayload(fileName)
                .replace("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_APPLICATION_ID", applicationId)
                .replace("RANDOM_PARENT_APPLICATION_ID", parentApplicationId)
                .replaceAll("RANDOM_PARTICULARS", particulars)
                .replaceAll("\"applicationReceivedDate\": \"2019-01-01\"", format("\"applicationReceivedDate\": \"%s\"", applicantReceivedDate))
                .replaceAll("RANDOM_APPLICATION_TYPE", applicationType)
                .replaceAll("\"RANDOM_APPLICATION_APPEAL\"", appeal.toString())
                .replaceAll("\"RANDOM_APPLICATION_APPEALLANT_FLAG\"", applicantAppellantFlag.toString())
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
                .replaceAll("RANDOM_PROSECUTION_CASE_ID", prosecutionCaseId)
                .replaceAll("RANDOM_PROSECUTION_AUTHORITY_ID", prosecutionAuthorityId)
                .replaceAll("RANDOM_PROSECUTION_AUTHORITY_CODE", prosecutionAuthorityCode)
                .replaceAll("RANDOM_PROSECUTION_AUTHORITY_REFERENCE", prosecutionAuthorityReference)
                .replaceAll("RANDOM_REFERENCE", applicationReference)
                .replaceAll("IS_BOXWORK_REQUEST", isBoxWorkRequest.toString())
                .replaceAll("IS_WELSH_TRANSLATION_REQUIRED", isWelshTranslationRequired.toString());

        LOGGER.info("applicationId={}, parentApplicationId={}, body={}", applicationId, parentApplicationId, body);
        return postCommand(getWriteUrl("/send-notification-for-application"),
                "application/vnd.progression.send-notification-for-application+json", body);

    }

    public static Response addCourtApplicationForIngestion(final String caseId,
                                                           final String applicationId,
                                                           final String applicantId,
                                                           final String applicantDefendantId,
                                                           final String respondentId,
                                                           final String respondentDefendantId,
                                                           final String applicationReference,
                                                           final String applicationStatus,
                                                           final String fileName)
            throws IOException {
        final String body = getPayload(fileName)
                .replaceAll("RANDOM_APPLICATION_ID", applicationId)
                .replaceAll("RANDOM_CASE_ID", caseId)
                .replaceAll("RANDOM_APPLICANT_ID", applicantId)
                .replaceAll("RANDOM_APPLICANT_DEFENDANT_ID", applicantDefendantId)
                .replaceAll("RANDOM_RESPONDANT_ID", respondentId)
                .replaceAll("RANDOM_RESPONDANT_DEFENDANT_ID", respondentDefendantId)
                .replaceAll("APPLICATION_STATUS", applicationStatus)
                .replaceAll("RANDOM_REFERENCE", applicationReference);

        LOGGER.info(body);
        return postCommand(getWriteUrl("/initiate-application"),
                "application/vnd.progression.initiate-court-proceedings-for-application+json", body);
    }

    public static Response addCourtApplicationForIngestion(final String caseId,
                                                           final String applicationId,
                                                           final String applicantId,
                                                           final String applicantDefendantId,
                                                           final String respondentId,
                                                           final String respondentDefendantId,
                                                           final String applicationStatus,
                                                           final String fileName) throws IOException {
        return addCourtApplicationForIngestion(caseId, applicationId, applicantId, applicantDefendantId,
                respondentId, respondentDefendantId, randomUUID().toString(), applicationStatus, fileName);
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
        final String body = getPayload(fileName)
                .replaceAll("RANDOM_CASE_ID", caseId)
                .replaceAll("RANDOM_APPLICATION_ID", applicationId)
                .replaceAll("RANDOM_APPLICANT_ID", applicantId)
                .replaceAll("RANDOM_APPLICANT_DEFENDANT_ID", applicantDefendantId)
                .replaceAll("RANDOM_RESPONDANT_ID", respondantId)
                .replaceAll("RANDOM_RESPONDANT_DEFENDANT_ID", respondantDefendantId)
                .replaceAll("RANDOM_REFERENCE", applicationReference);

        return postCommand(getWriteUrl("/initiate-application"),
                "application/vnd.progression.edit-court-proceedings-for-application+json", body);
    }

    public static Response updateCourtApplication(final String applicationId, final String applicantId, final String caseId, final String defendantId, final String hearingId, final String fileName) throws IOException {
        return postCommand(getWriteUrl("/initiate-application"),
                "application/vnd.progression.edit-court-proceedings-for-application+json",
                getUpdateCourtApplicationJsonBody(applicationId, applicantId, caseId, defendantId, hearingId, fileName));
    }

    public static Response addStandaloneCourtApplication(final String applicationId, final String parentApplicationId, final CourtApplicationRandomValues randomValues, final String fileName) throws IOException {
        final String payload = getStandaloneCourtApplicationJsonBody(applicationId, parentApplicationId, generateUrn(), randomValues, fileName);
        return postCommand(getWriteUrl("/initiate-application"),
                "application/vnd.progression.initiate-court-proceedings-for-application+json",
                payload);
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

    private static String getStandaloneCourtApplicationJsonBody(final String applicationId, final String parentApplicationId, final String applicationReference, final CourtApplicationRandomValues randomValues, final String fileName) {

        return getPayload(fileName)
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

    private static String getCourtApplicationJsonBody(final String caseId, final String applicationId, final String applicationReference, final String fileName) {
        return getPayload(fileName)
                .replace("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_APPLICATION_ID", applicationId)
                .replace("RANDOM_REFERENCE", applicationReference);
    }

    private static String getCourtApplicationJsonBody(final String caseId, final String defendantId, final String applicationId, final String applicationReference, final String fileName) {
        return getPayload(fileName)
                .replace("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_DEFENDANT_ID", defendantId)
                .replace("RANDOM_APPLICATION_ID", applicationId)
                .replace("RANDOM_REFERENCE", applicationReference);
    }

    private static String getShareCourtDocumentJsonBody(final String courtDocumentId, final String hearingId, final String userGroup, final String fileName) {
        return getPayload(fileName)
                .replace("COURT_DOCUMENT_ID", courtDocumentId)
                .replace("HEARING_ID", hearingId)
                .replace("USER_GROUP", userGroup);
    }

    private static String getLSMCasesJsonBody(final String prosecutionCaseId1, final String caseUrn2, final String caseUrn3, final String fileName) {
        return getPayload(fileName)
                .replace("PROSECUTION_CASE_ID", prosecutionCaseId1)
                .replace("CASE_URN2", caseUrn2)
                .replace("CASE_URN3", caseUrn3);
    }

    private static String getUnlinkCasesJsonBody(final String prosecutionCaseId, final String prosecutionCaseUrn, final String caseId, final String caseUrn, final String linkGroupId, final String fileName) {
        return getPayload(fileName)
                .replace("PROSECUTION_CASE_ID", prosecutionCaseId)
                .replace("PROSECUTION_CASE_URN", prosecutionCaseUrn)
                .replace("CASE_ID", caseId)
                .replace("CASE_URN", caseUrn)
                .replace("LINK_GROUP_ID", linkGroupId);
    }

    private static String getLSMCasesJsonBody(final String prosecutionCaseId1, final String caseUrn2, final String fileName) {
        return getPayload(fileName)
                .replace("PROSECUTION_CASE_ID", prosecutionCaseId1)
                .replace("CASE_URN2", caseUrn2);
    }

    private static String getCourtApplicationWithDefendantJsonBody(final String caseId, final String applicationId, final String defendantId, final String applicationReference, final String fileName) {
        return getPayload(fileName)
                .replace("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_APPLICATION_ID", applicationId)
                .replace("RANDOM_DEFENDANT_ID", defendantId)
                .replace("RANDOM_REFERENCE", applicationReference);
    }

    private static String getUpdateCourtApplicationJsonBody(final String applicationId, final String applicantId, final String caseId, final String defendantId, final String fileName) {
        return getPayload(fileName)
                .replace("APPLICATION_ID", applicationId)
                .replace("RANDOM_CASE_ID", caseId)
                .replaceAll("RANDOM_DEFENDANT_ID", defendantId)
                .replace("APPLICANT_ID", applicantId);
    }

    private static String getUpdateCourtApplicationJsonBody(final String applicationId, final String applicantId, final String caseId, final String defendantId, final String hearingId, final String fileName) {
        return getPayload(fileName)
                .replace("APPLICATION_ID", applicationId)
                .replace("RANDOM_CASE_ID", caseId)
                .replaceAll("RANDOM_DEFENDANT_ID", defendantId)
                .replace("APPLICANT_ID", applicantId)
                .replace("HEARING_ID", hearingId);
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
                                                               final String removalReason, final String fileName) {
        return getPayload(fileName)
                .replace("RANDOM_APPLICATION_ID", applicationId)
                .replace("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_REMOVAL_REASON", removalReason);
    }

    @SafeVarargs
    public static String getCourtDocumentFor(final String courtDocumentId, final Matcher<? super ReadContext>... matchers) {
        return poll(requestParams(getReadUrl(join("", "/courtdocuments/", courtDocumentId)), "application/vnd.progression.query.courtdocument+json").withHeader(USER_ID, UUID.randomUUID()))
                .timeout(160, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                matchers
                        ))).getPayload();
    }

    @SafeVarargs
    public static String verifyQueryResultsForbidden(final String courtDocumentId, final String userId, final Matcher<? super ReadContext>... matchers) {
        return poll(requestParams(getReadUrl(join("", "/courtdocuments/", courtDocumentId)), "application/vnd.progression.query.courtdocument+json")
                .withHeader(USER_ID, userId))
                .timeout(40, TimeUnit.SECONDS)
                .until(
                        status().is(FORBIDDEN), payload().isJson(allOf(
                                matchers))).getPayload();
    }

    private static String getReferApplicationToCourtJsonBody(final String applicationId, final String caseId, final String hearingId, final String fileName) {
        return getPayload(fileName)
                .replace("RANDOM_APPLICATION_ID", applicationId)
                .replaceAll("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_HEARING_ID", hearingId);

    }

    private static String getBookSlotsForApplicationJsonBody(final String applicationId, final String hearingRequestId, final String caseId, final String applicationReference, final String fileName) {
        return getPayload(fileName)
                .replaceAll("RANDOM_APPLICATION_ID", applicationId)
                .replaceAll("RANDOM_HEARING_ID", hearingRequestId)
                .replaceAll("RANDOM_CASE_ID", caseId)
                .replaceAll("RANDOM_REFERENCE", applicationReference);

    }

    private static String getExtendHearingJsonBody(final String applicationId, final String hearingId, final String fileName) {
        return getPayload(fileName)
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
                .timeout(TIMEOUT, TimeUnit.SECONDS)
                .until(status().is(OK));

    }

    @SafeVarargs
    public static void pollForApplication(final String applicationId, final Matcher<? super ReadContext>... matchers) {
        poll(requestParams(getReadUrl("/applications/" + applicationId),
                "application/vnd.progression.query.application+json").withHeader(USER_ID, randomUUID()))
                .until(status().is(OK),
                        payload().isJson(allOf(matchers)
                        ));

    }

    public static String createReferSJPCaseToCrownCourtJsonBody(final String caseId, final String defendantId, final String materialIdOne,
                                                                        final String materialIdTwo, final String courtDocumentId, final String referralId,
                                                                        final String caseUrn, final String courtCentreId, final String filePath) {
        return getPayload(filePath)
                .replaceAll("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_REFERENCE", caseUrn)
                .replaceAll("RANDOM_DEFENDANT_ID", defendantId)
                .replace("RANDOM_COURT_CENTRE_ID", courtCentreId)
                .replace("RANDOM_DOC_ID", courtDocumentId)
                .replace("RANDOM_MATERIAL_ID_ONE", materialIdOne)
                .replace("RANDOM_MATERIAL_ID_TWO", materialIdTwo)
                .replace("RANDOM_REFERRAL_ID", referralId)
                .replace("RR_ORDERED_DATE", LocalDate.now().toString());
    }

}
