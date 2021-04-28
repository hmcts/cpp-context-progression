package uk.gov.moj.cpp.progression.handler;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;

import uk.gov.justice.core.courts.Category;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.NextHearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChanged;
import uk.gov.justice.core.courts.UnscheduledNextHearingsRequested;
import uk.gov.justice.hearing.courts.HearingResult;
import uk.gov.justice.progression.courts.BookingReferenceCourtScheduleIds;
import uk.gov.justice.progression.courts.BookingReferencesAndCourtScheduleIdsStored;
import uk.gov.justice.progression.courts.HearingResulted;
import uk.gov.justice.core.courts.NextHearingsRequested;
import uk.gov.justice.core.courts.ProsecutionCasesResultedV2;
import uk.gov.justice.progression.courts.StoreBookingReferenceCourtScheduleIds;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingResultsCommandHandlerTest {

    private final UtcClock utcClock = new UtcClock();
    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            HearingResulted.class,
            ProsecutionCaseDefendantListingStatusChanged.class,
            NextHearingsRequested.class,
            ProsecutionCasesResultedV2.class,
            UnscheduledNextHearingsRequested.class,
            BookingReferencesAndCourtScheduleIdsStored.class);
    @Mock
    private EventSource eventSource;
    @Mock
    private EventStream eventStream;
    @Mock
    private AggregateService aggregateService;
    @InjectMocks
    private HearingResultsCommandHandler handler;

    private HearingAggregate hearingAggregate;

    @Before
    public void setup() {
        hearingAggregate = new HearingAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
    }

    @Test
    public void shouldProcessHearingResults() throws EventStreamException {
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final NextHearing nextHearing = NextHearing.nextHearing().withListedStartDateTime(ZonedDateTime.now()).build();
        final List<HearingDay> hearingDays = Arrays.asList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(1)).build());

        final HearingResult hearingResult = createCommandPayload(hearingId, caseId, utcClock.now().plusDays(1), nextHearing, hearingDays);

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.hearing-result")
                .withId(randomUUID())
                .build();

        final Envelope<HearingResult> envelope = envelopeFrom(metadata, hearingResult);

        handler.processHearingResults(envelope);

        final List<JsonEnvelope> events = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());
        assertThat(events.size(), is(5));

        checkEventsContentOfHearingResultedAndListingStatusChangedAndProsecutionCasesResulted(hearingId, caseId, events);

        final JsonEnvelope nextHearingRequested = events.get(3);
        assertThat(nextHearingRequested.metadata().name(), is("progression.event.next-hearings-requested"));
        assertThat(nextHearingRequested.payloadAsJsonObject().getJsonObject("hearing").getString("id"), is(hearingId.toString()));
        assertThat(nextHearingRequested.payloadAsJsonObject().getJsonObject("seedingHearing").getString("seedingHearingId"), is(hearingId.toString()));

        final JsonEnvelope unscheduledNextHearingsEvent = events.get(4);
        assertThat(unscheduledNextHearingsEvent.metadata().name(), is("progression.event.unscheduled-next-hearings-requested"));
        assertThat(unscheduledNextHearingsEvent.payloadAsJsonObject().getJsonObject("hearing").getString("id"), is(hearingId.toString()));
        assertThat(unscheduledNextHearingsEvent.payloadAsJsonObject().getJsonObject("seedingHearing").getString("seedingHearingId"), is(hearingId.toString()));
    }

    @Test
    public void shouldProcessHearingResultsWithoutNextHearingsEventsWhenEarliestNextHearingDateIsNotInFuture() throws EventStreamException {
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final NextHearing nextHearing = NextHearing.nextHearing().withListedStartDateTime(ZonedDateTime.now()).build();
        final List<HearingDay> hearingDays = Arrays.asList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(1)).build());

        final HearingResult hearingResult = createCommandPayload(hearingId, caseId, utcClock.now(), nextHearing, hearingDays);

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.hearing-result")
                .withId(randomUUID())
                .build();

        final Envelope<HearingResult> envelope = envelopeFrom(metadata, hearingResult);

        handler.processHearingResults(envelope);

        final List<JsonEnvelope> events = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());
        assertThat(events.size(), is(3));

        checkEventsContentOfHearingResultedAndListingStatusChangedAndProsecutionCasesResulted(hearingId, caseId, events);
    }


    @Test
    public void shouldProcessHearingResultsAndGetNextHearingsRequestedEventsWhenPayloadHasNextHearingPresentWithoutListedStartDateTimeAndHearingDaysPresent() throws EventStreamException {
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final NextHearing nextHearing = NextHearing.nextHearing().build();
        final List<HearingDay> hearingDays = Arrays.asList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now()).build());

        final HearingResult hearingResult = createCommandPayload(hearingId, caseId, ZonedDateTime.now().plusDays(1), nextHearing, hearingDays);

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.hearing-result")
                .withId(randomUUID())
                .build();

        final Envelope<HearingResult> envelope = envelopeFrom(metadata, hearingResult);

        handler.processHearingResults(envelope);

        final List<JsonEnvelope> events = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());
        assertThat(events.size(), is(5));

        checkEventsContentOfHearingResultedAndListingStatusChangedAndProsecutionCasesResulted(hearingId, caseId, events);

        final JsonEnvelope nextHearingRequested = events.get(3);
        assertThat(nextHearingRequested.metadata().name(), is("progression.event.next-hearings-requested"));
        assertThat(nextHearingRequested.payloadAsJsonObject().getJsonObject("hearing").getString("id"), is(hearingId.toString()));

        final JsonEnvelope nextHearingRequested1 = events.get(4);
        assertThat(nextHearingRequested1.metadata().name(), is("progression.event.unscheduled-next-hearings-requested"));
        assertThat(nextHearingRequested1.payloadAsJsonObject().getJsonObject("hearing").getString("id"), is(hearingId.toString()));
    }

    @Test
    public void shouldProcessHearingResultsAndGetNextHearingsRequestedEventsWhenPayloadHasNextHearingPresentWithoutListedStartDateTimeAndHearingDaysAbsent() throws EventStreamException {
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final NextHearing nextHearing = NextHearing.nextHearing().build();

        final HearingResult hearingResult = createCommandPayload(hearingId, caseId, ZonedDateTime.now().plusDays(1), nextHearing, ImmutableList.of());

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.hearing-result")
                .withId(randomUUID())
                .build();

        final Envelope<HearingResult> envelope = envelopeFrom(metadata, hearingResult);

        handler.processHearingResults(envelope);

        final List<JsonEnvelope> events = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());
        assertThat(events.size(), is(5));

        checkEventsContentOfHearingResultedAndListingStatusChangedAndProsecutionCasesResulted(hearingId, caseId, events);

        final JsonEnvelope nextHearingRequested = events.get(3);
        assertThat(nextHearingRequested.metadata().name(), is("progression.event.next-hearings-requested"));
        assertThat(nextHearingRequested.payloadAsJsonObject().getJsonObject("hearing").getString("id"), is(hearingId.toString()));

        final JsonEnvelope nextHearingRequested1 = events.get(4);
        assertThat(nextHearingRequested1.metadata().name(), is("progression.event.unscheduled-next-hearings-requested"));
        assertThat(nextHearingRequested1.payloadAsJsonObject().getJsonObject("hearing").getString("id"), is(hearingId.toString()));
    }

    @Test
    public void shouldProcessHearingResultsWithNextHearingsEventsWhenEarliestNextHearingDateIsInFuture() throws EventStreamException {
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final NextHearing nextHearing = NextHearing.nextHearing().withListedStartDateTime(ZonedDateTime.now()).build();
        final List<HearingDay> hearingDays = Arrays.asList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(1)).build());

        final HearingResult hearingResult = createCommandPayload(hearingId, caseId, ZonedDateTime.now().plusDays(1), nextHearing, hearingDays);

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.hearing-result")
                .withId(randomUUID())
                .build();

        final Envelope<HearingResult> envelope = envelopeFrom(metadata, hearingResult);

        handler.processHearingResults(envelope);

        final List<JsonEnvelope> events = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());
        assertThat(events.size(), is(5));

        checkEventsContentOfHearingResultedAndListingStatusChangedAndProsecutionCasesResulted(hearingId, caseId, events);

        final JsonEnvelope nextHearingRequested = events.get(3);
        assertThat(nextHearingRequested.metadata().name(), is("progression.event.next-hearings-requested"));
        assertThat(nextHearingRequested.payloadAsJsonObject().getJsonObject("hearing").getString("id"), is(hearingId.toString()));

        final JsonEnvelope nextHearingRequested1 = events.get(4);
        assertThat(nextHearingRequested1.metadata().name(), is("progression.event.unscheduled-next-hearings-requested"));
        assertThat(nextHearingRequested1.payloadAsJsonObject().getJsonObject("hearing").getString("id"), is(hearingId.toString()));
    }

    @Test
    public void shouldProcessHearingResultsAndShouldNotGenerateNextHearingRequestedEventWhenNextHearingIsWithInMultiDaysHearing() throws EventStreamException {
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final NextHearing nextHearing = NextHearing.nextHearing().withListedStartDateTime(ZonedDateTime.now().plusDays(1)).build();
        final List<HearingDay> hearingDays = Arrays.asList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(1)).build(),
                HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(2)).build());

        final HearingResult hearingResult = createCommandPayload(hearingId, caseId, ZonedDateTime.now().plusDays(1), nextHearing, hearingDays);

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.hearing-result")
                .withId(randomUUID())
                .build();

        final Envelope<HearingResult> envelope = envelopeFrom(metadata, hearingResult);

        handler.processHearingResults(envelope);

        final List<JsonEnvelope> events = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());
        assertThat(events.size(), is(4));

        checkEventsContentOfHearingResultedAndListingStatusChangedAndProsecutionCasesResulted(hearingId, caseId, events);

        final JsonEnvelope nextHearingRequested1 = events.get(3);
        assertThat(nextHearingRequested1.metadata().name(), is("progression.event.unscheduled-next-hearings-requested"));
        assertThat(nextHearingRequested1.payloadAsJsonObject().getJsonObject("hearing").getString("id"), is(hearingId.toString()));
    }

    @Test
    public void shouldProcessHearingResultsAndShouldGenerateOneNextHearingEventWhenOneNextHearingIsWithInMultiDaysHearingAndAnotherOutside() throws EventStreamException {
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();

        final HearingResult hearingResult = createCommandPayloadWithTwoOffencesAndOneNextHearingIsWithInMultiDaysHearingAndAnotherOutside(hearingId, caseId);

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.hearing-result")
                .withId(randomUUID())
                .build();

        final Envelope<HearingResult> envelope = envelopeFrom(metadata, hearingResult);

        handler.processHearingResults(envelope);

        final List<JsonEnvelope> events = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());
        assertThat(events.size(), is(5));

        checkEventsContentOfHearingResultedAndListingStatusChangedAndProsecutionCasesResulted(hearingId, caseId, events);

        final JsonEnvelope nextHearingRequested = events.get(3);
        assertThat(nextHearingRequested.metadata().name(), is("progression.event.next-hearings-requested"));
        assertThat(nextHearingRequested.payloadAsJsonObject().getJsonObject("hearing").getString("id"), is(hearingId.toString()));
        assertThat(nextHearingRequested.payloadAsJsonObject().getJsonObject("seedingHearing").getString("sittingDay"), is(LocalDate.now().toString()));

        final JsonEnvelope nextHearingRequested1 = events.get(4);
        assertThat(nextHearingRequested1.metadata().name(), is("progression.event.unscheduled-next-hearings-requested"));
        assertThat(nextHearingRequested1.payloadAsJsonObject().getJsonObject("hearing").getString("id"), is(hearingId.toString()));
        assertThat(nextHearingRequested1.payloadAsJsonObject().getJsonObject("seedingHearing").getString("sittingDay"), is(LocalDate.now().toString()));
    }

    @Test
    public void shouldProcessStoreBookingReferencesAndCourtScheduleIdsCommand() throws EventStreamException {
        final UUID hearingId = randomUUID();
        final LocalDate hearingDay = LocalDate.now();
        final UUID bookingId = randomUUID();
        final List<UUID> courtScheduleIds = Arrays.asList(randomUUID(), randomUUID());

        StoreBookingReferenceCourtScheduleIds storeBookingReferenceCourtScheduleIds = StoreBookingReferenceCourtScheduleIds.storeBookingReferenceCourtScheduleIds()
                .withHearingId(hearingId)
                .withHearingDay(hearingDay)
                .withBookingReferenceCourtScheduleIds(Arrays.asList(BookingReferenceCourtScheduleIds.bookingReferenceCourtScheduleIds()
                        .withBookingId(bookingId)
                        .withCourtScheduleIds(courtScheduleIds)
                        .build()))
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.store-booking-reference-court-schedule-ids")
                .withId(randomUUID())
                .build();

        final Envelope<StoreBookingReferenceCourtScheduleIds> envelope = envelopeFrom(metadata, storeBookingReferenceCourtScheduleIds);

        handler.processStoreBookingReferencesWithCourtScheduleIdsCommand(envelope);

        final List<JsonEnvelope> events = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());
        assertThat(events.size(), is(1));

        final JsonEnvelope bookingReferencesAndCourtScheduleIdsStored = events.get(0);
        assertThat(bookingReferencesAndCourtScheduleIdsStored.metadata().name(), is("progression.event.booking-references-and-court-schedule-ids-stored"));
        assertThat(bookingReferencesAndCourtScheduleIdsStored.payloadAsJsonObject().getString("hearingDay"), is(hearingDay.toString()));

        assertThat(bookingReferencesAndCourtScheduleIdsStored.payloadAsJsonObject().getJsonArray("bookingReferenceCourtScheduleIds").size(), is(1));
        assertThat(bookingReferencesAndCourtScheduleIdsStored.payloadAsJsonObject().getJsonArray("bookingReferenceCourtScheduleIds").getJsonObject(0).getString("bookingId"), is(bookingId.toString()));
        assertThat(bookingReferencesAndCourtScheduleIdsStored.payloadAsJsonObject().getJsonArray("bookingReferenceCourtScheduleIds").getJsonObject(0).getJsonArray("courtScheduleIds").size(), is(2));

    }

    private void checkEventsContentOfHearingResultedAndListingStatusChangedAndProsecutionCasesResulted(final UUID hearingId, final UUID caseId, final List<JsonEnvelope> events) {
        final JsonEnvelope hearingResultedEvent = events.get(0);
        assertThat(hearingResultedEvent.metadata().name(), is("progression.event.hearing-resulted"));
        assertThat(hearingResultedEvent.payloadAsJsonObject().getJsonObject("hearing").getString("id"), is(hearingId.toString()));
        assertThat(hearingResultedEvent.payloadAsJsonObject().getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0).getString("id"), is(caseId.toString()));

        final JsonEnvelope listingStatusChangedEvent = events.get(1);
        assertThat(listingStatusChangedEvent.metadata().name(), is("progression.event.prosecutionCase-defendant-listing-status-changed"));
        assertThat(listingStatusChangedEvent.payloadAsJsonObject().getJsonObject("hearing").getString("id"), is(hearingId.toString()));

        final JsonEnvelope prosecutionCaseResultedEvent = events.get(2);
        assertThat(prosecutionCaseResultedEvent.metadata().name(), is("progression.event.prosecution-cases-resulted-v2"));
        assertThat(prosecutionCaseResultedEvent.payloadAsJsonObject().getJsonObject("hearing").getString("id"), is(hearingId.toString()));
        assertThat(prosecutionCaseResultedEvent.payloadAsJsonObject().getString("hearingDay"), is(LocalDate.now().toString()));
    }

    private HearingResult createCommandPayload(final UUID hearingId, final UUID caseId, final ZonedDateTime earliestNextHearingDate, final NextHearing nextHearing, List<HearingDay> hearingDays) {
        return HearingResult.hearingResult()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                                .withId(caseId)
                                .withDefendants(Arrays.asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(Arrays.asList(Offence.offence()
                                                .withId(randomUUID())
                                                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                                                        .withCategory(Category.INTERMEDIARY)
                                                        .withIsUnscheduled(true)
                                                        .withNextHearing(nextHearing)
                                                        .build()))
                                                .build()))
                                        .build()))
                                .build()))
                        .withEarliestNextHearingDate(earliestNextHearingDate)
                        .withHearingDays(hearingDays)
                        .build())
                .withHearingDay(LocalDate.now())
                .build();
    }

    private HearingResult createCommandPayloadWithTwoOffencesAndOneNextHearingIsWithInMultiDaysHearingAndAnotherOutside(final UUID hearingId, final UUID caseId) {
        return HearingResult.hearingResult()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                                .withId(caseId)
                                .withDefendants(Arrays.asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(Arrays.asList(Offence.offence()
                                                        .withId(randomUUID())
                                                        .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                                                                .withCategory(Category.INTERMEDIARY)
                                                                .withIsUnscheduled(true)
                                                                .withNextHearing(NextHearing.nextHearing().withListedStartDateTime(ZonedDateTime.now()).build())
                                                                .build()))
                                                        .build(),
                                                Offence.offence()
                                                        .withId(randomUUID())
                                                        .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                                                                .withCategory(Category.INTERMEDIARY)
                                                                .withIsUnscheduled(true)
                                                                .withNextHearing(NextHearing.nextHearing().withListedStartDateTime(ZonedDateTime.now().plusDays(1)).build())
                                                                .build()))
                                                        .build()))
                                        .build()))
                                .build()))
                        .withEarliestNextHearingDate(ZonedDateTime.now().plusDays(1))
                        .withHearingDays(Arrays.asList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(1)).build(),
                                HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(2)).build()))
                        .build())
                .withHearingDay(LocalDate.now())
                .build();
    }
}
