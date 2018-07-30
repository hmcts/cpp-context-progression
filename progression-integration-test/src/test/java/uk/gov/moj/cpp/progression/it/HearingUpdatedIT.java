package uk.gov.moj.cpp.progression.it;

import static com.jayway.jsonassert.JsonAssert.with;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonObjectMetadata;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.progression.stub.ReferenceDataStub;

import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import org.junit.AfterClass;
import org.junit.Test;

public class HearingUpdatedIT {

    public static final String ARBITRARY_TRIAL = RandomGenerator.STRING.next();
    public static final String ARBITRARY_HEARING_DAY = "2016-06-01T10:00:00Z";
    private static final String PUBLIC_EVENT_HEARING_UPDATED = "public.hearing-updated";
    private static final String PUBLIC_PROGRESSION_EVENTS_HEARING_DETAIL_CHANGED = "public.hearing-detail-changed";
    private static final String ARBITRARY_HEARING_ID = UUID.randomUUID().toString();
    private static final String ARBITRARY_HEARING_COURT_ROOM_ID = "47834e9d-0bca-4f26-aa30-270580496e6e";
    private static final String ARBITRARY_HEARING_JUDGE_ID = UUID.randomUUID().toString();

    private static final String REF_DATA_QUERY_JUDGE_PAYLOAD = "/restResource/ref-data-get-judge.json";
    private static final String REF_DATA_QUERY_COURT_CENTRE_PAYLOAD = "/restResource/ref-data-get-court-centre.json";
    private static final MessageProducer MESSAGE_PRODUCER_CLIENT_PUBLIC = publicEvents.createProducer();
    private static final MessageConsumerClient PUBLIC_EVENT_HEARING_DETAIL_CHANGED_CONSUMER = new MessageConsumerClient();
    public static final String PUBLIC_ACTIVE_MQ_TOPIC = "public.event";
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();


    public static void init() {
        createMockEndpoints();
        ReferenceDataStub.stubQueryJudge(REF_DATA_QUERY_JUDGE_PAYLOAD);
        ReferenceDataStub.stubQueryCourtCentre(REF_DATA_QUERY_COURT_CENTRE_PAYLOAD);
        PUBLIC_EVENT_HEARING_DETAIL_CHANGED_CONSUMER.startConsumer(PUBLIC_PROGRESSION_EVENTS_HEARING_DETAIL_CHANGED,PUBLIC_ACTIVE_MQ_TOPIC);
    }



    @Test
    public void shouldEnrichUpdateHearingPublicEvent() throws Exception {

        final String userId = UUID.randomUUID().toString();
        init();
        Metadata metadata = JsonObjectMetadata.metadataOf(UUID.randomUUID(), PUBLIC_EVENT_HEARING_UPDATED)
                .withUserId(userId)
                .build();
        //message should trigger hearingUpdated process via  public event
        sendMessage(MESSAGE_PRODUCER_CLIENT_PUBLIC,
                PUBLIC_EVENT_HEARING_UPDATED, publicHearingUpdatedEvent(), metadata);

        //should publish new public fat hearing event
        verifyHearingDetailChangedPublicEvent();

    }
    private void verifyHearingDetailChangedPublicEvent() {
        final String sendingSheetCompletedEvent = PUBLIC_EVENT_HEARING_DETAIL_CHANGED_CONSUMER.retrieveMessage().orElse(null);

        assertThat(sendingSheetCompletedEvent, notNullValue());

        with(sendingSheetCompletedEvent)
                .assertThat("$.hearing.id", is(ARBITRARY_HEARING_ID));
        with(sendingSheetCompletedEvent)
                .assertThat("$.hearing.type", is(ARBITRARY_TRIAL));
        with(sendingSheetCompletedEvent)
                .assertThat("$.hearing.courtRoomName", is("1"));
        with(sendingSheetCompletedEvent)
                .assertThat("$.hearing.judge.firstName", is("John"));
    }

    @AfterClass
    public static void tearDown() throws JMSException {
        MESSAGE_PRODUCER_CLIENT_PUBLIC.close();
        PUBLIC_EVENT_HEARING_DETAIL_CHANGED_CONSUMER.close();
    }


    private JsonObject publicHearingUpdatedEvent() {
        return stringToJsonObjectConverter.convert(String.format("{\n" +
                        "  \"hearing\": {\n" +
                        "    \"id\": \"%s\",\n" +
                        "    \"type\": \"%s\",\n" +
                        "    \"courtRoomId\": \"%s\",\n" +
                        "    \"courtCentreId\": \"e8821a38-546d-4b56-9992-ebdd772a561f\",\n" +
                        "    \"judgeId\": \"%s\",\n" +
                        "    \"hearingDays\": [\n" +
                        "      \"%s\",\n" +
                        "      \"2016-07-03T10:15:00Z\"\n" +
                        "    ]\n" +
                        "  }\n" +
                        "}",
                ARBITRARY_HEARING_ID,
                ARBITRARY_TRIAL,
                ARBITRARY_HEARING_COURT_ROOM_ID,
                ARBITRARY_HEARING_JUDGE_ID,
                ARBITRARY_HEARING_DAY));
    }

}

