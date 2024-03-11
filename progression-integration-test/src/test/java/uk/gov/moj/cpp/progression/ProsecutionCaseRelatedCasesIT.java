package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedingsForDefendantMatching;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.matchDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonObject;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.stub.HearingStub.stubInitiateHearing;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.AwaitUtil;
import uk.gov.moj.cpp.progression.helper.RestHelper;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ProsecutionCaseRelatedCasesIT extends AbstractIT {
    private static final String PROGRESSION_QUERY_CASE = "application/vnd.progression.query.prosecutioncase+json";
    private static final String PUBLIC_HEARING_RESULTED = "public.hearing.resulted";
    private static final String PUBLIC_HEARING_RESULTED_CASE_UPDATED = "public.hearing.resulted-case-updated";

    private MessageConsumer publicEventConsumerForProsecutionCaseCreated;
    private MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged;
    private MessageProducer messageProducerClientPublic;
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private String prosecutionCaseId_1;
    private String prosecutionCaseId_2;
    private String masterDefendantId_1;
    private String defendantId_1_forMasterDefendantId_1;
    private String defendantId_2_forMasterDefendantId_1;
    private String materialIdActive;
    private String materialIdDeleted;
    private String referralReasonId;
    private String listedStartDateTime;
    private String earliestStartDateTime;
    private String defendantDOB;
    private String courtCentreId;


    @Before
    public void setUp() {
        stubInitiateHearing();
        prosecutionCaseId_1 = randomUUID().toString();
        prosecutionCaseId_2 = randomUUID().toString();

        masterDefendantId_1 = randomUUID().toString();

        defendantId_1_forMasterDefendantId_1 = randomUUID().toString();
        defendantId_2_forMasterDefendantId_1 = randomUUID().toString();


        materialIdActive = randomUUID().toString();
        materialIdDeleted = randomUUID().toString();
        referralReasonId = randomUUID().toString();
        listedStartDateTime = ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z").toString();
        earliestStartDateTime = ZonedDateTimes.fromString("2019-05-30T18:32:04.238Z").toString();
        defendantDOB = LocalDate.now().minusYears(15).toString();
        courtCentreId = randomUUID().toString();

        publicEventConsumerForProsecutionCaseCreated = publicEvents.createPublicConsumer("public.progression.prosecution-case-created");
        messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents.createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2");
        messageProducerClientPublic = publicEvents.createPublicProducer();
    }

    @After
    public void tearDown() throws JMSException {
        publicEventConsumerForProsecutionCaseCreated.close();
        messageProducerClientPublic.close();
        messageConsumerProsecutionCaseDefendantListingStatusChanged.close();
    }

    @Test
    public void shouldVerifyRelatedCasesWhenAllCasesInActive() throws IOException {
        // initiation of case
        initiateCourtProceedingsForDefendantMatching(prosecutionCaseId_1, defendantId_1_forMasterDefendantId_1, masterDefendantId_1, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);
        verifyInMessagingQueueForProsecutionCaseCreated();
        Matcher[] prosecutionCaseMatchers = getProsecutionCaseMatchers(prosecutionCaseId_1, defendantId_1_forMasterDefendantId_1, emptyList());
        pollProsecutionCasesProgressionFor(prosecutionCaseId_1, prosecutionCaseMatchers);
        final String hearingId1 = doVerifyProsecutionCaseDefendantListingStatusChanged();

        initiateCourtProceedingsForDefendantMatching(prosecutionCaseId_2, defendantId_2_forMasterDefendantId_1, defendantId_2_forMasterDefendantId_1, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);
        verifyInMessagingQueueForProsecutionCaseCreated();
        prosecutionCaseMatchers = getProsecutionCaseMatchers(prosecutionCaseId_2, defendantId_2_forMasterDefendantId_1, emptyList());
        pollProsecutionCasesProgressionFor(prosecutionCaseId_2, prosecutionCaseMatchers);
        final String hearingId2 = doVerifyProsecutionCaseDefendantListingStatusChanged();
        // match defendantId_2_forMasterDefendantId_1 associated to case 2
        matchDefendant(prosecutionCaseId_2, defendantId_2_forMasterDefendantId_1, prosecutionCaseId_1, defendantId_1_forMasterDefendantId_1, masterDefendantId_1);

        poll(requestParams(getReadUrl(String.format("/prosecutioncases/%s", prosecutionCaseId_1)), PROGRESSION_QUERY_CASE).withHeader(USER_ID, randomUUID()))
                .timeout(RestHelper.TIMEOUT, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.prosecutionCase.caseStatus", equalTo("ACTIVE")),
                                withJsonPath("$.relatedCases[0].masterDefendantId", equalTo(masterDefendantId_1)),
                                withJsonPath("$.relatedCases[0].cases[0].caseId", equalTo(prosecutionCaseId_2)),
                                withJsonPath("$.relatedCases[0].cases[0].caseStatus", equalTo("ACTIVE")),
                                withJsonPath("$.relatedCases[0].cases[0].offences[0].offenceTitle", equalTo("ROBBERY")),
                                withJsonPath("$.relatedCases[0].cases[0].offences[0].maxPenalty", equalTo("Max Penalty"))
                        ))
                );

        poll(requestParams(getReadUrl(String.format("/prosecutioncases/%s", prosecutionCaseId_2)), PROGRESSION_QUERY_CASE).withHeader(USER_ID, randomUUID()))
                .timeout(RestHelper.TIMEOUT, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.prosecutionCase.caseStatus", equalTo("ACTIVE")),
                                withJsonPath("$.relatedCases[0].masterDefendantId", equalTo(masterDefendantId_1)),
                                withJsonPath("$.relatedCases[0].cases[0].caseId", equalTo(prosecutionCaseId_1)),
                                withJsonPath("$.relatedCases[0].cases[0].caseStatus", equalTo("ACTIVE")),
                                withJsonPath("$.relatedCases[0].cases[0].offences[0].offenceTitle", equalTo("ROBBERY"))
                        ))
                );

        closeTheCase(prosecutionCaseId_1, masterDefendantId_1, hearingId1);
        closeTheCase(prosecutionCaseId_2, masterDefendantId_1, hearingId2);

        poll(requestParams(getReadUrl(String.format("/prosecutioncases/%s", prosecutionCaseId_1)), PROGRESSION_QUERY_CASE).withHeader(USER_ID, randomUUID()))
                .timeout(RestHelper.TIMEOUT, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.prosecutionCase.caseStatus", equalTo("INACTIVE")),
                                withJsonPath("$.relatedCases[0].masterDefendantId", equalTo(masterDefendantId_1)),
                                withJsonPath("$.relatedCases[0].cases[0].caseId", equalTo(prosecutionCaseId_2)),
                                withJsonPath("$.relatedCases[0].cases[0].caseStatus", equalTo("INACTIVE")),
                                withJsonPath("$.relatedCases[0].cases[0].offences[0].offenceTitle", equalTo("ROBBERY"))
                        ))
                );

        poll(requestParams(getReadUrl(String.format("/prosecutioncases/%s", prosecutionCaseId_2)), PROGRESSION_QUERY_CASE).withHeader(USER_ID, randomUUID()))
                .timeout(RestHelper.TIMEOUT, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.prosecutionCase.caseStatus", equalTo("INACTIVE")),
                                withJsonPath("$.relatedCases[0].masterDefendantId", equalTo(masterDefendantId_1)),
                                withJsonPath("$.relatedCases[0].cases[0].caseId", equalTo(prosecutionCaseId_1)),
                                withJsonPath("$.relatedCases[0].cases[0].caseStatus", equalTo("INACTIVE")),
                                withJsonPath("$.relatedCases[0].cases[0].offences[0].offenceTitle", equalTo("ROBBERY"))
                        ))
                );
    }


    @Test
    public void shouldVerifyRelatedCasesWhenCasesAreMix() throws IOException {
        // initiation of case
        initiateCourtProceedingsForDefendantMatching(prosecutionCaseId_1, defendantId_1_forMasterDefendantId_1, masterDefendantId_1, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);
        verifyInMessagingQueueForProsecutionCaseCreated();
        Matcher[] prosecutionCaseMatchers = getProsecutionCaseMatchers(prosecutionCaseId_1, defendantId_1_forMasterDefendantId_1, emptyList());
        pollProsecutionCasesProgressionFor(prosecutionCaseId_1, prosecutionCaseMatchers);
        final String hearingId1 = doVerifyProsecutionCaseDefendantListingStatusChanged();

        initiateCourtProceedingsForDefendantMatching(prosecutionCaseId_2, defendantId_2_forMasterDefendantId_1, defendantId_2_forMasterDefendantId_1, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);
        verifyInMessagingQueueForProsecutionCaseCreated();
        prosecutionCaseMatchers = getProsecutionCaseMatchers(prosecutionCaseId_2, defendantId_2_forMasterDefendantId_1, emptyList());
        pollProsecutionCasesProgressionFor(prosecutionCaseId_2, prosecutionCaseMatchers);
        final String hearingId2 = doVerifyProsecutionCaseDefendantListingStatusChanged();
        // match defendantId_2_forMasterDefendantId_1 associated to case 2
        matchDefendant(prosecutionCaseId_2, defendantId_2_forMasterDefendantId_1, prosecutionCaseId_1, defendantId_1_forMasterDefendantId_1, masterDefendantId_1);

        closeTheCase(prosecutionCaseId_1, masterDefendantId_1, hearingId1);

        poll(requestParams(getReadUrl(String.format("/prosecutioncases/%s", prosecutionCaseId_1)), PROGRESSION_QUERY_CASE).withHeader(USER_ID, randomUUID()))
                .timeout(RestHelper.TIMEOUT, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.prosecutionCase.caseStatus", equalTo("INACTIVE")),
                                hasNoJsonPath("$.relatedCases")
                        ))
                );

        poll(requestParams(getReadUrl(String.format("/prosecutioncases/%s", prosecutionCaseId_2)), PROGRESSION_QUERY_CASE).withHeader(USER_ID, randomUUID()))
                .timeout(RestHelper.TIMEOUT, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.prosecutionCase.caseStatus", equalTo("ACTIVE")),
                                hasNoJsonPath("$.relatedCases")
                        ))
                );
    }

    private void verifyInMessagingQueueForProsecutionCaseCreated() {
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(publicEventConsumerForProsecutionCaseCreated);
        assertTrue(message.isPresent());
    }

    private void closeTheCase(final String caseId, final String defendantId, final String hearingId){
        sendMessage(messageProducerClientPublic,
                PUBLIC_HEARING_RESULTED, getHearingWithSingleCaseJsonObject(PUBLIC_HEARING_RESULTED_CASE_UPDATED + ".json", caseId,
                        hearingId, defendantId, courtCentreId, "C", "Remanded into Custody", "2593cf09-ace0-4b7d-a746-0703a29f33b5"), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED)
                        .withUserId(randomUUID().toString())
                        .build());

    }

    private JsonObject getHearingWithSingleCaseJsonObject(final String path, final String caseId, final String hearingId,
                                                          final String defendantId, final String courtCentreId, final String bailStatusCode,
                                                          final String bailStatusDescription, final String bailStatusId) {
        return stringToJsonObjectConverter.convert(
                getPayload(path)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("BAIL_STATUS_ID", bailStatusId)
                        .replaceAll("BAIL_STATUS_CODE", bailStatusCode)
                        .replaceAll("BAIL_STATUS_DESCRIPTION", bailStatusDescription)
        );
    }

    private String doVerifyProsecutionCaseDefendantListingStatusChanged() {
        final Optional<JsonObject> message = AwaitUtil.awaitAndRetrieveMessageAsJsonObject(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        final JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        return prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
    }
}
