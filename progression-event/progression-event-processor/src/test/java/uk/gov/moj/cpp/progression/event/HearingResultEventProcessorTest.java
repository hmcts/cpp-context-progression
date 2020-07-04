package uk.gov.moj.cpp.progression.event;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.AttendanceDay;
import uk.gov.justice.core.courts.AttendanceType;
import uk.gov.justice.core.courts.Category;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationOutcome;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantAttendance;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.hearing.courts.HearingResulted;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.moj.cpp.progression.helper.HearingResultHelper;
import uk.gov.moj.cpp.progression.helper.HearingResultUnscheduledListingHelper;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.NextHearingService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.dto.NextHearingDetails;
import uk.gov.moj.cpp.progression.transformer.HearingToHearingListingNeedsTransformer;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingResultEventProcessorTest {

    @InjectMocks
    private HearingResultEventProcessor eventProcessor;

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<List<UUID>> applicationIdsArgumentCaptor;

    @Captor
    private ArgumentCaptor<List<CourtApplication>> courtApplicationsArgumentCaptor;

    @Captor
    private ArgumentCaptor<Hearing> hearingArgumentCaptor;

    @Captor
    private ArgumentCaptor<ApplicationStatus> applicationStatusArgumentCaptor;

    @Captor
    private ArgumentCaptor<HearingListingStatus> hearingListingStatusArgumentCaptor;

    @Captor
    private ArgumentCaptor<ProsecutionCase> prosecutionCaseArgumentCaptor;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    @InjectMocks
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();

    @Spy
    @InjectMocks
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter();

    @Mock
    private ProgressionService progressionService;

    @Mock
    private ListingService listingService;

    @Mock
    private NextHearingService nextHearingService;

    @Mock
    private HearingToHearingListingNeedsTransformer hearingToHearingListingNeedsTransformer;

    @Mock
    private HearingResultUnscheduledListingHelper hearingResultUnscheduledListingHelper;

    @Mock
    private HearingResultHelper hearingResultHelper;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void handleHearingResultWithoutApplicationOutcome() {

        final UUID courtApplicationId = randomUUID();
        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearing(Hearing.hearing()
                        .withId(randomUUID())
                        .withCourtApplications(singletonList(CourtApplication.courtApplication()
                                .withId(courtApplicationId)
                                .build()))
                        .withDefendantAttendance(
                                singletonList(DefendantAttendance.defendantAttendance()
                                        .withDefendantId(randomUUID())
                                        .withAttendanceDays(asList(
                                                AttendanceDay.attendanceDay().withAttendanceType(AttendanceType.BY_VIDEO).withDay(LocalDate.now()).build(),
                                                AttendanceDay.attendanceDay().withAttendanceType(AttendanceType.IN_PERSON).withDay(LocalDate.now().plusDays(7)).build()
                                        ))
                                        .build())
                        )
                        .build())
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("public.hearing.resulted"),
                objectToJsonObjectConverter.convert(hearingResulted));

        final Optional<JsonObject> hearingJsonOptional = getHearingJson();
        when(progressionService.getHearing(any(), any())).thenReturn(hearingJsonOptional);

        this.eventProcessor.handleHearingResultedPublicEvent(event);

        verify(this.sender, times(1)).send(this.envelopeArgumentCaptor.capture());
        verify(progressionService, atLeastOnce()).linkApplicationsToHearing(envelopeArgumentCaptor.capture(), hearingArgumentCaptor.capture(), applicationIdsArgumentCaptor.capture(), hearingListingStatusArgumentCaptor.capture());
        verify(progressionService, atLeastOnce()).updateCourtApplicationStatus(envelopeArgumentCaptor.capture(), applicationIdsArgumentCaptor.capture(), applicationStatusArgumentCaptor.capture());

        assertThat(hearingArgumentCaptor.getValue(), notNullValue());
        assertThat(hearingArgumentCaptor.getValue().getId(), equalTo(hearingResulted.getHearing().getId()));
        assertThat(applicationIdsArgumentCaptor.getValue(), notNullValue());
        assertThat(hearingListingStatusArgumentCaptor.getValue(), equalTo(HearingListingStatus.HEARING_RESULTED));
        assertNull(hearingResulted.getHearing().getCourtApplications().get(0).getApplicationOutcome());
    }

    @Test
    public void handleHearingResultWithNoApplications() {

        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearing(Hearing.hearing()
                        .withId(randomUUID())
                        .withDefendantAttendance(
                                asList(DefendantAttendance.defendantAttendance()
                                        .withDefendantId(randomUUID())
                                        .withAttendanceDays(asList(
                                                AttendanceDay.attendanceDay().withAttendanceType(AttendanceType.BY_VIDEO).withDay(LocalDate.now()).build(),
                                                AttendanceDay.attendanceDay().withAttendanceType(AttendanceType.IN_PERSON).withDay(LocalDate.now().plusDays(7)).build()
                                        ))
                                        .build())
                        )
                        .build())
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("public.hearing.resulted"),
                objectToJsonObjectConverter.convert(hearingResulted));

        final Optional<JsonObject> hearingJsonOptional = getHearingJson();
        when(progressionService.getHearing(any(), any())).thenReturn(hearingJsonOptional);
        when(hearingResultHelper.doProsecutionCasesContainNextHearingResults(anyList())).thenReturn(true);
        when(hearingResultHelper.doCourtApplicationsContainNextHearingResults(anyList())).thenReturn(true);

        this.eventProcessor.handleHearingResultedPublicEvent(event);

        verify(this.sender, times(1)).send(this.envelopeArgumentCaptor.capture());
        verify(progressionService, never()).linkApplicationsToHearing((JsonEnvelope) envelopeArgumentCaptor.capture(), hearingArgumentCaptor.capture(), applicationIdsArgumentCaptor.capture(), hearingListingStatusArgumentCaptor.capture());

        final List allValues = envelopeArgumentCaptor.getAllValues();
        assertThat(allValues.size(), is(1));
        assertThat(((DefaultEnvelope)allValues.get(0)).metadata().name(), equalTo("progression.command.hearing-result"));

    }


    @Test
    public void handleHearingResultWithApplicationOutCome() {

        final UUID courtApplicationId = randomUUID();

        final CourtApplicationOutcome courtApplicationOutcome = CourtApplicationOutcome.courtApplicationOutcome()
                .withApplicationId(courtApplicationId)
                .build();

        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearing(Hearing.hearing()
                        .withId(randomUUID())
                        .withDefendantAttendance(
                                asList(DefendantAttendance.defendantAttendance()
                                        .withDefendantId(randomUUID())
                                        .withAttendanceDays(asList(
                                                AttendanceDay.attendanceDay().withAttendanceType(AttendanceType.BY_VIDEO).withDay(LocalDate.now()).build(),
                                                AttendanceDay.attendanceDay().withAttendanceType(AttendanceType.IN_PERSON).withDay(LocalDate.now().plusDays(7)).build()
                                        ))
                                        .build())
                        )
                        .withCourtApplications(asList(CourtApplication.courtApplication()
                                .withApplicationOutcome(courtApplicationOutcome)
                                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                                        .withCategory(Category.FINAL).build(), JudicialResult.judicialResult()
                                        .withCategory(Category.ANCILLARY).build()))
                                .withId(courtApplicationId)
                                .build()))
                        .build())
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("public.hearing.resulted"),
                objectToJsonObjectConverter.convert(hearingResulted));

        final Optional<JsonObject> hearingJsonOptional = getHearingJson();
        when(progressionService.getHearing(any(), any())).thenReturn(hearingJsonOptional);

        this.eventProcessor.handleHearingResultedPublicEvent(event);

        verify(this.sender, times(1)).send(this.envelopeArgumentCaptor.capture());
        verify(progressionService, atLeastOnce()).linkApplicationsToHearing(envelopeArgumentCaptor.capture(), hearingArgumentCaptor.capture(), applicationIdsArgumentCaptor.capture(), hearingListingStatusArgumentCaptor.capture());
        verify(progressionService, atLeastOnce()).updateCourtApplicationStatus(envelopeArgumentCaptor.capture(), applicationIdsArgumentCaptor.capture(), applicationStatusArgumentCaptor.capture());

        assertThat(hearingArgumentCaptor.getValue(), notNullValue());
        assertThat(hearingArgumentCaptor.getValue().getId(), equalTo(hearingResulted.getHearing().getId()));
        assertThat(applicationIdsArgumentCaptor.getValue(), notNullValue());
        assertThat(applicationIdsArgumentCaptor.getValue().get(0), equalTo(courtApplicationId));
        assertThat(hearingListingStatusArgumentCaptor.getValue(), equalTo(HearingListingStatus.HEARING_RESULTED));
        assertThat(applicationStatusArgumentCaptor.getValue(), equalTo(ApplicationStatus.FINALISED));
    }

    @Test
    public void handleResultWhenProsecutionCaseIsPresentOnHearing() {

        final UUID courtApplicationId = randomUUID();

        final CourtApplicationOutcome courtApplicationOutcome = CourtApplicationOutcome.courtApplicationOutcome()
                .withApplicationId(courtApplicationId)
                .build();

        final UUID commonUUID = randomUUID();
        final Defendant defendant1 = Defendant.defendant()
                .withId(commonUUID)
                .withOffences(asList(Offence.offence()
                        .build()))
                .build();
        final Defendant defendant2 = Defendant.defendant()
                .withId(commonUUID)
                .withOffences(asList(Offence.offence()
                        .build()))
                .build();
        final Defendant defendant3 = Defendant.defendant()
                .withId(randomUUID())
                .withOffences(asList(Offence.offence()
                        .build()))
                .build();

        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(defendant1);
        defendants.add(defendant2);
        defendants.add(defendant3);

        final List<ProsecutionCase> prosecutionCases = new ArrayList<>();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().withDefendants(defendants).build();
        prosecutionCases.add(prosecutionCase);


        final List<CourtApplication> courtApplications = singletonList(CourtApplication.courtApplication()
                .withApplicationOutcome(courtApplicationOutcome)
                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                        .withCategory(Category.FINAL).build(), JudicialResult.judicialResult()
                        .withCategory(Category.ANCILLARY).build()))
                .withId(courtApplicationId)
                .build());
        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearing(Hearing.hearing()
                        .withId(randomUUID())
                        .withDefendantAttendance(
                                asList(DefendantAttendance.defendantAttendance()
                                        .withDefendantId(randomUUID())
                                        .withAttendanceDays(asList(
                                                AttendanceDay.attendanceDay().withAttendanceType(AttendanceType.BY_VIDEO).withDay(LocalDate.now()).build(),
                                                AttendanceDay.attendanceDay().withAttendanceType(AttendanceType.IN_PERSON).withDay(LocalDate.now().plusDays(7)).build()
                                        ))
                                        .build())
                        )
                        .withProsecutionCases(prosecutionCases)
                        .withCourtApplications(courtApplications)
                        .build())
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("public.hearing.resulted"),
                objectToJsonObjectConverter.convert(hearingResulted));

        List<HearingListingNeeds> hearingListingNeedsList = asList(HearingListingNeeds.hearingListingNeeds()
                .withId(randomUUID())
                .build());
        final NextHearingDetails nextHearingDetails = new NextHearingDetails(null, hearingListingNeedsList, null);
        List<HearingListingNeeds> hearingListingNeedsForNextHearings = new ArrayList<>();
        final Optional<JsonObject> hearingJsonOptional = getHearingJson();
        when(progressionService.getHearing(any(), any())).thenReturn(hearingJsonOptional);
        when(nextHearingService.getNextHearingDetails(any())).thenReturn(nextHearingDetails);
        when(hearingToHearingListingNeedsTransformer.transform(any())).thenReturn(hearingListingNeedsForNextHearings);
        this.eventProcessor.handleHearingResultedPublicEvent(event);

        verify(this.sender, times(1)).send(this.envelopeArgumentCaptor.capture());
        verify(progressionService, atLeastOnce()).updateCase(envelopeArgumentCaptor.capture(), prosecutionCaseArgumentCaptor.capture(), courtApplicationsArgumentCaptor.capture());
        assertThat(prosecutionCaseArgumentCaptor.getValue().getDefendants().size(), is(3));
        assertThat(prosecutionCaseArgumentCaptor.getValue().getDefendants().get(0).getId(), is(commonUUID));
    }

    @Test
    public void handleResultWithEmptyProsecutionCaseOnHearing() throws Exception {

        final UUID courtApplicationId = randomUUID();

        final CourtApplicationOutcome courtApplicationOutcome = CourtApplicationOutcome.courtApplicationOutcome()
                .withApplicationId(courtApplicationId)
                .build();

        final List<CourtApplication> courtApplications = singletonList(CourtApplication.courtApplication()
                .withApplicationOutcome(courtApplicationOutcome)
                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                        .withCategory(Category.FINAL).build(), JudicialResult.judicialResult()
                        .withCategory(Category.ANCILLARY).build()))
                .withId(courtApplicationId)
                .build());
        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearing(Hearing.hearing()
                        .withId(randomUUID())
                        .withDefendantAttendance(
                                asList(DefendantAttendance.defendantAttendance()
                                        .withDefendantId(randomUUID())
                                        .withAttendanceDays(asList(
                                                AttendanceDay.attendanceDay().withAttendanceType(AttendanceType.BY_VIDEO).withDay(LocalDate.now()).build(),
                                                AttendanceDay.attendanceDay().withAttendanceType(AttendanceType.IN_PERSON).withDay(LocalDate.now().plusDays(7)).build()
                                        ))
                                        .build())
                        )
                        .withCourtApplications(courtApplications)
                        .build())
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("public.hearing.resulted"),
                objectToJsonObjectConverter.convert(hearingResulted));

        final Optional<JsonObject> hearingJsonOptional = getHearingJson();
        when(progressionService.getHearing(any(), any())).thenReturn(hearingJsonOptional);

        this.eventProcessor.handleHearingResultedPublicEvent(event);
        verify(this.sender, times(1)).send(this.envelopeArgumentCaptor.capture());
        verify(progressionService, never()).updateCase(envelopeArgumentCaptor.capture(), prosecutionCaseArgumentCaptor.capture(), courtApplicationsArgumentCaptor.capture());
    }

    private Optional<JsonObject> getHearingJson() {
        Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .withHasSharedResults(false)
                .build();
        final JsonObject hearingJson = Json.createObjectBuilder()
                .add("hearing", objectToJsonObjectConverter.convert(hearing))
                .build();
        return Optional.of(hearingJson);
    }
}