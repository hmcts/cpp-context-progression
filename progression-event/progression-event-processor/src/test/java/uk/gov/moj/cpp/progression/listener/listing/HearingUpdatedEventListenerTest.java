package uk.gov.moj.cpp.progression.listener.listing;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.justice.services.test.utils.core.random.StringGenerator;
import uk.gov.moj.cpp.progression.helper.JsonHelper;
import uk.gov.moj.cpp.progression.service.ReferenceDataService;

import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingUpdatedEventListenerTest {
    public static final String ARBITRARY_TRIAL = RandomGenerator.STRING.next();
    public static final String ARBITRARY_COURT_NAME = RandomGenerator.STRING.next();
    public static final String ARBITRARY_HEARING_DAY = "2016-06-01T10:00:00Z";
    private static final String PUBLIC_EVENT_HEARING_UPDATED = "public.hearing-updated";
    private static final String PUBLIC_PROGRESSION_EVENTS_HEARING_DETAIL_CHANGED = "public.hearing-detail-changed";
    private static final String ARBITRARY_HEARING_ID = UUID.randomUUID().toString();
    private static final String ARBITRARY_HEARING_COURT_ROOM_ID = UUID.randomUUID().toString();
    private static final String ARBITRARY_HEARING_JUDGE_ID = UUID.randomUUID().toString();
    private static final String ARBITRARY_HEARING_JUDGE_TITLE = RandomGenerator.STRING.next();
    private static final String ARBITRARY_HEARING_JUDGE_FIRST_NAME = RandomGenerator.STRING.next();
    private static final String ARBITRARY_HEARING_JUDGE_LAST_NAME = RandomGenerator.STRING.next();

    @Mock
    private Sender sender;
    @Spy
    private Enveloper enveloper = createEnveloper();
    @Mock
    private ReferenceDataService referenceDataService;

    @Captor
    private ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor;

    @InjectMocks
    private HearingUpdatedEventListener testObj;

    @Test
    public void publishHearingDetailChangedPublicEvent() throws Exception {
        //Given
        final String userId = UUID.randomUUID().toString();
        final JsonEnvelope jsonEnvelope = JsonHelper.createJsonEnvelope(
                JsonHelper.createMetadataWithProcessIdAndUserId(UUID.randomUUID().toString(), PUBLIC_EVENT_HEARING_UPDATED, UUID.randomUUID().toString(), userId),
                publicHearingUpdatedEvent());
        when(referenceDataService.getCourtCentreByIdAsText(any(), any())).thenReturn(Optional.of(getCourtRoom(ARBITRARY_HEARING_COURT_ROOM_ID)));
        when(referenceDataService.getJudgeByIdAsText(any(), any())).thenReturn(Optional.of(getJudge()));

        //when
        testObj.publishHearingDetailChangedPublicEvent(jsonEnvelope);

        //then
        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getValue(), is(jsonEnvelope(
                withMetadataEnvelopedFrom(jsonEnvelope)
                        .withName(PUBLIC_PROGRESSION_EVENTS_HEARING_DETAIL_CHANGED),
                payloadIsJson(allOf(
                        withJsonPath("$.hearing.id", equalTo(ARBITRARY_HEARING_ID)),
                        withJsonPath("$.hearing.type", equalTo(ARBITRARY_TRIAL)),
                        withJsonPath("$.hearing.courtRoomName", equalTo(ARBITRARY_COURT_NAME)),
                        withJsonPath("$.hearing.courtRoomId", equalTo(ARBITRARY_HEARING_COURT_ROOM_ID)),
                        withJsonPath("$.hearing.hearingDays[0]", equalTo(ARBITRARY_HEARING_DAY)),
                        withJsonPath("$.hearing.judge.id", equalTo(ARBITRARY_HEARING_JUDGE_ID)),
                        withJsonPath("$.hearing.judge.firstName", equalTo(ARBITRARY_HEARING_JUDGE_FIRST_NAME)),
                        withJsonPath("$.hearing.judge.lastName", equalTo(ARBITRARY_HEARING_JUDGE_LAST_NAME)),
                        withJsonPath("$.hearing.judge.title", equalTo(ARBITRARY_HEARING_JUDGE_TITLE))
                ))).thatMatchesSchema()
        ));
    }

    public JsonObject getCourtRoom(String courtRoomID) {
        final JsonObject courtRoomJsonObject = Json.createObjectBuilder()
                .add("name", new StringGenerator().next())
                .add("courtRooms", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("id", courtRoomID)
                                .add("name", ARBITRARY_COURT_NAME).build()).build())
                .build();
        return courtRoomJsonObject;
    }

    public JsonObject getJudge() {
        final JsonObject judgeJsonObject = Json.createObjectBuilder()
                .add("firstName", ARBITRARY_HEARING_JUDGE_FIRST_NAME)
                .add("lastName", ARBITRARY_HEARING_JUDGE_LAST_NAME)
                .add("title", ARBITRARY_HEARING_JUDGE_TITLE)
                .build();
        return judgeJsonObject;
    }

    private JsonObject publicHearingUpdatedEvent() {
        return Json.createObjectBuilder()
                .add("hearing", Json.createObjectBuilder()
                        .add("id", ARBITRARY_HEARING_ID)
                        .add("type", ARBITRARY_TRIAL)
                        .add("judgeId", ARBITRARY_HEARING_JUDGE_ID)
                        .add("courtRoomId", ARBITRARY_HEARING_COURT_ROOM_ID)
                        .add("courtCentreId", "e8821a38-546d-4b56-9992-ebdd772a561f")
                        .add("hearingDays", Json.createArrayBuilder().add(ARBITRARY_HEARING_DAY).add("2016-07-03T10:15:00Z").build())
                        .build())
                .build();
    }
}