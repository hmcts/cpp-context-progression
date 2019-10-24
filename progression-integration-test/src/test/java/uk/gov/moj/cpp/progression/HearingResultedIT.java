package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.helper.DefaultRequests.PROGRESSION_QUERY_PROSECUTION_CASE_JSON;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getProsecutioncasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getQueryUri;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.assertProsecutionCase;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.helper.RestHelper;
import uk.gov.moj.cpp.progression.stub.HearingStub;

import java.nio.charset.Charset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import com.jayway.restassured.path.json.JsonPath;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class HearingResultedIT {

    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_HEARING_RESULTED = "public.hearing.resulted";
    private static final String PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT = "public.progression" +
            ".prosecution-cases-referred-to-court";
    private static final MessageProducer messageProducerClientPublic = publicEvents.createProducer();
    private static final MessageConsumer messageConsumerClientPublicForReferToCourtOnHearingInitiated = publicEvents
            .createConsumer(PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT);

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private String userId;
    private String hearingId;
    private String caseId;
    private String defendantId;
    private String courtCentreId;
    private String newCourtCentreId;

    @AfterClass
    public static void tearDown() throws JMSException {
        messageProducerClientPublic.close();
        messageConsumerClientPublicForReferToCourtOnHearingInitiated.close();
    }

    private static String getPayloadForCreatingRequest(final String ramlPath) {
        String request = null;
        try {
            request = Resources.toString(
                    Resources.getResource(ramlPath),
                    Charset.defaultCharset()
            );
        } catch (final Exception e) {
            fail("Error consuming file from location " + ramlPath);
        }
        return request;
    }

    @Before
    public void setUp() {
        createMockEndpoints();
        HearingStub.stubInitiateHearing();
        userId = randomUUID().toString();
        hearingId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        newCourtCentreId = UUID.fromString("999bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
    }

    @Test
    public void shouldUpdateCaseAtAGlance() throws Exception {

        System.out.println("HEARING ID ::" + hearingId);
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        assertProsecutionCase(getJsonObject(getProsecutioncasesProgressionFor(caseId)).getJsonObject
                ("prosecutionCase"), caseId, defendantId);

        final Metadata metadata = JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfimedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId);
        sendMessage(messageProducerClientPublic,
                PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfimedJson, metadata);

        verifyInMessagingQueueForCasesReferredToCourts();

        sendMessage(messageProducerClientPublic,
                PUBLIC_HEARING_RESULTED, getHearingJsonObject(PUBLIC_HEARING_RESULTED + ".json", caseId,
                        hearingId, defendantId, newCourtCentreId), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED)
                        .withUserId(userId)
                        .build());


        getProsecutioncasesProgressionFor(caseId);

        final ResponseData responseData = poll(requestParams(getQueryUri("/prosecutioncases/" + caseId), PROGRESSION_QUERY_PROSECUTION_CASE_JSON)
                .withHeader(USER_ID, UUID.randomUUID()))
                .timeout(RestHelper.TIMEOUT, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                                withJsonPath("$.caseAtAGlance.hearings.[*].type.description", hasItem(equalTo("Sentence"))),
                                withJsonPath("$.caseAtAGlance.hearings.[*].courtCentre.id", hasItem(equalTo(newCourtCentreId))),
                                withJsonPath("$.caseAtAGlance.hearings.[*].defendants.[*].id", hasItem(equalTo(defendantId)))
                        )));

        verifyPersonDefendantAndOffenceIsUpdatedWithCTL(responseData);

    }


    private void verifyPersonDefendantAndOffenceIsUpdatedWithCTL(final ResponseData responseData) {

        JsonPath jsonResponse = new JsonPath(responseData.getPayload());

        assertThat(jsonResponse.get("prosecutionCase.defendants[0].personDefendant.custodyTimeLimit"), equalTo("2018-01-01"));

        assertThat(jsonResponse.get("prosecutionCase.defendants[0].personDefendant.bailStatus.custodyTimeLimit.timeLimit"), equalTo("2018-09-10"));
        assertThat(jsonResponse.get("prosecutionCase.defendants[0].personDefendant.bailStatus.custodyTimeLimit.daysSpent"), equalTo(44));
        assertThat(jsonResponse.get("prosecutionCase.defendants[0].offences[0].custodyTimeLimit.timeLimit"), equalTo("2018-09-14"));
        assertThat(jsonResponse.get("prosecutionCase.defendants[0].offences[0].custodyTimeLimit.daysSpent"), equalTo(55));

    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String courtCentreId) {
        return stringToJsonObjectConverter.convert(
                getPayloadForCreatingRequest(path)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
        );
    }

    public static void verifyInMessagingQueueForCasesReferredToCourts() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerClientPublicForReferToCourtOnHearingInitiated);
        assertTrue(message.isPresent());
    }
}

