package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelopeProvider;
import uk.gov.moj.cpp.progression.command.service.WeekCommencingSessionAdapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.ServerErrorException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ListNewHearingApiTest {
    private static final String LIST_NEW_HEARING_NAME = "progression.list-new-hearing";
    private static final String LIST_COMMAND_NEW_HEARING_NAME = "progression.command.list-new-hearing";
    private static final String BOOKED_COURT_SCHEDULE_ID = "ccb4af70-e150-479a-954c-1c6ae98d0a34";

    @InjectMocks
    private ListNewHearingApi listNewHearingApi;

    @Mock
    private Sender sender;

    @Mock
    private WeekCommencingSessionAdapter weekCommencingSessionAdapter;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> envelopeCaptor;

    private UUID uuid;
    private UUID userId;

    @BeforeEach
    public void setUp() {
        uuid = randomUUID();
        userId = randomUUID();
    }

    @Test
    public void shouldHandleAddDocumentCommand() {
        assertThat(ListNewHearingApi.class, isHandlerClass(COMMAND_API)
                .with(method("handle").thatHandles(LIST_NEW_HEARING_NAME)));
    }

    @Test
    public void shouldListNewHearing() {
        final JsonObject payload = CommandClientTestBase.readJson("json/progression.list-new-hearing.json", JsonObject.class);
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(LIST_NEW_HEARING_NAME)
                .withId(uuid)
                .withUserId(userId.toString())
                .build();
        final JsonEnvelope commandEnvelope = new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);


        listNewHearingApi.handle(commandEnvelope);
        verify(sender, times(1)).send(envelopeCaptor.capture());

        final Envelope newCommand = envelopeCaptor.getValue();

        assertThat(newCommand.metadata().name(), is(LIST_COMMAND_NEW_HEARING_NAME));
        assertThat(newCommand.payload(), equalTo(commandEnvelope.payload()));
    }

    @Test
    public void shouldAddWeekCommencingDateWhenTheCrownBookedSessionIsWeekCommencing() {
        final JsonObject weekCommencingDate = createObjectBuilder().add("startDate", "2026-11-16").add("duration", 1).build();
        when(weekCommencingSessionAdapter.weekCommencingDateOf(List.of(BOOKED_COURT_SCHEDULE_ID))).thenReturn(Optional.of(weekCommencingDate));
        final JsonObject payload = payload(crownHearing().add("bookedSlots", createArrayBuilder().add(bookedSlot(BOOKED_COURT_SCHEDULE_ID))));

        final JsonObject sent = sentFor(payload);

        final JsonObject expected = createObjectBuilder(payload)
                .add("listNewHearing", createObjectBuilder(payload.getJsonObject("listNewHearing")).add("weekCommencingDate", weekCommencingDate))
                .build();
        assertThat(sent, is(expected));
    }

    @Test
    public void shouldSendCrownHearingUnchangedWhenTheBookedSessionIsNotWeekCommencing() {
        when(weekCommencingSessionAdapter.weekCommencingDateOf(List.of(BOOKED_COURT_SCHEDULE_ID))).thenReturn(Optional.empty());
        final JsonObject payload = payload(crownHearing().add("bookedSlots", createArrayBuilder().add(bookedSlot(BOOKED_COURT_SCHEDULE_ID))));

        assertThat(sentFor(payload), is(payload));
    }

    @Test
    public void shouldNotLookUpTheSessionForMagistratesHearing() {
        final JsonObject payload = payload(hearing("MAGISTRATES").add("bookedSlots", createArrayBuilder().add(bookedSlot(BOOKED_COURT_SCHEDULE_ID))));

        assertThat(sentFor(payload), is(payload));
        verify(weekCommencingSessionAdapter, never()).weekCommencingDateOf(any());
    }

    @Test
    public void shouldKeepTheWeekCommencingDateAlreadyOnTheRequest() {
        final JsonObject payload = payload(crownHearing()
                .add("weekCommencingDate", createObjectBuilder().add("startDate", "2026-11-23").add("duration", 2))
                .add("bookedSlots", createArrayBuilder().add(bookedSlot(BOOKED_COURT_SCHEDULE_ID))));

        assertThat(sentFor(payload), is(payload));
        verify(weekCommencingSessionAdapter, never()).weekCommencingDateOf(any());
    }

    @Test
    public void shouldLookUpOnlyBookedSlotsThatHaveACourtScheduleId() {
        when(weekCommencingSessionAdapter.weekCommencingDateOf(List.of(BOOKED_COURT_SCHEDULE_ID))).thenReturn(Optional.empty());
        final JsonObject payload = payload(crownHearing().add("bookedSlots", createArrayBuilder()
                .add(createObjectBuilder().add("duration", 20))
                .add(bookedSlot(" "))
                .add(bookedSlot(BOOKED_COURT_SCHEDULE_ID))));

        assertThat(sentFor(payload), is(payload));
        verify(weekCommencingSessionAdapter).weekCommencingDateOf(List.of(BOOKED_COURT_SCHEDULE_ID));
    }

    @Test
    public void shouldPassNoIdsWhenTheCrownHearingHasNoBookedSlots() {
        when(weekCommencingSessionAdapter.weekCommencingDateOf(List.of())).thenReturn(Optional.empty());
        final JsonObject payload = payload(crownHearing());

        assertThat(sentFor(payload), is(payload));
    }

    @Test
    public void shouldNotSendTheCommandWhenTheSessionLookupFails() {
        when(weekCommencingSessionAdapter.weekCommencingDateOf(List.of(BOOKED_COURT_SCHEDULE_ID)))
                .thenThrow(new ServerErrorException("courtscheduler down", 500));
        final JsonObject payload = payload(crownHearing().add("bookedSlots", createArrayBuilder().add(bookedSlot(BOOKED_COURT_SCHEDULE_ID))));

        assertThrows(ServerErrorException.class, () -> listNewHearingApi.handle(envelopeOf(payload)));

        verify(sender, never()).send(any());
    }

    private JsonObject sentFor(final JsonObject payload) {
        listNewHearingApi.handle(envelopeOf(payload));
        verify(sender).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata().name(), is(LIST_COMMAND_NEW_HEARING_NAME));
        return (JsonObject) envelopeCaptor.getValue().payload();
    }

    private JsonEnvelope envelopeOf(final JsonObject payload) {
        final Metadata metadata = Envelope.metadataBuilder()
                .withName(LIST_NEW_HEARING_NAME)
                .withId(uuid)
                .withUserId(userId.toString())
                .build();
        return new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);
    }

    private static JsonObject payload(final JsonObjectBuilder listNewHearing) {
        return createObjectBuilder()
                .add("listNewHearing", listNewHearing)
                .add("sendNotificationToParties", false)
                .build();
    }

    private static JsonObjectBuilder crownHearing() {
        return hearing("CROWN");
    }

    private static JsonObjectBuilder hearing(final String jurisdictionType) {
        return createObjectBuilder()
                .add("jurisdictionType", jurisdictionType)
                .add("estimatedMinutes", 20)
                .add("earliestStartDateTime", "2026-11-16T10:00:00.000Z");
    }

    private static JsonObjectBuilder bookedSlot(final String courtScheduleId) {
        return createObjectBuilder()
                .add("duration", 20)
                .add("courtScheduleId", courtScheduleId);
    }
}
