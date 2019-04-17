package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.helper.DefaultRequests.PROGRESSION_QUERY_CASE_AT_A_GLANCE_JSON;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getProsecutionCaseAtAGlanceFor;
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
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.HearingStub;

import java.nio.charset.Charset;
import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class HearingUpdatedIT {

    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_LISTING_HEARING_UPDATED = "public.listing.hearing-updated";
    private static final String PUBLIC_HEARING_DETAIL_CHANGED = "public.hearing-detail-changed";
    private static final String PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT = "public.progression.prosecution-cases-referred-to-court";

    private static final MessageProducer messageProducerClientPublic = publicEvents.createProducer();
    private static final MessageConsumer messageConsumerClientPublicForReferToCourtOnHearingInitiated = publicEvents
            .createConsumer(PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT);
    private static final MessageConsumer messageConsumerClientPublicForHearingDetailChanged = publicEvents
            .createConsumer(PUBLIC_HEARING_DETAIL_CHANGED);


    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private String caseId;
    private String defendantId;
    private String userId;
    private String courtCentreId;
    private String hearingId;

    @Before
    public void setUp() {
        createMockEndpoints();
        HearingStub.stubInitiateHearing();
        hearingId = randomUUID().toString();
        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        courtCentreId = randomUUID().toString();
    }

    @AfterClass
    public static void tearDown() throws JMSException {
        messageProducerClientPublic.close();
        messageConsumerClientPublicForReferToCourtOnHearingInitiated.close();
    }

    @Test
    public void shouldUpdateHearing() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, defendantId);

        final JsonObject prosecutionCaseJson = getJsonObject(getProsecutioncasesProgressionFor(caseId));
        assertProsecutionCase(prosecutionCaseJson.getJsonObject("prosecutionCase"), caseId, defendantId);

        getProsecutionCaseAtAGlanceFor(caseId);

        final Metadata hearingConfirmedMetadata = createMetadata(PUBLIC_LISTING_HEARING_CONFIRMED);
        final JsonObject hearingConfirmedJson = getHearingConfirmedJsonObject(hearingId);
        sendMessage(messageProducerClientPublic, PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, hearingConfirmedMetadata);

        verifyInMessagingQueue(messageConsumerClientPublicForReferToCourtOnHearingInitiated);

        verifyCourtCentreIdInCaseAtAGlance(caseId, courtCentreId);

        final String updatedCourtCentreId = randomUUID().toString();
        final Metadata hearingUpdatedMetadata = createMetadata(PUBLIC_LISTING_HEARING_UPDATED);
        final JsonObject hearingUpdatedJson = getHearingUpdatedJsonObject(hearingId, updatedCourtCentreId);
        sendMessage(messageProducerClientPublic, PUBLIC_LISTING_HEARING_UPDATED, hearingUpdatedJson, hearingUpdatedMetadata);

        verifyCourtCentreIdInCaseAtAGlance(caseId, updatedCourtCentreId);
        verifyInMessagingQueue(messageConsumerClientPublicForHearingDetailChanged);

    }

    private static void verifyCourtCentreIdInCaseAtAGlance(final String caseId, final String courtCentreId) {
        poll(requestParams(getQueryUri("/prosecutioncases/" + caseId), PROGRESSION_QUERY_CASE_AT_A_GLANCE_JSON)
                .withHeader(USER_ID, UUID.randomUUID()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.hearings[0].courtCentre.id", equalTo(courtCentreId)),
                                withJsonPath("$.hearings[0].hearingListingStatus", equalTo("HEARING_INITIALISED"))
                        )));
    }

    private Metadata createMetadata(final String eventName) {
        return JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName(eventName)
                .withUserId(userId)
                .build();
    }

    private JsonObject getHearingConfirmedJsonObject(final String hearingId) {
        return stringToJsonObjectConverter.convert(
                getPayloadForCreatingRequest("public.listing.hearing-confirmed.json")
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
        );
    }

    private JsonObject getHearingUpdatedJsonObject(final String hearingId, final String courtCentreId) {
        return stringToJsonObjectConverter.convert(
                getPayloadForCreatingRequest("public.listing.hearing-updated.json")
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
        );
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

    public static void verifyInMessagingQueue(final MessageConsumer consumer) {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(consumer);
        assertTrue(message.isPresent());
    }

}
