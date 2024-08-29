package uk.gov.moj.cpp.progression.event;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantCase;
import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.HearingUnscheduledListingNeeds;
import uk.gov.justice.core.courts.InitiateApplicationForCaseRequested;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.ListUnscheduledNextHearings;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.NextHearing;
import uk.gov.justice.core.courts.NextHearingsRequested;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ProsecutionCasesResultedV2;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.core.courts.UnscheduledNextHearingsRequested;
import uk.gov.justice.core.progression.courts.HearingForApplicationCreated;
import uk.gov.justice.core.progression.courts.HearingForApplicationCreatedV2;
import uk.gov.justice.listing.courts.ListNextHearings;
import uk.gov.justice.listing.courts.ListNextHearingsV3;
import uk.gov.justice.progression.courts.BookingReferenceCourtScheduleIds;
import uk.gov.justice.progression.courts.StoreBookingReferenceCourtScheduleIds;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.random.DateGenerator;
import uk.gov.justice.services.test.utils.core.random.ZonedDateTimeGenerator;
import uk.gov.moj.cpp.progression.helper.HearingResultUnscheduledListingHelper;
import uk.gov.moj.cpp.progression.helper.SummonsHelper;
import uk.gov.moj.cpp.progression.helper.UnscheduledCourtHearingListTransformer;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.transformer.HearingToHearingListingNeedsTransformer;
import uk.gov.moj.cpp.progression.transformer.ListCourtHearingTransformer;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)

public class HearingResultedEventProcessorTest {

    @InjectMocks
    private HearingResultedEventProcessor eventProcessor;

    @Mock
    private Sender sender;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private ListingService listingService;

    @Mock
    private HearingToHearingListingNeedsTransformer hearingToHearingListingNeedsTransformer;

    @Mock
    private UnscheduledCourtHearingListTransformer unscheduledCourtHearingListTransformer;

    @Mock
    private HearingResultUnscheduledListingHelper hearingResultUnscheduledListingHelper;

    @Spy
    private ListCourtHearingTransformer listCourtHearingTransformer;

    @Mock
    private SummonsHelper summonsHelper;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @Spy
    @InjectMocks
    private final ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(objectMapper);

    @Spy
    @InjectMocks
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @Captor
    private ArgumentCaptor<Envelope> envelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<ProsecutionCase> prosecutionCaseArgumentCaptor;

    @Captor
    private ArgumentCaptor<CourtCentre> courtCentreArgumentCaptor;

    @Captor
    private ArgumentCaptor<List<CourtApplication>> courtApplicationsArgumentCaptor;

    @Captor
    private ArgumentCaptor<List<DefendantJudicialResult>> defendantJudicialResultArgumentCaptor;

    @Captor
    private ArgumentCaptor<ListNextHearingsV3> listNextHearingsArgumentCaptor;

    @Captor
    private ArgumentCaptor<List<HearingListingNeeds>> hearingsArgumentCaptor;

    @Captor
    private ArgumentCaptor<ListUnscheduledNextHearings> listUnscheduledNextHearingsArgumentCaptor;

    @Captor
    private ArgumentCaptor<List<Hearing>> hearingArgumentCaptor;

    @Captor
    private ArgumentCaptor<StoreBookingReferenceCourtScheduleIds> storeBookingReferenceCourtScheduleIdsArgumentCaptor;

    @Captor
    private ArgumentCaptor<Set<UUID>> hearingUnscheduledListingNeedsCaptor;

    @Captor
    private ArgumentCaptor<Hearing> hearingCaptor;

    @Captor
    private ArgumentCaptor<ListCourtHearing> listCourtHearingCaptor;
    @Captor
    private ArgumentCaptor<UUID> hearingIdCaptor;
    @Captor
    private ArgumentCaptor<HearingType> hearingTypeCaptor;
    @Captor
    private ArgumentCaptor<JurisdictionType> jurisdictionTypeCaptor;
    @Captor
    private ArgumentCaptor<Boolean> isBoxHearingCaptor;

    @Test
    public void shouldIssueCommandToProcessHearingResultsWhenHearingResultedReceived() {
        final Hearing hearing = Hearing.hearing().withId(randomUUID()).build();
        final JsonObject publicEventPayload = Json.createObjectBuilder().add("hearing", objectToJsonObjectConverter.convert(hearing))
                .add("sharedTime", new UtcClock().now().toString())
                .add("hearingDay", LocalDate.now().toString()).build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("public.events.hearing.hearing-resulted"),
                objectToJsonObjectConverter.convert(publicEventPayload));

        this.eventProcessor.handlePublicHearingResulted(event);

        verify(sender).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("progression.command.process-hearing-results"));
        verify(summonsHelper).initiateSummonsProcess(event, hearing);
    }

    @Test
    public void shouldUpdateDriveNumberWhenHearingResultedReceived() {
        final CourtApplicationParty courtApplicationParty = CourtApplicationParty.courtApplicationParty()
                .withMasterDefendant(MasterDefendant.masterDefendant()
                        .withMasterDefendantId(randomUUID())
                        .withDefendantCase(Arrays.asList(DefendantCase.defendantCase()
                                .withCaseId(randomUUID())
                                .withDefendantId(randomUUID())
                                .withCaseReference("URN1")
                                .build(),DefendantCase.defendantCase()
                                .withCaseId(randomUUID())
                                .withDefendantId(randomUUID())
                                .withCaseReference("URN2")
                                .build())
                                )
                        .withPersonDefendant(PersonDefendant.personDefendant()
                                .withDriverNumber("DVL1234")
                                .build())
                        .build())
                .build();

        final Hearing hearing = Hearing.hearing().withId(randomUUID())
                .withCourtApplications(singletonList(CourtApplication.courtApplication()
                        .withApplicant(courtApplicationParty)
                        .withSubject(courtApplicationParty)
                        .build())).build();
        final JsonObject publicEventPayload = Json.createObjectBuilder().add("hearing", objectToJsonObjectConverter.convert(hearing))
                .add("sharedTime", new UtcClock().now().toString())
                .add("hearingDay", LocalDate.now().toString()).build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("public.events.hearing.hearing-resulted"),
                objectToJsonObjectConverter.convert(publicEventPayload));

        this.eventProcessor.handlePublicHearingResulted(event);

        verify(sender, times(3)).send(envelopeArgumentCaptor.capture());

        Envelope command = envelopeArgumentCaptor.getAllValues().get(0);
        assertThat(command.metadata().name(), is("progression.command.process-hearing-results"));
        verify(summonsHelper).initiateSummonsProcess(event, hearing);

        command = envelopeArgumentCaptor.getAllValues().get(1);
        JsonObject payload = (JsonObject) command.payload();
        assertThat(command.metadata().name(), is("progression.update-case-defendant-with-driver-number"));
        assertThat(payload.getString("prosecutionCaseId"), is(hearing.getCourtApplications().get(0).getApplicant().getMasterDefendant().getDefendantCase().get(0).getCaseId().toString()));
        assertThat(payload.getString("defendantId"), is(hearing.getCourtApplications().get(0).getApplicant().getMasterDefendant().getDefendantCase().get(0).getDefendantId().toString()));
        assertThat(payload.getString("driverNumber"), is("DVL1234"));

        command = envelopeArgumentCaptor.getAllValues().get(2);
        payload = (JsonObject) command.payload();
        assertThat(command.metadata().name(), is("progression.update-case-defendant-with-driver-number"));
        assertThat(payload.getString("prosecutionCaseId"), is(hearing.getCourtApplications().get(0).getApplicant().getMasterDefendant().getDefendantCase().get(1).getCaseId().toString()));
        assertThat(payload.getString("defendantId"), is(hearing.getCourtApplications().get(0).getApplicant().getMasterDefendant().getDefendantCase().get(1).getDefendantId().toString()));
        assertThat(payload.getString("driverNumber"), is("DVL1234"));
    }

    @Test
    public void shouldIssueCommandsToUpdateCasesWhenProsecutionCaseResultedReceived() {
        final UUID applicationId = UUID.randomUUID();
        final UUID caseId1 = UUID.randomUUID();
        final UUID caseId2 = UUID.randomUUID();
        final UUID hearingId = randomUUID();
        final ProsecutionCasesResultedV2 hearingResulted = ProsecutionCasesResultedV2.prosecutionCasesResultedV2().withHearing(
                Hearing.hearing()
                        .withId(hearingId)
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .withProsecutionCases(Arrays.asList(
                                ProsecutionCase.prosecutionCase().withId(caseId1).withDefendants(Collections.singletonList(Defendant.defendant().build())).build(),
                                ProsecutionCase.prosecutionCase().withId(caseId2).withDefendants(Collections.singletonList(Defendant.defendant().build())).build()))
                        .withCourtApplications(Arrays.asList(CourtApplication.courtApplication().withId(applicationId).build()))
                        .build())
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.prosecution-cases-resulted-v2"),
                objectToJsonObjectConverter.convert(hearingResulted));

        this.eventProcessor.handleProsecutionCasesResultedV2(event);

        verifyNoMoreInteractions(sender);
        verify(progressionService, times(2)).updateCase(eq(event), prosecutionCaseArgumentCaptor.capture(),
                courtApplicationsArgumentCaptor.capture(), defendantJudicialResultArgumentCaptor.capture(),
                courtCentreArgumentCaptor.capture(), hearingIdCaptor.capture(), hearingTypeCaptor.capture(), jurisdictionTypeCaptor.capture(), isBoxHearingCaptor.capture());

        final List<ProsecutionCase> capturedCases = prosecutionCaseArgumentCaptor.getAllValues();
        assertTrue(capturedCases.stream().anyMatch(c -> caseId1.equals(c.getId())));
        assertTrue(capturedCases.stream().anyMatch(c -> caseId2.equals(c.getId())));

        assertThat(courtApplicationsArgumentCaptor.getValue().get(0).getId(), is(applicationId));
        assertThat(hearingIdCaptor.getValue(), CoreMatchers.is(hearingId));
        assertThat(jurisdictionTypeCaptor.getValue(), CoreMatchers.is(JurisdictionType.CROWN));
    }

    @Test
    public void shouldCreateNextHearingsInListingAndUpdateStatus() {
        final UUID seedingHearingId = randomUUID();
        final UUID hearingId = randomUUID();
        final LocalDate hearingDay = LocalDate.now();
        final UUID bookingReferenceId = randomUUID();
        final List<UUID> courtScheduleIds = Arrays.asList(randomUUID(), randomUUID());

        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing()
                .withSeedingHearingId(seedingHearingId)
                .withSittingDay(hearingDay.toString())
                .build();

        final List<BookingReferenceCourtScheduleIds> bookingReferenceCourtScheduleIds = Arrays.asList(BookingReferenceCourtScheduleIds.bookingReferenceCourtScheduleIds()
                .withBookingId(bookingReferenceId)
                .withCourtScheduleIds(courtScheduleIds)
                .build());

        final Map<UUID, Set<UUID>> alreadyExistingAndNewBookingReferencesWithCourtScheduleIds = new HashMap<>();
        alreadyExistingAndNewBookingReferencesWithCourtScheduleIds.put(bookingReferenceId, new HashSet<>(courtScheduleIds));

        final NextHearingsRequested hearingResulted = NextHearingsRequested.nextHearingsRequested()
                .withSeedingHearing(seedingHearing)
                .withPreviousBookingReferencesWithCourtScheduleIds(bookingReferenceCourtScheduleIds)
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .build())
                .build();

        final List<HearingListingNeeds> hearings = Arrays.asList(HearingListingNeeds.hearingListingNeeds()
                .withId(hearingId)
                .build());
        when(hearingToHearingListingNeedsTransformer.transformWithSeedHearing(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(hearings);
        when(hearingToHearingListingNeedsTransformer.getCombinedBookingReferencesAndCourtScheduleIds(Mockito.any(), Mockito.any())).thenReturn(alreadyExistingAndNewBookingReferencesWithCourtScheduleIds);

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.next-hearings-requested"),
                objectToJsonObjectConverter.convert(hearingResulted));

        this.eventProcessor.handleNextHearingsRequested(event);

        verify(progressionService).updateHearingListingStatusToSentForListing(eq(event), listNextHearingsArgumentCaptor.capture());
        assertThat(listNextHearingsArgumentCaptor.getValue().getHearingId(), is(seedingHearing.getSeedingHearingId()));
        assertThat(listNextHearingsArgumentCaptor.getValue().getHearings().size(), CoreMatchers.is(1));
        assertThat(listNextHearingsArgumentCaptor.getValue().getHearings().get(0).getId(), is(hearingId));

        verify(progressionService).storeBookingReferencesWithCourtScheduleIds(eq(event), storeBookingReferenceCourtScheduleIdsArgumentCaptor.capture());
        assertThat(storeBookingReferenceCourtScheduleIdsArgumentCaptor.getValue().getHearingId(), is(hearingId));
        assertThat(storeBookingReferenceCourtScheduleIdsArgumentCaptor.getValue().getHearingDay(), is(hearingDay));
        assertThat(storeBookingReferenceCourtScheduleIdsArgumentCaptor.getValue().getBookingReferenceCourtScheduleIds().size(), is(1));
        verify(progressionService).populateHearingToProbationCaseworker(eq(event), eq(hearingId));
    }

    @Test
    public void shouldNotCreateNextHearingsInListingAndUpdateStatus() {
        final UUID seedingHearingId = randomUUID();
        final UUID hearingId = randomUUID();
        final LocalDate hearingDay = LocalDate.now();
        final UUID bookingReferenceId = randomUUID();
        final List<UUID> courtScheduleIds = Arrays.asList(randomUUID(), randomUUID());

        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing()
                .withSeedingHearingId(seedingHearingId)
                .withSittingDay(hearingDay.toString())
                .build();

        final List<BookingReferenceCourtScheduleIds> bookingReferenceCourtScheduleIds = Arrays.asList(BookingReferenceCourtScheduleIds.bookingReferenceCourtScheduleIds()
                .withBookingId(bookingReferenceId)
                .withCourtScheduleIds(courtScheduleIds)
                .build());

        final Map<UUID, Set<UUID>> alreadyExistingAndNewBookingReferencesWithCourtScheduleIds = new HashMap<>();
        alreadyExistingAndNewBookingReferencesWithCourtScheduleIds.put(bookingReferenceId, new HashSet<>(courtScheduleIds));

        final NextHearingsRequested hearingResulted = NextHearingsRequested.nextHearingsRequested()
                .withSeedingHearing(seedingHearing)
                .withPreviousBookingReferencesWithCourtScheduleIds(bookingReferenceCourtScheduleIds)
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .build())
                .build();
        when(hearingToHearingListingNeedsTransformer.transformWithSeedHearing(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(null);
        when(hearingToHearingListingNeedsTransformer.getCombinedBookingReferencesAndCourtScheduleIds(Mockito.any(), Mockito.any())).thenReturn(alreadyExistingAndNewBookingReferencesWithCourtScheduleIds);

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.next-hearings-requested"),
                objectToJsonObjectConverter.convert(hearingResulted));
        this.eventProcessor.handleNextHearingsRequested(event);

        // Zero times execution of sending hearing to listing and updating status
        verify(progressionService, times(0)).updateHearingListingStatusToSentForListing(eq(event), listNextHearingsArgumentCaptor.capture());

        verify(progressionService).storeBookingReferencesWithCourtScheduleIds(eq(event), storeBookingReferenceCourtScheduleIdsArgumentCaptor.capture());
        assertThat(storeBookingReferenceCourtScheduleIdsArgumentCaptor.getValue().getHearingId(), is(hearingId));
        assertThat(storeBookingReferenceCourtScheduleIdsArgumentCaptor.getValue().getHearingDay(), is(hearingDay));
        assertThat(storeBookingReferenceCourtScheduleIdsArgumentCaptor.getValue().getBookingReferenceCourtScheduleIds().size(), is(1));
        verify(progressionService).populateHearingToProbationCaseworker(eq(event), eq(hearingId));
    }

    @Test
    public void shouldCreateNextUnscheduledHearingsInListingAndUpdateStatus() {
        final UUID seedingHearingId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing()
                .withSeedingHearingId(seedingHearingId)
                .build();

        final UnscheduledNextHearingsRequested unscheduledNextHearingsRequested = mockConverterAndTransformerResponses(hearingId, caseId, seedingHearing);

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.unscheduled-next-hearings-requested"),
                objectToJsonObjectConverter.convert(unscheduledNextHearingsRequested));

        this.eventProcessor.handUnscheduledNextHearingsRequested(event);

        verify(listingService).listUnscheduledNextHearings(eq(event), listUnscheduledNextHearingsArgumentCaptor.capture());
        assertThat(listUnscheduledNextHearingsArgumentCaptor.getValue().getHearingId(), is(seedingHearingId));
        assertThat(listUnscheduledNextHearingsArgumentCaptor.getValue().getHearings().get(0).getId(), is(hearingId));

        verify(progressionService).sendUpdateDefendantListingStatusForUnscheduledListing(eq(event), hearingArgumentCaptor.capture(), hearingUnscheduledListingNeedsCaptor.capture());
        assertThat(hearingArgumentCaptor.getValue().get(0).getId(), is(hearingId));
        final Set<UUID> unscheduledHearingIds = new HashSet<>();
        unscheduledHearingIds.add(hearingId);
        assertThat(hearingUnscheduledListingNeedsCaptor.getValue(), is(unscheduledHearingIds));

        verify(progressionService).recordUnlistedHearing(eq(event), eq(hearingId), hearingArgumentCaptor.capture());
        assertThat(hearingArgumentCaptor.getValue().get(0).getId(), is(hearingId));
    }

    @Test
    public void shouldCreateNextUnscheduledHearingsInListingAndUpdateStatusForApplicationWithCase() {
        final UUID seedingHearingId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID applicationId = randomUUID();
        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing()
                .withSeedingHearingId(seedingHearingId)
                .build();

        final UnscheduledNextHearingsRequested unscheduledNextHearingsRequested = mockConverterAndTransformerResponsesWithApplication(hearingId, caseId, applicationId, seedingHearing);

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.unscheduled-next-hearings-requested"),
                objectToJsonObjectConverter.convert(unscheduledNextHearingsRequested));

        this.eventProcessor.handUnscheduledNextHearingsRequested(event);

        verify(listingService).listUnscheduledNextHearings(eq(event), listUnscheduledNextHearingsArgumentCaptor.capture());
        assertThat(listUnscheduledNextHearingsArgumentCaptor.getValue().getHearingId(), is(seedingHearingId));
        assertThat(listUnscheduledNextHearingsArgumentCaptor.getValue().getHearings().get(0).getId(), is(hearingId));

        verify(progressionService).sendUpdateDefendantListingStatusForUnscheduledListing(eq(event), hearingArgumentCaptor.capture(), hearingUnscheduledListingNeedsCaptor.capture());
        assertThat(hearingArgumentCaptor.getValue().get(0).getId(), is(hearingId));
        assertThat(hearingArgumentCaptor.getValue().get(0).getCourtApplications().get(0).getId(), is(applicationId));
        assertThat(hearingArgumentCaptor.getValue().get(0).getProsecutionCases().get(0).getId(), is(caseId));
        final Set<UUID> unscheduledHearingIds = new HashSet<>();
        unscheduledHearingIds.add(hearingId);
        assertThat(hearingUnscheduledListingNeedsCaptor.getValue(), is(unscheduledHearingIds));

        verify(progressionService).recordUnlistedHearing(eq(event), eq(hearingId), hearingArgumentCaptor.capture());
        assertThat(hearingArgumentCaptor.getValue().get(0).getId(), is(hearingId));
    }


    @Test
    public void shouldProcessCreateHearingForApplication() {
        final UUID hearingId = randomUUID();
        final UUID courtApplicationId = randomUUID();
        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withCourtApplications(Arrays.asList(CourtApplication.courtApplication()
                        .withId(courtApplicationId)
                        .build()))
                .build();

        final HearingForApplicationCreated hearingForApplicationCreated = HearingForApplicationCreated.hearingForApplicationCreated()
                .withHearing(hearing)
                .withHearingListingStatus(HearingListingStatus.SENT_FOR_LISTING)
                .build();
        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.hearing-for-application-created"),
                objectToJsonObjectConverter.convert(hearingForApplicationCreated));

        this.eventProcessor.processCreateHearingForApplication(event);

        verify(progressionService).linkApplicationToHearing(eq(event), hearingCaptor.capture(), eq(HearingListingStatus.SENT_FOR_LISTING));
        assertThat(hearingCaptor.getValue().getId(), is(hearingId));
        assertThat(hearingCaptor.getValue().getCourtApplications().get(0).getId(), is(courtApplicationId));

    }

    @Test
    public void shouldProcessCreateHearingForApplicationV2_whenListHearingRequestsIsNotEmpty() {
        final UUID hearingId = randomUUID();
        final UUID courtApplicationId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final String offenceCode = RandomStringUtils.randomAlphanumeric(8);

        final ProsecutionCase prosecutionCase = getProsecutionCase(caseId, defendantId, offenceId, offenceCode, false);

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withProsecutionCases(singletonList(prosecutionCase))
                .withCourtApplications(Arrays.asList(CourtApplication.courtApplication()
                        .withId(courtApplicationId)
                        .build()))
                .build();
        final ListHearingRequest listHearingRequest = populateListHearingRequest(caseId, defendantId, offenceId);

        final HearingForApplicationCreatedV2 hearingForApplicationCreated = HearingForApplicationCreatedV2.hearingForApplicationCreatedV2()
                .withHearing(hearing)
                .withHearingListingStatus(HearingListingStatus.SENT_FOR_LISTING)
                .withListHearingRequests(singletonList(listHearingRequest))
                .build();
        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.hearing-for-application-created-v2"),
                objectToJsonObjectConverter.convert(hearingForApplicationCreated));

        this.eventProcessor.processCreateHearingForApplicationV2(event);

        verify(progressionService).linkApplicationToHearing(eq(event), hearingCaptor.capture(), eq(HearingListingStatus.SENT_FOR_LISTING));
        assertThat(hearingCaptor.getValue().getId(), is(hearingId));
        assertThat(hearingCaptor.getValue().getCourtApplications().get(0).getId(), is(courtApplicationId));

        verify(listingService).listCourtHearing(eq(event), listCourtHearingCaptor.capture());
        assertThat(listCourtHearingCaptor.getValue().getHearings().get(0).getId(), is(hearingId));
        assertThat(listCourtHearingCaptor.getValue().getHearings().get(0).getProsecutionCases().get(0).getId(), is(caseId));
    }

    @Test
    public void shouldProcessCreateHearingForApplicationV2_whenListHearingRequestsIsEmpty() {
        final UUID hearingId = randomUUID();
        final UUID courtApplicationId = randomUUID();
        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withCourtApplications(Arrays.asList(CourtApplication.courtApplication()
                        .withId(courtApplicationId)
                        .build()))
                .build();

        final HearingForApplicationCreatedV2 hearingForApplicationCreated = HearingForApplicationCreatedV2.hearingForApplicationCreatedV2()
                .withHearing(hearing)
                .withHearingListingStatus(HearingListingStatus.SENT_FOR_LISTING)
                .build();
        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.hearing-for-application-created-v2"),
                objectToJsonObjectConverter.convert(hearingForApplicationCreated));

        this.eventProcessor.processCreateHearingForApplicationV2(event);

        verify(progressionService).linkApplicationToHearing(eq(event), hearingCaptor.capture(), eq(HearingListingStatus.SENT_FOR_LISTING));
        assertThat(hearingCaptor.getValue().getId(), is(hearingId));
        assertThat(hearingCaptor.getValue().getCourtApplications().get(0).getId(), is(courtApplicationId));
        verify(listingService, never()).listCourtHearing(any(JsonEnvelope.class), any(ListCourtHearing.class));
    }

    @Test
    public void shouldProcessInitiateApplicationForCaseRequested() {
        final UUID hearingId = randomUUID();
        final UUID courtApplicationId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID oldApplicationId = randomUUID();
        final LocalDate issueDate = LocalDate.now();
        final String offenceCode = RandomStringUtils.randomAlphanumeric(8);

        final ProsecutionCase prosecutionCase = getProsecutionCase(caseId, defendantId, offenceId, offenceCode, false);
        final Defendant defendant = prosecutionCase.getDefendants().get(0);
        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withProsecutionCases(singletonList(prosecutionCase))
                .withCourtApplications(Arrays.asList(CourtApplication.courtApplication()
                        .withId(courtApplicationId)
                        .build()))
                .build();
        final NextHearing nextHearing = NextHearing.nextHearing().withIsFirstReviewHearing(true).build();

        final InitiateApplicationForCaseRequested initiateApplicationForCaseRequested = InitiateApplicationForCaseRequested.initiateApplicationForCaseRequested()
                .withHearing(hearing)
                .withProsecutionCase(prosecutionCase)
                .withApplicationId(courtApplicationId)
                .withNextHearing(nextHearing)
                .withDefendant(defendant)
                .withIsAmended(true)
                .withOldApplicationId(oldApplicationId)
                .withIssueDate(issueDate)
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.initiate-application-for-case-requested"),
                objectToJsonObjectConverter.convert(initiateApplicationForCaseRequested));

        this.eventProcessor.processInitiateApplicationForCaseRequested(event);

        verify(progressionService).initiateNewCourtApplication(eq(event), eq(initiateApplicationForCaseRequested));

    }

    private UnscheduledNextHearingsRequested mockConverterAndTransformerResponses(final UUID hearingId, final UUID caseId, final SeedingHearing seedingHearing) {
        final ArrayList<HearingDay> hearingDays = new ArrayList<>();
        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withId(caseId)
                .build();
        final UnscheduledNextHearingsRequested unscheduledNextHearingsRequested = UnscheduledNextHearingsRequested.unscheduledNextHearingsRequested()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withHearingDays(hearingDays)
                        .build())
                .withSeedingHearing(seedingHearing)
                .build();

        final List<HearingUnscheduledListingNeeds> hearingUnscheduledListingNeeds = Arrays.asList(HearingUnscheduledListingNeeds.hearingUnscheduledListingNeeds()
                .withId(hearingId)
                .withProsecutionCases(Arrays.asList(prosecutionCase))
                .build());

        when(unscheduledCourtHearingListTransformer.transformWithSeedHearing(Mockito.any(), eq(seedingHearing))).thenReturn(hearingUnscheduledListingNeeds);
        final Hearing hearing = Hearing.hearing().withId(hearingId).build();
        when(hearingResultUnscheduledListingHelper.convertToHearing(hearingUnscheduledListingNeeds.get(0), hearingDays)).thenReturn(hearing);

        final Set<UUID> hearingIds = new HashSet<UUID>();
        hearingIds.add(hearingId);
        when(hearingResultUnscheduledListingHelper.getHearingIsToBeSentNotification(hearingUnscheduledListingNeeds)).thenReturn(hearingIds);
        return unscheduledNextHearingsRequested;
    }


    private UnscheduledNextHearingsRequested mockConverterAndTransformerResponsesWithApplication(final UUID hearingId, final UUID caseId, final UUID applicationId, final SeedingHearing seedingHearing) {
        final ArrayList<HearingDay> hearingDays = new ArrayList<>();
        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withId(caseId)
                .build();
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withCourtApplicationCases(Arrays.asList(CourtApplicationCase.courtApplicationCase()
                        .withProsecutionCaseId(caseId)
                        .build()))
                .build();
        final UnscheduledNextHearingsRequested unscheduledNextHearingsRequested = UnscheduledNextHearingsRequested.unscheduledNextHearingsRequested()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withHearingDays(hearingDays)
                        .build())
                .withSeedingHearing(seedingHearing)
                .build();

        final List<HearingUnscheduledListingNeeds> hearingUnscheduledListingNeeds = Arrays.asList(HearingUnscheduledListingNeeds.hearingUnscheduledListingNeeds()
                .withId(hearingId)
                .withProsecutionCases(Arrays.asList(prosecutionCase))
                .withCourtApplications(Arrays.asList(courtApplication))
                .build());

        when(unscheduledCourtHearingListTransformer.transformWithSeedHearing(Mockito.any(), eq(seedingHearing))).thenReturn(hearingUnscheduledListingNeeds);
        final Hearing hearing = Hearing.hearing().withId(hearingId)
                .withProsecutionCases(Arrays.asList(prosecutionCase()
                        .withId(caseId)
                        .build()
                ))
                .withCourtApplications(Arrays.asList(CourtApplication.courtApplication()
                        .withId(applicationId)
                        .build()
                ))
                .build();
        when(hearingResultUnscheduledListingHelper.convertToHearing(hearingUnscheduledListingNeeds.get(0), hearingDays)).thenReturn(hearing);

        final Set<UUID> hearingIds = new HashSet<UUID>();
        hearingIds.add(hearingId);
        when(hearingResultUnscheduledListingHelper.getHearingIsToBeSentNotification(hearingUnscheduledListingNeeds)).thenReturn(hearingIds);
        return unscheduledNextHearingsRequested;
    }

    private ListHearingRequest populateListHearingRequest(final UUID caseId, final UUID defendantId, final UUID offenceId) {
        return ListHearingRequest.listHearingRequest()
                .withListDefendantRequests(Arrays.asList(ListDefendantRequest.listDefendantRequest()
                        .withProsecutionCaseId(caseId)
                        .withDefendantId(defendantId)
                        .withDefendantOffences(Arrays.asList(offenceId))
                        .build()))
                .withListedStartDateTime(new ZonedDateTimeGenerator(Period.ofDays(20), ZonedDateTime.now(), DateGenerator.Direction.FUTURE).next())
                .build();
    }

    private ProsecutionCase getProsecutionCase(final UUID caseId, final UUID defendantId, final UUID offenceId, final String offenceCode, final boolean isYouth) {
        return getProsecutionCase(caseId, defendantId, offenceId, offenceCode, isYouth, ProsecutionCaseIdentifier.prosecutionCaseIdentifier().build());
    }

    private ProsecutionCase getProsecutionCase(final UUID caseId, final UUID defendantId, final UUID offenceId, final String offenceCode, final boolean isYouth, final ProsecutionCaseIdentifier prosecutionCaseIdentifier) {
        final ProsecutionCase.Builder builder = new ProsecutionCase.Builder().withId(caseId)
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier);

        final Defendant.Builder defendantBuilder = new Defendant.Builder()
                .withId(defendantId)
                .withOffences(Stream.of(Offence.offence()
                                .withId(offenceId)
                                .withOffenceCode(offenceCode)
                                .build())
                        .collect(Collectors.toList()));
        if (isYouth) {
            defendantBuilder.withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(Person.person().withDateOfBirth(LocalDate.of(2005, 11, 11)).build()).build());
        }
        builder.withDefendants(Arrays.asList(defendantBuilder.build()));

        return builder.build();
    }

}
