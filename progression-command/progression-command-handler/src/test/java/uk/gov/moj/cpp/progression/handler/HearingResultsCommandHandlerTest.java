package uk.gov.moj.cpp.progression.handler;

import static java.util.Collections.singletonList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.hearing.courts.HearingResult.hearingResult;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.progression.test.CoreTestTemplates.CoreTemplateArguments.toMap;
import static uk.gov.moj.cpp.progression.test.CoreTestTemplates.defaultArguments;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultCategory;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.NextHearing;
import uk.gov.justice.core.courts.NextHearingsRequested;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChangedV2;
import uk.gov.justice.core.courts.ProsecutionCasesResultedV2;
import uk.gov.justice.core.courts.ReportingRestriction;
import uk.gov.justice.core.courts.UnscheduledNextHearingsRequested;
import uk.gov.justice.hearing.courts.HearingResult;
import uk.gov.justice.progression.courts.BookingReferenceCourtScheduleIds;
import uk.gov.justice.progression.courts.BookingReferencesAndCourtScheduleIdsStored;
import uk.gov.justice.progression.courts.HearingResulted;
import uk.gov.justice.progression.courts.StoreBookingReferenceCourtScheduleIds;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.GroupCaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.test.CoreTestTemplates;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingResultsCommandHandlerTest {

    private final UtcClock utcClock = new UtcClock();
    private static final String SEXUAL_OFFENCE_RR_DESCRIPTION = "Complainant's anonymity protected by virtue of Section 1 of the Sexual Offences Amendment Act 1992";
    private static final String YOUTH_RESTRICTION = "Section 49 of the Children and Young Persons Act 1933 applies";
    private static final String MANUAL_RESTRICTION = "Order made under Section 11 of the Contempt of Court Act 1981";

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            HearingResulted.class,
            ProsecutionCaseDefendantListingStatusChangedV2.class,
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

    @Mock
    private GroupCaseAggregate groupCaseAggregate;

    @Mock
    private CaseAggregate caseAggregate;

    @Mock
    private Requester requester;

    @BeforeEach
    public void setup() {
        hearingAggregate = new HearingAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
    }

    @Test
    public void shouldHandleProcessUpdateDefendantListingStatusForGroupCases() throws EventStreamException {
        final Offence offence = Offence.offence()
                .withId(randomUUID())
                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                        .withIsAdjournmentResult(false)
                        .withCategory(JudicialResultCategory.FINAL).build(), JudicialResult.judicialResult()
                        .withIsAdjournmentResult(false)
                        .withCategory(JudicialResultCategory.ANCILLARY).build()))
                .build();
        final Defendant defendant = Defendant.defendant()
                .withId(randomUUID())
                .withOffences(Arrays.asList(offence))
                .build();

        final List<HearingDay> hearingDays = Arrays.asList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(1)).build(),
                HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(2)).build());
        List<Defendant> defendantList = new ArrayList<>();
        defendantList.add(defendant);

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(randomUUID())
                .withIsGroupMaster(Boolean.TRUE)
                .withCaseStatus(CaseStatusEnum.READY_FOR_REVIEW.getDescription())
                .withCpsOrganisation("A01")
                .withDefendants(defendantList).build();
        final HearingResult hearingResult = hearingResult()
                .withHearing(Hearing.hearing()
                        .withIsGroupProceedings(true)
                        .withEarliestNextHearingDate(ZonedDateTime.now().minusDays(2))
                        .withHearingDays(hearingDays)
                        .withProsecutionCases(Arrays.asList(prosecutionCase))
                        .withId(UUID.randomUUID())
                        .build())
                .build();
        final JsonEnvelope resultsEnvelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(),
                createObjectBuilder().add("resultDefinitions", createArrayBuilder().add(createObjectBuilder().add("id", randomUUID().toString()))));

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
        when(aggregateService.get(eventStream, GroupCaseAggregate.class)).thenReturn(groupCaseAggregate);
        when(requester.request(any(JsonEnvelope.class))).thenReturn(resultsEnvelope);

        hearingAggregate.apply(hearingResult.getHearing());

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.process-hearing-results")
                .withId(randomUUID())
                .build();
        final Envelope<HearingResult> envelope = envelopeFrom(metadata, hearingResult);

        handler.processHearingResults(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        final List<Envelope> envelopes = envelopeStream.map(value -> (Envelope) value).collect(Collectors.toList());

        final JsonEnvelope  hearingResultedEnvelope = (JsonEnvelope)envelopes.stream().filter(env -> env.metadata().name().equals("progression.event.hearing-resulted")).findFirst().get();

        assertThat(hearingResultedEnvelope, jsonEnvelope(metadata().withName("progression.event.hearing-resulted"), payloadIsJson(CoreMatchers.allOf(
                withJsonPath("$.hearing", notNullValue()),
                withJsonPath("$.hearing.isGroupProceedings",
                        is(true)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].proceedingsConcluded",
                        is(true)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].proceedingsConcluded",
                        is(true)),
                withJsonPath("$.hearing.prosecutionCases[0].caseStatus", is(CaseStatusEnum.INACTIVE.getDescription())),
                withJsonPath("$.hearing.prosecutionCases[0].cpsOrganisation", is("A01")))

        )));
    }

    @Test
    public void shouldProcessHearingResults() throws EventStreamException {
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final NextHearing nextHearing = NextHearing.nextHearing().withListedStartDateTime(new UtcClock().now()).withAdjournmentReason("AdjournmentReason").build();
        final List<HearingDay> hearingDays = Arrays.asList(HearingDay.hearingDay().withSittingDay(new UtcClock().now().plusDays(1)).build());

        final HearingResult hearingResult = createCommandPayload(hearingId, caseId, utcClock.now().plusDays(1), nextHearing, hearingDays);

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.hearing-result")
                .withId(randomUUID())
                .build();

        final Envelope<HearingResult> envelope = envelopeFrom(metadata, hearingResult);
        final JsonEnvelope resultsEnvelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(),
                createObjectBuilder().add("resultDefinitions", createArrayBuilder().add(createObjectBuilder().add("id", randomUUID().toString()))));

        when(requester.request(any(JsonEnvelope.class))).thenReturn(resultsEnvelope);

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
    public void shouldProcessHearingResultsWithoutNextHearingsOnReshareWhenNextHearingResultsNotAmended() throws EventStreamException {
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final NextHearing nextHearing = NextHearing.nextHearing().withListedStartDateTime(new UtcClock().now()).withAdjournmentReason("AdjournmentReason").build();
        final List<HearingDay> hearingDays = Arrays.asList(HearingDay.hearingDay().withSittingDay(new UtcClock().now().plusDays(1)).build());

        final HearingResult hearingResult = createCommandPayload(hearingId, caseId, utcClock.now().plusDays(1), nextHearing, hearingDays, false);

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.hearing-result")
                .withId(randomUUID())
                .build();

        final Envelope<HearingResult> envelope = envelopeFrom(metadata, hearingResult);

        final JsonEnvelope resultsEnvelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(),
                createObjectBuilder().add("resultDefinitions", createArrayBuilder().add(createObjectBuilder().add("id", randomUUID().toString()))));

        when(requester.request(any(JsonEnvelope.class))).thenReturn(resultsEnvelope);

        handler.processHearingResults(envelope);

        final List<JsonEnvelope> events = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());
        assertThat(events.size(), is(3));

        checkEventsContentOfHearingResultedAndListingStatusChangedAndProsecutionCasesResulted(hearingId, caseId, events);
    }

    @Test
    public void shouldProcessHearingResultsWithNextHearingsEventsWhenEarliestNextHearingDateIsNotInFutureAndSingleDayHearing() throws EventStreamException {
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final NextHearing nextHearing = NextHearing.nextHearing().withListedStartDateTime(new UtcClock().now()).withAdjournmentReason("AdjournmentReason").build();
        final List<HearingDay> hearingDays = Arrays.asList(HearingDay.hearingDay().withSittingDay(new UtcClock().now().plusDays(1)).build());

        final HearingResult hearingResult = createCommandPayload(hearingId, caseId, utcClock.now(), nextHearing, hearingDays);

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.hearing-result")
                .withId(randomUUID())
                .build();

        final Envelope<HearingResult> envelope = envelopeFrom(metadata, hearingResult);

        final JsonEnvelope resultsEnvelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(),
                createObjectBuilder().add("resultDefinitions", createArrayBuilder().add(createObjectBuilder().add("id", randomUUID().toString()))));
        when(requester.request(any())).thenReturn(resultsEnvelope);

        handler.processHearingResults(envelope);

        final List<JsonEnvelope> events = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());
        assertThat(events.size(), is(5));

        checkEventsContentOfHearingResultedAndListingStatusChangedAndProsecutionCasesResulted(hearingId, caseId, events);
    }

    @Test
    public void shouldProcessHearingResultsWithoutNextHearingsEventsWhenEarliestNextHearingDateIsNotInFutureAndMultiDayHearing() throws EventStreamException {
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final NextHearing nextHearing = NextHearing.nextHearing().withListedStartDateTime(new UtcClock().now()).withAdjournmentReason("AdjournmentReason").build();
        final List<HearingDay> hearingDays = Arrays.asList(HearingDay.hearingDay().withSittingDay(new UtcClock().now().plusDays(1)).build(),
                HearingDay.hearingDay().withSittingDay(new UtcClock().now().plusDays(2)).build());

        final HearingResult hearingResult = createCommandPayload(hearingId, caseId, utcClock.now(), nextHearing, hearingDays);

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.hearing-result")
                .withId(randomUUID())
                .build();

        final Envelope<HearingResult> envelope = envelopeFrom(metadata, hearingResult);
        final JsonEnvelope resultsEnvelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(),
                createObjectBuilder().add("resultDefinitions", createArrayBuilder().add(createObjectBuilder().add("id", randomUUID().toString()))));
        when(requester.request(any())).thenReturn(resultsEnvelope);

        handler.processHearingResults(envelope);

        final List<JsonEnvelope> events = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());
        assertThat(events.size(), is(3));

        checkEventsContentOfHearingResultedAndListingStatusChangedAndProsecutionCasesResulted(hearingId, caseId, events);
    }


    @Test
    public void shouldProcessHearingResultsAndGetNextHearingsRequestedEventsWhenPayloadHasNextHearingPresentWithoutListedStartDateTimeAndHearingDaysPresent() throws EventStreamException {
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final NextHearing nextHearing = NextHearing.nextHearing().withAdjournmentReason("AdjournmentReason").build();
        final List<HearingDay> hearingDays = Arrays.asList(HearingDay.hearingDay().withSittingDay(new UtcClock().now()).build());

        final HearingResult hearingResult = createCommandPayload(hearingId, caseId, ZonedDateTime.now().plusDays(1), nextHearing, hearingDays);

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.hearing-result")
                .withId(randomUUID())
                .build();

        final Envelope<HearingResult> envelope = envelopeFrom(metadata, hearingResult);

        final JsonEnvelope resultsEnvelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(),
                createObjectBuilder().add("resultDefinitions", createArrayBuilder().add(createObjectBuilder().add("id", randomUUID().toString()))));
        when(requester.request(any())).thenReturn(resultsEnvelope);

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
        final NextHearing nextHearing = NextHearing.nextHearing().withAdjournmentReason("AdjournmentReason").build();

        final HearingResult hearingResult = createCommandPayload(hearingId, caseId, ZonedDateTime.now().plusDays(1), nextHearing, ImmutableList.of());

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.hearing-result")
                .withId(randomUUID())
                .build();

        final Envelope<HearingResult> envelope = envelopeFrom(metadata, hearingResult);

        final JsonEnvelope resultsEnvelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(),
                createObjectBuilder().add("resultDefinitions", createArrayBuilder().add(createObjectBuilder().add("id", randomUUID().toString()))));

        when(requester.request(any(JsonEnvelope.class))).thenReturn(resultsEnvelope);

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
        final NextHearing nextHearing = NextHearing.nextHearing().withListedStartDateTime(ZonedDateTime.now()).withAdjournmentReason("AdjournmentReason").build();
        final List<HearingDay> hearingDays = Arrays.asList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(1)).build());

        final HearingResult hearingResult = createCommandPayload(hearingId, caseId, ZonedDateTime.now().plusDays(1), nextHearing, hearingDays);

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.hearing-result")
                .withId(randomUUID())
                .build();

        final Envelope<HearingResult> envelope = envelopeFrom(metadata, hearingResult);

        final JsonEnvelope resultsEnvelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(),
                createObjectBuilder().add("resultDefinitions", createArrayBuilder().add(createObjectBuilder().add("id", randomUUID().toString()))));
        when(requester.request(any())).thenReturn(resultsEnvelope);

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
        final NextHearing nextHearing = NextHearing.nextHearing().withListedStartDateTime(ZonedDateTime.now().plusDays(1)).withAdjournmentReason("AdjournmentReason").build();
        final List<HearingDay> hearingDays = Arrays.asList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(1)).build(),
                HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(2)).build());

        final HearingResult hearingResult = createCommandPayload(hearingId, caseId, ZonedDateTime.now().plusDays(1), nextHearing, hearingDays);

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.hearing-result")
                .withId(randomUUID())
                .build();

        final Envelope<HearingResult> envelope = envelopeFrom(metadata, hearingResult);

        final JsonEnvelope resultsEnvelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(),
                createObjectBuilder().add("resultDefinitions", createArrayBuilder().add(createObjectBuilder().add("id", randomUUID().toString()))));
        when(requester.request(any())).thenReturn(resultsEnvelope);

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

        final JsonEnvelope resultsEnvelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(),
                createObjectBuilder().add("resultDefinitions", createArrayBuilder().add(createObjectBuilder().add("id", randomUUID().toString()))));
        when(requester.request(any())).thenReturn(resultsEnvelope);

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
    public void shouldProcessHearingResultsWithoutNextHearingForCaseForAutoApplicationCreation() throws EventStreamException {
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final NextHearing nextHearing = NextHearing.nextHearing().withApplicationTypeCode("SE20508").withListedStartDateTime(ZonedDateTime.now()).withAdjournmentReason("AdjournmentReason").build();
        final List<HearingDay> hearingDays = Arrays.asList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(1)).build());

        final HearingResult hearingResult = createCommandPayload(hearingId, caseId, utcClock.now().plusDays(1), nextHearing, hearingDays);

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.hearing-result")
                .withId(randomUUID())
                .build();

        final Envelope<HearingResult> envelope = envelopeFrom(metadata, hearingResult);

        final JsonEnvelope resultsEnvelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(),
                createObjectBuilder().add("resultDefinitions", createArrayBuilder().add(createObjectBuilder().add("id", randomUUID().toString()))));
        when(requester.request(any())).thenReturn(resultsEnvelope);

        handler.processHearingResults(envelope);

        final List<JsonEnvelope> events = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());
        assertThat(events.size(), is(4));

        checkEventsContentOfHearingResultedAndListingStatusChangedAndProsecutionCasesResulted(hearingId, caseId, events);

        final JsonEnvelope unscheduledNextHearingsEvent = events.get(3);
        assertThat(unscheduledNextHearingsEvent.metadata().name(), is("progression.event.unscheduled-next-hearings-requested"));
        assertThat(unscheduledNextHearingsEvent.payloadAsJsonObject().getJsonObject("hearing").getString("id"), is(hearingId.toString()));
        assertThat(unscheduledNextHearingsEvent.payloadAsJsonObject().getJsonObject("seedingHearing").getString("seedingHearingId"), is(hearingId.toString()));
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

    @Test
    public void shouldProcessHearingResultsWithAddingRRFlagsToCurrentOnes() throws Exception {
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID offenceId = randomUUID();
        this.hearingAggregate = getHearingAggregateWithOneOffenceandTwoReportingRestrictions(offenceId);

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);
        final JsonEnvelope resultsEnvelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(),
                createObjectBuilder().add("resultDefinitions", createArrayBuilder().add(createObjectBuilder().add("id", randomUUID().toString()))));



        final HearingResult hearingResult = createCommandPayloadWithOneOffenceWithManualRRFlagAndOneNextHearingIsWithInMultiDaysHearingAndAnotherOutside(hearingId, caseId, offenceId);

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.hearing-result")
                .withId(randomUUID())
                .build();
        when(requester.request(any(JsonEnvelope.class))).thenReturn(resultsEnvelope);

        final Envelope<HearingResult> envelope = envelopeFrom(metadata, hearingResult);


        handler.processHearingResults(envelope);

        final List<JsonEnvelope> events = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());
        assertThat(events.size(), is(3));

        checkEventsContentOfHearingResultedWithManuallyAddedReportingRestrictions(hearingId, caseId, events, 1);

    }

    private void checkEventsContentOfHearingResultedAndListingStatusChangedAndProsecutionCasesResulted(final UUID hearingId, final UUID caseId, final List<JsonEnvelope> events) {

        final JsonEnvelope listingStatusChangedEvent = events.get(0);
        assertThat(listingStatusChangedEvent.metadata().name(), is("progression.event.prosecutionCase-defendant-listing-status-changed-v2"));
        assertThat(listingStatusChangedEvent.payloadAsJsonObject().getJsonObject("hearing").getString("id"), is(hearingId.toString()));
        assertThat(listingStatusChangedEvent.payloadAsJsonObject().getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0).getString("lastAdjournDate"), is("-999999999-01-01"));
        assertThat(listingStatusChangedEvent.payloadAsJsonObject().getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0).getString("lastAdjournedHearingType"), is("AdjournmentReason"));


        final JsonEnvelope hearingResultedEvent = events.get(1);
        assertThat(hearingResultedEvent.metadata().name(), is("progression.event.hearing-resulted"));
        assertThat(hearingResultedEvent.payloadAsJsonObject().getJsonObject("hearing").getString("id"), is(hearingId.toString()));
        assertThat(hearingResultedEvent.payloadAsJsonObject().getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0).getString("id"), is(caseId.toString()));
        assertThat(hearingResultedEvent.payloadAsJsonObject().getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0).getString("lastAdjournDate"), is("-999999999-01-01"));
        assertThat(hearingResultedEvent.payloadAsJsonObject().getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0).getString("lastAdjournedHearingType"), is("AdjournmentReason"));

        final JsonEnvelope prosecutionCaseResultedEvent = events.get(2);
        assertThat(prosecutionCaseResultedEvent.metadata().name(), is("progression.event.prosecution-cases-resulted-v2"));
        assertThat(prosecutionCaseResultedEvent.payloadAsJsonObject().getJsonObject("hearing").getString("id"), is(hearingId.toString()));
        assertThat(prosecutionCaseResultedEvent.payloadAsJsonObject().getString("hearingDay"), is(LocalDate.now().toString()));
        assertThat(prosecutionCaseResultedEvent.payloadAsJsonObject().getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0).getString("lastAdjournDate"), is("-999999999-01-01"));
        assertThat(prosecutionCaseResultedEvent.payloadAsJsonObject().getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0).getString("lastAdjournedHearingType"), is("AdjournmentReason"));

    }

    private void checkEventsContentOfHearingResultedWithManuallyAddedReportingRestrictions(final UUID hearingId, final UUID caseId, final List<JsonEnvelope> events, final int expectedNumberOfReportingRestrictions) {

        final JsonEnvelope listingStatusChangedEvent = events.get(0);
        assertThat(listingStatusChangedEvent.metadata().name(), is("progression.event.prosecutionCase-defendant-listing-status-changed-v2"));
        assertThat(listingStatusChangedEvent.payloadAsJsonObject().getJsonObject("hearing").getString("id"), is(hearingId.toString()));
        assertThat(listingStatusChangedEvent.payloadAsJsonObject().getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0).getString("lastAdjournDate"), is("-999999999-01-01"));
        assertThat(listingStatusChangedEvent.payloadAsJsonObject().getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0).getString("lastAdjournedHearingType"), is("AdjournmentReason"));


        final JsonEnvelope hearingResultedEvent = events.get(1);
        assertThat(hearingResultedEvent.metadata().name(), is("progression.event.hearing-resulted"));
        assertThat(hearingResultedEvent.payloadAsJsonObject().getJsonObject("hearing").getString("id"), is(hearingId.toString()));
        assertThat(hearingResultedEvent.payloadAsJsonObject().getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0).getString("id"), is(caseId.toString()));
        assertThat(hearingResultedEvent.payloadAsJsonObject().getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0).getString("lastAdjournDate"), is("-999999999-01-01"));
        assertThat(hearingResultedEvent.payloadAsJsonObject().getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0).getString("lastAdjournedHearingType"), is("AdjournmentReason"));
        assertThat(hearingResultedEvent.payloadAsJsonObject().getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0).getJsonArray("reportingRestrictions").size(), is(expectedNumberOfReportingRestrictions));


        final JsonEnvelope prosecutionCaseResultedEvent = events.get(2);
        assertThat(prosecutionCaseResultedEvent.metadata().name(), is("progression.event.prosecution-cases-resulted-v2"));
        assertThat(prosecutionCaseResultedEvent.payloadAsJsonObject().getJsonObject("hearing").getString("id"), is(hearingId.toString()));
        assertThat(prosecutionCaseResultedEvent.payloadAsJsonObject().getString("hearingDay"), is(LocalDate.now().toString()));
        assertThat(prosecutionCaseResultedEvent.payloadAsJsonObject().getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0).getString("lastAdjournDate"), is("-999999999-01-01"));
        assertThat(prosecutionCaseResultedEvent.payloadAsJsonObject().getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0).getString("lastAdjournedHearingType"), is("AdjournmentReason"));

    }

    private HearingAggregate getHearingAggregateWithOneOffenceandTwoReportingRestrictions(final UUID offenceId) {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final Hearing hearing = CoreTestTemplates.hearing(defaultArguments()
                .setJurisdictionType(JurisdictionType.CROWN)
                .setStructure(toMap(caseId, toMap(defendantId, singletonList(offenceId))))
                .setReportingRestrictions(Boolean.TRUE)
                .setConvicted(false)).build();

        hearingAggregate.apply(createHearingResulted(hearing));
        return hearingAggregate;
    }

    private HearingResulted createHearingResulted(final Hearing hearing) {
        return HearingResulted.hearingResulted().withHearing(hearing).build();
    }


    private HearingResult createCommandPayload(final UUID hearingId, final UUID caseId, final ZonedDateTime earliestNextHearingDate, final NextHearing nextHearing, List<HearingDay> hearingDays) {
        return createCommandPayload(hearingId, caseId, earliestNextHearingDate, nextHearing, hearingDays, true);
    }

    private HearingResult createCommandPayload(final UUID hearingId, final UUID caseId, final ZonedDateTime earliestNextHearingDate, final NextHearing nextHearing, List<HearingDay> hearingDays, final boolean hasAmendedResults) {
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
                                                        .withCategory(JudicialResultCategory.INTERMEDIARY)
                                                        .withIsUnscheduled(true)
                                                        .withNextHearing(nextHearing)
                                                        .withIsAdjournmentResult(true)
                                                        .withOrderedDate(LocalDate.MIN)
                                                        .withIsNewAmendment(hasAmendedResults)
                                                        .build()))
                                                .build()))
                                        .build()))
                                .build()))
                        .withEarliestNextHearingDate(earliestNextHearingDate)
                        .withHearingDays(hearingDays)
                        .withType(HearingType.hearingType().withDescription("description").build())
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
                                                                .withCategory(JudicialResultCategory.INTERMEDIARY)
                                                                .withIsUnscheduled(true)
                                                                .withNextHearing(NextHearing.nextHearing().withListedStartDateTime(ZonedDateTime.now())
                                                                        .withAdjournmentReason("AdjournmentReason")
                                                                        .build())
                                                                .withIsAdjournmentResult(true)
                                                                .withIsNewAmendment(true)
                                                                .withOrderedDate(LocalDate.MIN)
                                                                .build()))
                                                        .build(),
                                                Offence.offence()
                                                        .withId(randomUUID())
                                                        .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                                                                .withCategory(JudicialResultCategory.INTERMEDIARY)
                                                                .withIsUnscheduled(true)
                                                                .withNextHearing(NextHearing.nextHearing().withListedStartDateTime(ZonedDateTime.now().plusDays(1))
                                                                        .withAdjournmentReason("AdjournmentReason")
                                                                        .build())
                                                                .withIsAdjournmentResult(false)
                                                                .withIsNewAmendment(true)
                                                                .build()))
                                                        .build()))
                                        .build()))
                                .build()))
                        .withEarliestNextHearingDate(ZonedDateTime.now().plusDays(1))
                        .withHearingDays(Arrays.asList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(1)).build(),
                                HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(2)).build()))
                        .withType(HearingType.hearingType().withDescription("description").build())
                        .build())
                .withHearingDay(LocalDate.now())
                .build();
    }

    //Not in use?
    private HearingResult createCommandPayloadWithOneOffencesWithTwoRRFlagsAndOneNextHearingIsWithInMultiDaysHearingAndAnotherOutside(final UUID hearingId, final UUID caseId, final UUID offenceId) {
        return HearingResult.hearingResult()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                                .withId(caseId)
                                .withDefendants(Arrays.asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(Arrays.asList(Offence.offence()
                                                .withId(offenceId)
                                                .withReportingRestrictions(Stream.of(ReportingRestriction.reportingRestriction().withId(randomUUID()).withLabel(SEXUAL_OFFENCE_RR_DESCRIPTION).withOrderedDate(LocalDate.now()).build(),
                                                        ReportingRestriction.reportingRestriction().withId(randomUUID()).withLabel(YOUTH_RESTRICTION).withOrderedDate(LocalDate.now()).build()).collect(Collectors.toList()))
                                                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                                                        .withCategory(JudicialResultCategory.INTERMEDIARY)
                                                        .withIsUnscheduled(true)
                                                        .withNextHearing(NextHearing.nextHearing().withListedStartDateTime(ZonedDateTime.now())
                                                                .withAdjournmentReason("AdjournmentReason")
                                                                .build())
                                                        .withIsAdjournmentResult(true)
                                                        .withOrderedDate(LocalDate.MIN)
                                                        .build()))
                                                .build()
                                        ))
                                        .build()))
                                .build()))
                        .withEarliestNextHearingDate(ZonedDateTime.now().plusDays(1))
                        .withHearingDays(Arrays.asList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(1)).build(),
                                HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(2)).build()))
                        .withType(HearingType.hearingType().withDescription("description").build())
                        .build())
                .withHearingDay(LocalDate.now())
                .build();
    }

    private HearingResult createCommandPayloadWithOneOffenceWithManualRRFlagAndOneNextHearingIsWithInMultiDaysHearingAndAnotherOutside(final UUID hearingId, final UUID caseId, final UUID offenceId) {
        return HearingResult.hearingResult()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                                .withId(caseId)
                                .withDefendants(Arrays.asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(Arrays.asList(Offence.offence()
                                                .withId(offenceId)
                                                .withReportingRestrictions(Stream.of(ReportingRestriction.reportingRestriction().withId(randomUUID()).withLabel(MANUAL_RESTRICTION).withOrderedDate(LocalDate.now()).build()).collect(Collectors.toList()))
                                                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                                                        .withCategory(JudicialResultCategory.INTERMEDIARY)
                                                        .withIsUnscheduled(true)
                                                        .withNextHearing(NextHearing.nextHearing().withListedStartDateTime(ZonedDateTime.now())
                                                                .withAdjournmentReason("AdjournmentReason")
                                                                .build())
                                                        .withIsAdjournmentResult(true)
                                                        .withOrderedDate(LocalDate.MIN)
                                                        .build()))
                                                .build()
                                        ))
                                        .build()))
                                .build()))
                        .withEarliestNextHearingDate(ZonedDateTime.now().plusDays(1))
                        .withHearingDays(Arrays.asList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(1)).build(),
                                HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(2)).build()))
                        .withType(HearingType.hearingType().withDescription("description").build())
                        .build())
                .withHearingDay(LocalDate.now())
                .build();
    }
}
