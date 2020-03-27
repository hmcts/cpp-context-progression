package uk.gov.moj.cpp.progression.event;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.AttendanceDay;
import uk.gov.justice.core.courts.AttendanceType;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationOutcome;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantAttendance;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.hearing.courts.HearingResulted;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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


    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<List<UUID>> applicationIdsArgumentCaptor;

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


    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void handleHearingResultWithoutApplicationOutcome() {

        final UUID courtApplicationId = UUID.randomUUID();
        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearing(Hearing.hearing()
                        .withId(UUID.randomUUID())
                        .withCourtApplications(Arrays.asList(CourtApplication.courtApplication()
                                .withId(courtApplicationId)
                                .build()))
                        .withDefendantAttendance(
                                Arrays.asList(DefendantAttendance.defendantAttendance()
                                        .withDefendantId(UUID.randomUUID())
                                        .withAttendanceDays(Arrays.asList(
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

        this.eventProcessor.handleHearingResultedPublicEvent(event);

        verify(this.sender , times(1)).send(this.envelopeArgumentCaptor.capture());
        verify(progressionService, atLeastOnce()).linkApplicationsToHearing(envelopeArgumentCaptor.capture(), hearingArgumentCaptor.capture(), applicationIdsArgumentCaptor.capture(), hearingListingStatusArgumentCaptor.capture());
        verify(progressionService, atLeastOnce()).updateCourtApplicationStatus(envelopeArgumentCaptor.capture(), applicationIdsArgumentCaptor.capture(), applicationStatusArgumentCaptor.capture());

        assertThat(hearingArgumentCaptor.getValue(), notNullValue());
        assertThat(hearingArgumentCaptor.getValue().getId(), equalTo(hearingResulted.getHearing().getId()));
        assertThat(applicationIdsArgumentCaptor.getValue(), notNullValue());
        assertEquals(0, applicationIdsArgumentCaptor.getValue().size());
        assertThat(hearingListingStatusArgumentCaptor.getValue(), equalTo(HearingListingStatus.HEARING_RESULTED));
        assertNull(hearingResulted.getHearing().getCourtApplications().get(0).getApplicationOutcome());
    }

    @Test
    public void handleHearingResultWithNoApplications() throws EventStreamException {

        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearing(Hearing.hearing()
                        .withId(UUID.randomUUID())
                        .withDefendantAttendance(
                                Arrays.asList(DefendantAttendance.defendantAttendance()
                                        .withDefendantId(UUID.randomUUID())
                                        .withAttendanceDays(Arrays.asList(
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

        this.eventProcessor.handleHearingResultedPublicEvent(event);

        verify(this.sender , times(1)).send(this.envelopeArgumentCaptor.capture());
        verify(progressionService, never()).linkApplicationsToHearing(envelopeArgumentCaptor.capture(), hearingArgumentCaptor.capture(), applicationIdsArgumentCaptor.capture(), hearingListingStatusArgumentCaptor.capture());

        final List<JsonEnvelope> allValues = envelopeArgumentCaptor.getAllValues();
        assertThat(allValues.size(), is(1));
        assertThat(allValues.get(0).metadata().name(), equalTo("progression.command.hearing-result"));

    }



    @Test
    public void handleHearingResultWithApplicationOutCome() {

        final UUID courtApplicationId = UUID.randomUUID();

        final CourtApplicationOutcome courtApplicationOutcome = CourtApplicationOutcome.courtApplicationOutcome()
                .withApplicationId(courtApplicationId)
                .build();

        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearing(Hearing.hearing()
                        .withId(UUID.randomUUID())
                        .withDefendantAttendance(
                                Arrays.asList(DefendantAttendance.defendantAttendance()
                                        .withDefendantId(UUID.randomUUID())
                                        .withAttendanceDays(Arrays.asList(
                                                AttendanceDay.attendanceDay().withAttendanceType(AttendanceType.BY_VIDEO).withDay(LocalDate.now()).build(),
                                                AttendanceDay.attendanceDay().withAttendanceType(AttendanceType.IN_PERSON).withDay(LocalDate.now().plusDays(7)).build()
                                        ))
                                        .build())
                        )
                        .withCourtApplications(Arrays.asList(CourtApplication.courtApplication()
                                .withApplicationOutcome(courtApplicationOutcome)
                                .withId(courtApplicationId)
                                .build()))
                        .build())
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("public.hearing.resulted"),
                objectToJsonObjectConverter.convert(hearingResulted));

        this.eventProcessor.handleHearingResultedPublicEvent(event);

        verify(this.sender , times(1)).send(this.envelopeArgumentCaptor.capture());
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
    public void handleResultWhenProsecutionCaseIsPresentOnHearing() throws Exception {

        final UUID courtApplicationId = UUID.randomUUID();

        final CourtApplicationOutcome courtApplicationOutcome = CourtApplicationOutcome.courtApplicationOutcome()
                .withApplicationId(courtApplicationId)
                .build();

        final UUID commonUUID = UUID.randomUUID();
        final Defendant defendant1 = Defendant.defendant().withId(commonUUID).build();
        final Defendant defendant2 = Defendant.defendant().withId(commonUUID).build();
        final Defendant defendant3 = Defendant.defendant().withId(UUID.randomUUID()).build();

        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(defendant1);
        defendants.add(defendant2);
        defendants.add(defendant3);

        final List<ProsecutionCase> prosecutionCases = new ArrayList<>();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().withDefendants(defendants).build();
        prosecutionCases.add(prosecutionCase);


        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearing(Hearing.hearing()
                        .withId(UUID.randomUUID())
                        .withDefendantAttendance(
                                Arrays.asList(DefendantAttendance.defendantAttendance()
                                        .withDefendantId(UUID.randomUUID())
                                        .withAttendanceDays(Arrays.asList(
                                                AttendanceDay.attendanceDay().withAttendanceType(AttendanceType.BY_VIDEO).withDay(LocalDate.now()).build(),
                                                AttendanceDay.attendanceDay().withAttendanceType(AttendanceType.IN_PERSON).withDay(LocalDate.now().plusDays(7)).build()
                                        ))
                                        .build())
                        )
                        .withProsecutionCases(prosecutionCases)
                        .withCourtApplications(Arrays.asList(CourtApplication.courtApplication()
                                .withApplicationOutcome(courtApplicationOutcome)
                                .withId(courtApplicationId)
                                .build()))
                        .build())
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("public.hearing.resulted"),
                objectToJsonObjectConverter.convert(hearingResulted));

        this.eventProcessor.handleHearingResultedPublicEvent(event);

        verify(this.sender , times(1)).send(this.envelopeArgumentCaptor.capture());
        verify(progressionService, atLeastOnce()).updateCase(envelopeArgumentCaptor.capture(), prosecutionCaseArgumentCaptor.capture());
        assertThat(prosecutionCaseArgumentCaptor.getValue().getDefendants().size(), is(3));
        assertThat(prosecutionCaseArgumentCaptor.getValue().getDefendants().get(0).getId(), is(commonUUID));


    }

    @Test
    public void handleResultWithEmptyProsecutionCaseOnHearing() throws Exception {

        final UUID courtApplicationId = UUID.randomUUID();

        final CourtApplicationOutcome courtApplicationOutcome = CourtApplicationOutcome.courtApplicationOutcome()
                .withApplicationId(courtApplicationId)
                .build();

        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearing(Hearing.hearing()
                        .withId(UUID.randomUUID())
                        .withDefendantAttendance(
                                Arrays.asList(DefendantAttendance.defendantAttendance()
                                        .withDefendantId(UUID.randomUUID())
                                        .withAttendanceDays(Arrays.asList(
                                                AttendanceDay.attendanceDay().withAttendanceType(AttendanceType.BY_VIDEO).withDay(LocalDate.now()).build(),
                                                AttendanceDay.attendanceDay().withAttendanceType(AttendanceType.IN_PERSON).withDay(LocalDate.now().plusDays(7)).build()
                                        ))
                                        .build())
                        )
                        .withCourtApplications(Arrays.asList(CourtApplication.courtApplication()
                                .withApplicationOutcome(courtApplicationOutcome)
                                .withId(courtApplicationId)
                                .build()))
                        .build())
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("public.hearing.resulted"),
                objectToJsonObjectConverter.convert(hearingResulted));

        this.eventProcessor.handleHearingResultedPublicEvent(event);
        verify(this.sender , times(1)).send(this.envelopeArgumentCaptor.capture());
        verify(progressionService, never()).updateCase(envelopeArgumentCaptor.capture(), prosecutionCaseArgumentCaptor.capture());
    }
}