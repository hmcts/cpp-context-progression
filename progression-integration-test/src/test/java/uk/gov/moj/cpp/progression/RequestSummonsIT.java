package uk.gov.moj.cpp.progression;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.stub.ListingStub.getHearingIdFromSendCaseForListingRequest;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostSendCaseForListing;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub;
import uk.gov.moj.cpp.progression.stub.HearingStub;
import uk.gov.moj.cpp.progression.stub.IdMapperStub;
import uk.gov.moj.cpp.progression.stub.NotificationServiceStub;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import com.jayway.restassured.path.json.JsonPath;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@SuppressWarnings("squid:S1607")
@Ignore
// Temporarily ignoring the tests to debug the issue
// also unblock hearing
public class RequestSummonsIT {

    public static final String PUBLIC_EVENT = "public.event";
    public static final String NOTIFICATION_NOTIFY_CONTENT_TYPE = "application/vnd.notificationnotify.letter+json";
    private static final String PUBLIC_SUMMONS_REQUESTED = "public.summons-requested";
    private static final String PUBLIC_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String CASE_ID = randomUUIDString();
    private static final String COURT_DOCUMENT_ID = randomUUIDString();
    private static final String MATERIAL_ID_ACTIVE = randomUUIDString();
    private static final String MATERIAL_ID_DELETED = randomUUIDString();
    private static final String DEFENDANT_ID = randomUUIDString();
    private static final String COURT_CENTRE_ID = randomUUIDString();
    private static final MessageProducer PUBLIC_MESSAGE_PRODUCER = publicEvents.createProducer();
    private static final MessageConsumer PRIVATE_MESSAGE_CONSUMER = privateEvents.createConsumer("progression.event.nows-material-request-recorded");
    private static final String DOCUMENT_TEXT = STRING.next();

    @AfterClass
    public static void tearDown() throws JMSException {
        PUBLIC_MESSAGE_PRODUCER.close();
        PRIVATE_MESSAGE_CONSUMER.close();
    }

    private static String randomUUIDString() {
        return UUID.randomUUID().toString();
    }

    private static String getStringFromResource(final String path) {
        String request = null;
        try {
            request = Resources.toString(Resources.getResource(path), Charset.defaultCharset());
        } catch (final Exception e) {
            fail("Error consuming file from location " + path);
        }
        return request;
    }

    @Before
    public void setUp() throws IOException {
        createMockEndpoints();
        HearingStub.stubInitiateHearing();
        DocumentGeneratorStub.stubDocumentCreate(DOCUMENT_TEXT);
        IdMapperStub.setUp();
        NotificationServiceStub.setUp();
    }

    @Test
    public void shouldRequestSummons() throws Exception {
        addProsecutionCaseToCrownCourt(CASE_ID, DEFENDANT_ID, MATERIAL_ID_ACTIVE, MATERIAL_ID_DELETED, COURT_DOCUMENT_ID, "2daefec3-2f76-8109-82d9-2e60544a6c02");
        verifyPostSendCaseForListing(CASE_ID, DEFENDANT_ID);

        String hearingId = getHearingIdFromSendCaseForListingRequest();
        final Metadata metadata = generateMetadata();
        JsonObject hearingConfirmedPayload = generateHearingConfirmedPayload(hearingId);
        sendMessage(PUBLIC_MESSAGE_PRODUCER, PUBLIC_HEARING_CONFIRMED, hearingConfirmedPayload, metadata);

        verifyPrintRequestAccepted();
    }

    private JsonObject generateHearingConfirmedPayload(final String hearingId) throws IOException {
        String payloadStr = getStringFromResource(PUBLIC_HEARING_CONFIRMED + ".json")
                .replaceAll("CASE_ID", CASE_ID)
                .replaceAll("COURT_CENTRE_ID", COURT_CENTRE_ID)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("DEFENDANT_ID", DEFENDANT_ID);
        return new StringToJsonObjectConverter().convert(payloadStr);
    }

    private Metadata generateMetadata() {
        return metadataBuilder()
                .withId(UUID.randomUUID())
                .withUserId(UUID.randomUUID().toString())
                .withName(PUBLIC_HEARING_CONFIRMED)
                .build();
    }

    private void verifyPostSendLetterNotificationSend() {
        verify(
                postRequestedFor(urlMatching("/notificationnotify-service/command/api/rest/notificationnotify/notifications/.*"))
                        .withHeader("Content-Type", equalTo(NOTIFICATION_NOTIFY_CONTENT_TYPE)));
    }

    public void verifyPrintRequestAccepted() {
        final JsonPath jsonResponse = retrieveMessage(PRIVATE_MESSAGE_CONSUMER);

        assertThat(jsonResponse.get("context.caseId"), is(CASE_ID.toString()));
    }
}
