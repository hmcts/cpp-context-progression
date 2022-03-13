package uk.gov.moj.cpp.progression.util;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.jayway.awaitility.Awaitility.waitAtMost;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.stub.HearingStub.HEARING_COMMAND;
import static uk.gov.moj.cpp.progression.stub.HearingStub.HEARING_RESPONSE_TYPE;

import com.jayway.jsonpath.matchers.JsonPathMatchers;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.helper.AbstractTestHelper;
import uk.gov.moj.cpp.progression.helper.QueueUtil;

import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.io.Resources;
import com.jayway.awaitility.Duration;
import org.json.JSONException;
import org.json.JSONObject;


public class ReferBoxWorkApplicationHelper extends AbstractTestHelper {

    private static final String PUBLIC_PROGRESSION_BOXWORK_APPLICATION_REFERRED = "public.progression.boxwork-application-referred";
    public static final String PUBLIC_PROGRESSION_EVENTS_HEARING_EXTENDED = "public.progression.events.hearing-extended";

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private static final MessageProducer messageProducerClientPublic = publicEvents.createPublicProducer();
    private static final MessageConsumer messageConsumerClientPublicForReferBoxWorkApplicationOnHearingInitiated = publicEvents.createPublicConsumer(PUBLIC_PROGRESSION_BOXWORK_APPLICATION_REFERRED);
    private static final MessageConsumer publicEventsConsumerForHearingExtended = publicEvents.createPublicConsumer(PUBLIC_PROGRESSION_EVENTS_HEARING_EXTENDED);

    final String userId = randomUUID().toString();
    final String hearingId = randomUUID().toString();
    final String caseId = randomUUID().toString();
    final String dueDate = LocalDate.now().plusDays(5).toString();

    final Metadata metadata = JsonEnvelope.metadataBuilder()
            .withId(randomUUID())
            .withName("hearing.command.initiate")
            .withUserId(userId)
            .build();

    final JsonObject boxWorkApplicationJson = getHearingJsonObject("public.boxwork-application-referred.json", caseId, hearingId, dueDate);

    public ReferBoxWorkApplicationHelper() {

        privateEventsConsumer = QueueUtil.privateEvents.createPrivateConsumer("hearing.command.initiate");
        sendMessage(messageProducerClientPublic, PUBLIC_PROGRESSION_BOXWORK_APPLICATION_REFERRED, boxWorkApplicationJson, metadata);

    }


    private JsonObject getHearingJsonObject(String path, String caseId, String hearingId, String dueDate) {
        return stringToJsonObjectConverter.convert(
                getPayloadForCreatingRequest(path)
                        .replaceAll("DUE_DATE", dueDate)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)

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

    public static void verifyInMessagingQueueForBoxWorkReferred() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerClientPublicForReferBoxWorkApplicationOnHearingInitiated);
        assertTrue(message.isPresent());
    }

    public static JsonObject getHearingInMessagingQueueForBoxWorkReferred() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerClientPublicForReferBoxWorkApplicationOnHearingInitiated);
        assertTrue(message.isPresent());
        return message.get().getJsonObject("hearing");
    }


    public static void verifyPostBoxWorkApplicationReferredHearing(final String applicationId) {
        waitAtMost(Duration.TEN_SECONDS).until(() ->
                {
                    final Stream<JSONObject> boxWorkCourtHearingRequestsAsStream = getBoxWorkApplicationReferredToCourtHearingRequestsAsStream();
                    boxWorkCourtHearingRequestsAsStream
                            .anyMatch(
                                    payload -> {
                                        try {
                                            JSONObject courtApplication = payload.getJSONArray("hearingRequest").getJSONObject(0).getJSONArray("courtApplications").getJSONObject(0);
                                            return courtApplication.getString("id").equals(applicationId);
                                        } catch (JSONException e) {
                                            return false;
                                        }
                                    }
                            );
                }

        );

    }

    public static String getPostBoxWorkApplicationReferredHearing(final String applicationId ) {
        return waitAtMost(Duration.FIVE_MINUTES).until(() ->
                {
                    final Stream<JSONObject> boxWorkCourtHearingRequestsAsStream = getBoxWorkApplicationReferredToCourtHearingRequestsAsStream();
                    return boxWorkCourtHearingRequestsAsStream
                            .filter(
                                    payload -> {
                                        try {
                                            JSONObject courtApplication = payload.getJSONObject("hearing").getJSONArray("courtApplications").getJSONObject(0);
                                            return courtApplication.getString("id").equals(applicationId);
                                        } catch (JSONException e) {
                                            return false;
                                        }
                                    }
                            ).findFirst().map(JSONObject::toString).orElse("{hearing:{}}");
                }, JsonPathMatchers.hasJsonPath("$.hearing")
        );

    }


    public static void verifyPublicEventForHearingExtended(final String sittingDate, final String courtCenterCode, final String jurisdictionType) {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(publicEventsConsumerForHearingExtended);
        assertTrue(message.isPresent());
        final JsonObject publicHearingExtendedEvent = message.get();
        assertThat(publicHearingExtendedEvent.getString("hearingId"), is(notNullValue()));
        assertThat(publicHearingExtendedEvent.getJsonArray("hearingDays").size(), is(1));
        assertThat(publicHearingExtendedEvent.getJsonObject("courtCentre").getString("code"), is(courtCenterCode));
        assertThat(publicHearingExtendedEvent.getString("jurisdictionType"), is(jurisdictionType));
        assertThat(((JsonObject)publicHearingExtendedEvent.getJsonArray("hearingDays").get(0)).getString("sittingDay"), is(sittingDate));
    }

    private static Stream<JSONObject> getBoxWorkApplicationReferredToCourtHearingRequestsAsStream() {
        return findAll(postRequestedFor(urlPathEqualTo(HEARING_COMMAND))
                .withHeader(CONTENT_TYPE, equalTo(HEARING_RESPONSE_TYPE)))
                .stream()
                .map(LoggedRequest::getBodyAsString)
                .map(JSONObject::new);
    }

}
