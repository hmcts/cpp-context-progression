package uk.gov.moj.cpp.progression.event;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.AttendanceDay.attendanceDay;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.CourtApplicationType.courtApplicationType;
import static uk.gov.justice.core.courts.Defendant.defendant;
import static uk.gov.justice.core.courts.DefendantAttendance.defendantAttendance;
import static uk.gov.justice.core.courts.Hearing.hearing;
import static uk.gov.justice.core.courts.HearingType.hearingType;
import static uk.gov.justice.core.courts.JudicialResult.judicialResult;
import static uk.gov.justice.core.courts.Offence.offence;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.core.courts.ProsecutionCasesResulted.prosecutionCasesResulted;
import static uk.gov.justice.progression.courts.ApplicationsResulted.applicationsResulted;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.progression.service.RefDataService.REFERENCEDATA_GET_ALL_RESULT_DEFINITIONS;
import static uk.gov.moj.cpp.progression.utils.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.utils.FileUtil.jsonFromString;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.AttendanceDay;
import uk.gov.justice.core.courts.AttendanceType;
import uk.gov.justice.core.courts.CommittingCourt;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.CustodialEstablishment;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantAttendance;
import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultCategory;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.LinkType;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.NextHearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCasesResulted;
import uk.gov.justice.progression.courts.ApplicationsResulted;
import uk.gov.justice.progression.courts.HearingResulted;
import uk.gov.justice.progression.courts.api.ApplicationConcluded;
import uk.gov.justice.progression.courts.api.ProsecutionConcludedForLAA;
import uk.gov.justice.progression.courts.exract.ProsecutionConcluded;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.converter.SeedingHearingConverter;
import uk.gov.moj.cpp.progression.domain.pojo.PrisonCustodySuite;
import uk.gov.moj.cpp.progression.helper.CustodialEstablishmentUpdateHelper;
import uk.gov.moj.cpp.progression.exception.LaaAzureApimInvocationException;
import uk.gov.moj.cpp.progression.helper.HearingResultHelper;
import uk.gov.moj.cpp.progression.helper.HearingResultUnscheduledListingHelper;
import uk.gov.moj.cpp.progression.helper.SummonsHelper;
import uk.gov.moj.cpp.progression.helper.TestHelper;
import uk.gov.moj.cpp.progression.service.ApplicationParameters;
import uk.gov.moj.cpp.progression.service.AzureFunctionService;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.NextHearingService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.progression.service.UpdateDefendantService;
import uk.gov.moj.cpp.progression.service.dto.NextHearingDetails;
import uk.gov.moj.cpp.progression.service.utils.OffenceToCommittingCourtConverter;
import uk.gov.moj.cpp.progression.transformer.DefendantProceedingConcludedTransformer;
import uk.gov.moj.cpp.progression.transformer.HearingListingNeedsTransformer;
import uk.gov.moj.cpp.progression.transformer.HearingToHearingListingNeedsTransformer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings({"squid:S5976"})
@ExtendWith(MockitoExtension.class)
public class HearingResultEventProcessorTest {

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @Spy
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());



    @Mock
    private Sender sender;

    @Mock
    private SummonsHelper summonsHelper;

    @Captor
    private ArgumentCaptor<Envelope<?>> envelopeArgumentCaptor2;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> envelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<List<UUID>> applicationIdsArgumentCaptor;

    @Captor
    private ArgumentCaptor<List<CourtApplication>> courtApplicationsArgumentCaptor;

    @Captor
    private ArgumentCaptor<List<DefendantJudicialResult>> defendantJudicialResultArgumentCaptor;

    @Captor
    private ArgumentCaptor<Hearing> hearingArgumentCaptor;

    @Captor
    private ArgumentCaptor<ApplicationStatus> applicationStatusArgumentCaptor;

    @Captor
    private ArgumentCaptor<HearingListingStatus> hearingListingStatusArgumentCaptor;

    @Captor
    private ArgumentCaptor<ProsecutionCase> prosecutionCaseArgumentCaptor;

    @Captor
    private ArgumentCaptor<CourtCentre> courtCentreArgumentCaptor;

    @Captor
    private ArgumentCaptor<ListCourtHearing> listCourtHearingArgumentCaptor;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private ListingService listingService;

    @Mock
    private RefDataService referenceDataService;

    @Mock
    private NextHearingService nextHearingService;

    @Mock
    private HearingToHearingListingNeedsTransformer hearingToHearingListingNeedsTransformer;

    @Mock
    private HearingResultUnscheduledListingHelper hearingResultUnscheduledListingHelper;

    @Mock
    private HearingResultHelper hearingResultHelper;

    @Mock
    private OffenceToCommittingCourtConverter offenceToCommittingCourtConverter;

    @Mock
    private SeedingHearingConverter seedingHearingConverter;

    @Mock
    private HearingListingNeedsTransformer hearingListingNeedsTransformer;

    @Captor
    private ArgumentCaptor<Envelope> argumentCaptor;

    @Captor
    private ArgumentCaptor<Metadata> argumentCaptorMetadata;

    @Captor
    private ArgumentCaptor<JsonObject> argumentCaptorJsonObject;

    @Captor
    private ArgumentCaptor<CustodialEstablishment> argumentCaptorCustodialEstablishment;

    @Mock
    private UpdateDefendantService updateDefendantService;

    @Spy
    private CustodialEstablishmentUpdateHelper custodialEstablishmentUpdateHelper;


    @Mock
    private DefendantProceedingConcludedTransformer proceedingConcludedConverter;

    @Mock
    private AzureFunctionService azureFunctionService;

    @Mock
    private ApplicationParameters applicationParameters;

    @Captor
    private ArgumentCaptor<UUID> hearingIdCaptor;
    @Captor
    private ArgumentCaptor<HearingType> hearingTypeCaptor;
    @Captor
    private ArgumentCaptor<JurisdictionType> jurisdictionTypeCaptor;
    @Captor
    private ArgumentCaptor<Boolean> isBoxHearingCaptor;

    @InjectMocks
    private HearingResultEventProcessor eventProcessor;


    private static final UUID HEARING_ID = randomUUID();
    private static final UUID COURTROOM_ID = randomUUID();


    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void handleHearingResultWithoutApplicationOutcome() {

        final UUID courtApplicationId = randomUUID();
        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearing(hearing()
                        .withId(randomUUID())
                        .withCourtApplications(singletonList(courtApplication()
                                .withId(courtApplicationId)
                                .withType(courtApplicationType().withId(randomUUID()).withLinkType(LinkType.STANDALONE).build())
                                .build()))
                        .withDefendantAttendance(
                                singletonList(defendantAttendance()
                                        .withDefendantId(randomUUID())
                                        .withAttendanceDays(asList(
                                                attendanceDay().withAttendanceType(AttendanceType.BY_VIDEO).withDay(LocalDate.now()).build(),
                                                attendanceDay().withAttendanceType(AttendanceType.IN_PERSON).withDay(LocalDate.now().plusDays(7)).build()
                                        ))
                                        .build()))
                        .withIsBoxHearing(true)
                        .build())
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("public.hearing.resulted"),
                objectToJsonObjectConverter.convert(hearingResulted));

        this.eventProcessor.handleHearingResultedPublicEvent(event);

        verify(this.sender, times(1)).send(this.envelopeArgumentCaptor2.capture());
        assertThat(envelopeArgumentCaptor2.getValue(), notNullValue());
    }

    @Test
    public void shouldNotConsumePublicHearingResultedEventFromSJP() {
        final UUID courtApplicationId = randomUUID();
        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearing(hearing()
                        .withId(randomUUID())
                        .withIsSJPHearing(true)
                        .build())
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("public.hearing.resulted"),
                objectToJsonObjectConverter.convert(hearingResulted));

        verifyNoMoreInteractions(hearingResultUnscheduledListingHelper);
        verifyNoMoreInteractions(progressionService);
        verifyNoMoreInteractions(sender);
    }

    @Test
    public void handleHearingResultWithNoApplications() {

        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearing(hearing()
                        .withId(randomUUID())
                        .withDefendantAttendance(
                                asList(defendantAttendance()
                                        .withDefendantId(randomUUID())
                                        .withAttendanceDays(asList(
                                                attendanceDay().withAttendanceType(AttendanceType.BY_VIDEO).withDay(LocalDate.now()).build(),
                                                attendanceDay().withAttendanceType(AttendanceType.IN_PERSON).withDay(LocalDate.now().plusDays(7)).build()
                                        ))
                                        .build())
                        )
                        .build())
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("public.hearing.resulted"),
                objectToJsonObjectConverter.convert(hearingResulted));

        this.eventProcessor.handleHearingResultedPublicEvent(event);

        verify(this.sender).send(this.envelopeArgumentCaptor.capture());
        verify(progressionService, never()).linkApplicationsToHearing((JsonEnvelope) envelopeArgumentCaptor.capture(), hearingArgumentCaptor.capture(), applicationIdsArgumentCaptor.capture(), hearingListingStatusArgumentCaptor.capture());

        final List allValues = envelopeArgumentCaptor.getAllValues();
        assertThat(allValues.size(), is(1));
        assertThat(((DefaultEnvelope) allValues.get(0)).metadata().name(), equalTo("progression.command.hearing-result"));

    }


    @Test
    public void handleResultWhenProsecutionCaseIsPresentOnHearing() {

        final UUID courtApplicationId = randomUUID();

        final UUID commonUUID = randomUUID();
        final Defendant defendant1 = defendant()
                .withId(commonUUID)
                .withOffences(asList(offence()
                        .build()))
                .build();
        final Defendant defendant2 = defendant()
                .withId(commonUUID)
                .withOffences(asList(offence()
                        .build()))
                .build();
        final Defendant defendant3 = defendant()
                .withId(randomUUID())
                .withOffences(asList(offence()
                        .build()))
                .build();

        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(defendant1);
        defendants.add(defendant2);
        defendants.add(defendant3);

        final List<ProsecutionCase> prosecutionCases = new ArrayList<>();
        final ProsecutionCase prosecutionCase = prosecutionCase().withDefendants(defendants).build();
        prosecutionCases.add(prosecutionCase);


        final List<CourtApplication> courtApplications = singletonList(courtApplication()
                .withJudicialResults(asList(judicialResult()
                        .withCategory(JudicialResultCategory.FINAL).build(), judicialResult()
                        .withCategory(JudicialResultCategory.ANCILLARY).build()))
                .withId(courtApplicationId)
                .withType(courtApplicationType().withLinkType(LinkType.LINKED).build())
                .build());

        final ProsecutionCasesResulted prosecutionCasesResulted = prosecutionCasesResulted()
                .withHearing(Hearing.hearing()
                        .withId(randomUUID())
                        .withDefendantAttendance(
                                asList(defendantAttendance()
                                        .withDefendantId(randomUUID())
                                        .withAttendanceDays(asList(
                                                attendanceDay().withAttendanceType(AttendanceType.BY_VIDEO).withDay(LocalDate.now()).build(),
                                                attendanceDay().withAttendanceType(AttendanceType.IN_PERSON).withDay(LocalDate.now().plusDays(7)).build()
                                        ))
                                        .build())
                        )
                        .withProsecutionCases(prosecutionCases)
                        .withCourtApplications(courtApplications)
                        .build())
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.prosecution-cases-resulted"),
                objectToJsonObjectConverter.convert(prosecutionCasesResulted));

        this.eventProcessor.handleProsecutionCasesResulted(event);

        verify(this.sender, never()).send(this.envelopeArgumentCaptor.capture());
    }

    @Test
    public void handleResultWithEmptyProsecutionCaseOnHearing() throws Exception {

        final UUID courtApplicationId = randomUUID();

        final List<CourtApplication> courtApplications = singletonList(courtApplication()
                .withJudicialResults(asList(judicialResult()
                        .withCategory(JudicialResultCategory.FINAL).build(), judicialResult()
                        .withCategory(JudicialResultCategory.ANCILLARY).build()))
                .withId(courtApplicationId)
                .withType(courtApplicationType().withLinkType(LinkType.LINKED).build())
                .build());
        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearing(hearing()
                        .withId(randomUUID())
                        .withDefendantAttendance(
                                asList(defendantAttendance()
                                        .withDefendantId(randomUUID())
                                        .withAttendanceDays(asList(
                                                attendanceDay().withAttendanceType(AttendanceType.BY_VIDEO).withDay(LocalDate.now()).build(),
                                                attendanceDay().withAttendanceType(AttendanceType.IN_PERSON).withDay(LocalDate.now().plusDays(7)).build()
                                        ))
                                        .build())
                        )
                        .withCourtApplications(courtApplications)
                        .build())
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("public.hearing.resulted"),
                objectToJsonObjectConverter.convert(hearingResulted));

        this.eventProcessor.handleHearingResultedPublicEvent(event);

        verify(this.sender, times(1)).send(this.envelopeArgumentCaptor.capture());
    }

    @Test
    public void shouldProcessHandleApplicationsResultedAndShouldNotCallLaaApi() {
        final ApplicationsResulted applicationsResulted = buildApplicationsResulted(false, false);
        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.prosecution-applications-resulted"),
                objectToJsonObjectConverter.convert(applicationsResulted));

        eventProcessor.processHandleApplicationsResulted(event);

        verifyNoMoreInteractions(azureFunctionService);
        verify(this.sender).send(this.envelopeArgumentCaptor2.capture());
        final List<Envelope<?>> allValues = envelopeArgumentCaptor2.getAllValues();
        assertThat(allValues.size(), is(1));
        assertThat(allValues.get(0).metadata().name(), equalTo("progression.command.hearing-resulted-update-application"));

        verify(progressionService, never()).updateCourtApplicationStatus(any(JsonEnvelope.class), any(UUID.class), any(ApplicationStatus.class));
        verify(progressionService, never()).linkApplicationsToHearing(any(), any(), any(), any());
    }


    @Test
    public void shouldProcessHandleApplicationsResultedAndShouldCallLaaApiWhenProceedingsConcluded() {
        final ApplicationsResulted applicationsResulted = buildApplicationsResulted(true, true);
        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.prosecution-applications-resulted"),
                objectToJsonObjectConverter.convert(applicationsResulted));

        final ProsecutionConcludedForLAA prosecutionConcludedForLAA = ProsecutionConcludedForLAA.prosecutionConcludedForLAA()
                .withProsecutionConcluded(singletonList(ProsecutionConcluded.prosecutionConcluded()
                        .withIsConcluded(true)
                        .withApplicationConcluded(ApplicationConcluded.applicationConcluded()
                                .withSubjectId(randomUUID())
                                .withApplicationResultCode("APP_RESULT_CODE")
                                .withApplicationId(randomUUID())
                                .build())
                        .build()))
                .build();

        when(proceedingConcludedConverter.getApplicationConcludedRequest(any(), any())).thenReturn(prosecutionConcludedForLAA);
        when(azureFunctionService.concludeDefendantProceeding(anyString())).thenReturn(HttpStatus.SC_ACCEPTED);
        when(applicationParameters.getRetryTimes()).thenReturn("3");
        when(applicationParameters.getRetryInterval()).thenReturn("1000");

        eventProcessor.processHandleApplicationsResulted(event);

        verify(azureFunctionService).concludeDefendantProceeding(anyString());

        verify(this.sender).send(this.envelopeArgumentCaptor2.capture());
        final List<Envelope<?>> allValues = envelopeArgumentCaptor2.getAllValues();
        assertThat(allValues.size(), is(1));
        assertThat(allValues.get(0).metadata().name(), equalTo("progression.command.hearing-resulted-update-application"));

        verify(progressionService, never()).updateCourtApplicationStatus(any(JsonEnvelope.class), any(UUID.class), any(ApplicationStatus.class));
        verify(progressionService, never()).linkApplicationsToHearing(any(), any(), any(), any());


    }

    @Test
    public void shouldProcessHandleApplicationsResultedAndShouldNotCallLaaApiWhenOffencesNotThereForProsecutionCases() {
        final ApplicationsResulted applicationsResulted = buildApplicationsResulted(true, true, false);
        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.prosecution-applications-resulted"),
                objectToJsonObjectConverter.convert(applicationsResulted));

        eventProcessor.processHandleApplicationsResulted(event);

        verifyNoMoreInteractions(azureFunctionService);
        verify(this.sender).send(this.envelopeArgumentCaptor2.capture());
        final List<Envelope<?>> allValues = envelopeArgumentCaptor2.getAllValues();
        assertThat(allValues.size(), is(1));
        assertThat(allValues.get(0).metadata().name(), equalTo("progression.command.hearing-resulted-update-application"));

        verify(progressionService, never()).updateCourtApplicationStatus(any(JsonEnvelope.class), any(UUID.class), any(ApplicationStatus.class));
        verify(progressionService, never()).linkApplicationsToHearing(any(), any(), any(), any());
    }


    private static ApplicationsResulted buildApplicationsResulted(final boolean proceedingsConcluded, final boolean isWithLaaApplnReference) {
        return buildApplicationsResulted(proceedingsConcluded, isWithLaaApplnReference, true);
    }

    private static ApplicationsResulted buildApplicationsResulted(final boolean proceedingsConcluded, final boolean isWithLaaApplnReference, final boolean isWithOffences) {
        return applicationsResulted().withHearing(hearing().withCourtApplications(singletonList(courtApplication()
                .withId(randomUUID())
                .withApplicationStatus(ApplicationStatus.LISTED)
                .withCourtApplicationCases(asList(CourtApplicationCase.courtApplicationCase()
                                .withProsecutionCaseId(randomUUID())
                                .withOffences(getOffences(isWithLaaApplnReference, isWithOffences))
                                .build(),
                        CourtApplicationCase.courtApplicationCase()
                                .withProsecutionCaseId(randomUUID())
                                .withOffences(getOffences(isWithLaaApplnReference, isWithOffences))
                                .build()
                ))
                .withProceedingsConcluded(proceedingsConcluded)
                .build())).build()).build();
    }

    private static List<Offence> getOffences(final boolean isWithLaaApplnReference, final boolean isWithOffences) {
        if(!isWithOffences){
            return null;
        }
        return asList(
                offence()
                        .withId(randomUUID())
                        .withLaaApplnReference(getLaaApplnReference(isWithLaaApplnReference))
                        .build(),
                offence()
                        .withId(randomUUID())
                        .build());
    }

    private static LaaReference getLaaApplnReference(final boolean isWithLaaApplnReference) {
        if (isWithLaaApplnReference) {
            return LaaReference.laaReference()
                    .withStatusId(randomUUID())
                    .build();
        }
        return null;
    }

    @Test
    public void shouldProcessHandleApplicationsResultedAndThrowDefendantProceedingConcludedExceptionAfterAllRetries() {
        final ApplicationsResulted applicationsResulted = buildApplicationsResulted(true, true);
        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.prosecution-applications-resulted"),
                objectToJsonObjectConverter.convert(applicationsResulted));

        final ProsecutionConcludedForLAA prosecutionConcludedForLAA = ProsecutionConcludedForLAA.prosecutionConcludedForLAA()
                .withProsecutionConcluded(singletonList(ProsecutionConcluded.prosecutionConcluded()
                        .withIsConcluded(true)
                        .withApplicationConcluded(ApplicationConcluded.applicationConcluded()
                                .withSubjectId(randomUUID())
                                .withApplicationResultCode("APP_RESULT_CODE")
                                .withApplicationId(randomUUID())
                                .build())
                        .build()))
                .build();

        when(proceedingConcludedConverter.getApplicationConcludedRequest(any(), any())).thenReturn(prosecutionConcludedForLAA);
        when(azureFunctionService.concludeDefendantProceeding(anyString())).thenReturn(HttpStatus.SC_GATEWAY_TIMEOUT);
        when(applicationParameters.getRetryTimes()).thenReturn("3");
        when(applicationParameters.getRetryInterval()).thenReturn("1000");

        assertThrows(LaaAzureApimInvocationException.class, () -> eventProcessor.processHandleApplicationsResulted(event));
    }

    @Test
    public void shouldNotProcessHandleApplicationsResultedAsAmendReshareFeatureEnabledByDefault() {
        final ApplicationsResulted applicationsResulted = buildApplicationsResulted(true, true);
        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.prosecution-applications-resulted"),
                objectToJsonObjectConverter.convert(applicationsResulted));

        final ProsecutionConcludedForLAA prosecutionConcludedForLAA = ProsecutionConcludedForLAA.prosecutionConcludedForLAA()
                .withProsecutionConcluded(singletonList(ProsecutionConcluded.prosecutionConcluded()
                        .withIsConcluded(false)
                        .withApplicationConcluded(ApplicationConcluded.applicationConcluded()
                                .withSubjectId(randomUUID())
                                .withApplicationResultCode("APP_RESULT_CODE")
                                .withApplicationId(randomUUID())
                                .build())
                        .build()))
                .build();

        when(proceedingConcludedConverter.getApplicationConcludedRequest(any(), any())).thenReturn(prosecutionConcludedForLAA);
        when(azureFunctionService.concludeDefendantProceeding(anyString())).thenReturn(HttpStatus.SC_ACCEPTED);
        when(applicationParameters.getRetryTimes()).thenReturn("3");
        when(applicationParameters.getRetryInterval()).thenReturn("1000");

        eventProcessor.processHandleApplicationsResulted(event);

        verify(azureFunctionService).concludeDefendantProceeding(anyString());

        verify(this.sender).send(this.envelopeArgumentCaptor2.capture());
        final List<Envelope<?>> allValues = envelopeArgumentCaptor2.getAllValues();
        assertThat(allValues.size(), is(1));
        assertThat(allValues.get(0).metadata().name(), equalTo("progression.command.hearing-resulted-update-application"));
    }

    @Test
    public void shouldCallCommandWithCustodialEstablishment() {

        final JsonObject payload = jsonFromString(getPayload("stub-data/progression.event.applications-resulted.json")
                .replaceAll("%HEARING_ID%", HEARING_ID.toString())
                .replaceAll("%COURTROOM_ID%", COURTROOM_ID.toString())
        );

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.hearing-application-link-created"),
                payload);
        final UUID prisonId = randomUUID();
        final String prisonType = "prison";
        final String prisonName = "HMP Channings Wood";

        when(referenceDataService.getPrisonsCustodySuites(any())).thenReturn(asList(PrisonCustodySuite.prisonCustodySuite()
                .withId(prisonId)
                .withType(prisonType)
                .withName(prisonName)
                .build()));

        eventProcessor.processHandleApplicationsResulted(envelope);

        verify(sender).send(argumentCaptor.capture());
        verify(updateDefendantService).updateDefendantCustodialEstablishment(argumentCaptorMetadata.capture(), argumentCaptorJsonObject.capture(), argumentCaptorCustodialEstablishment.capture());

        assertThat(argumentCaptor.getAllValues().get(0).metadata().name(), is("progression.command.hearing-resulted-update-application"));

        final CustodialEstablishment custodialEstablishment = argumentCaptorCustodialEstablishment.getValue();
        assertThat(custodialEstablishment.getId(), is(prisonId));
        assertThat(custodialEstablishment.getName(), is(prisonName));
        assertThat(custodialEstablishment.getCustody(), is(prisonType));
    }

    @Test
    public void shouldPassShadowListedOffencesToListingService() throws IOException {
        final UUID defendantUuid1 = randomUUID();

        final List<ProsecutionCase> prosecutionCases = singletonList(prosecutionCase()
                .withDefendants(singletonList(defendant()
                        .withId(defendantUuid1)
                        .build()))
                .build());

        final UUID offenceUuid1 = randomUUID();
        final UUID offenceUuid2 = randomUUID();
        final List<UUID> shadowListedOffences = Arrays.asList(offenceUuid1, offenceUuid2);

        final UUID hearingId = randomUUID();
        final ProsecutionCasesResulted prosecutionCasesResulted = prosecutionCasesResulted()
                .withHearing(hearing()
                        .withId(hearingId)
                        .withType(hearingType().withDescription("Trial").build())
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .withProsecutionCases(prosecutionCases)
                        .build())
                .withShadowListedOffences(shadowListedOffences)
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.prosecution-cases-resulted"),
                objectToJsonObjectConverter.convert(prosecutionCasesResulted));

        final List<HearingListingNeeds> hearingListingNeedsList = singletonList(HearingListingNeeds.hearingListingNeeds()
                .withId(randomUUID())
                .build());
        final NextHearingDetails nextHearingDetails = new NextHearingDetails(hearingListingNeedsList, null);
        final List<HearingListingNeeds> hearingListingNeedsForNextHearings = singletonList(HearingListingNeeds.hearingListingNeeds().build());
        when(nextHearingService.getNextHearingDetails(any(), any(), any())).thenReturn(nextHearingDetails);
        when(hearingToHearingListingNeedsTransformer.transform(any(), any(), any())).thenReturn(hearingListingNeedsForNextHearings);
        when(hearingResultHelper.doHearingContainNextHearingResults(any())).thenReturn(true);
        this.eventProcessor.handleProsecutionCasesResulted(event);

        verify(this.sender).send(this.envelopeArgumentCaptor.capture());
        verify(progressionService, atLeastOnce()).updateCase(jsonEnvelopeArgumentCaptor.capture(), prosecutionCaseArgumentCaptor.capture(), courtApplicationsArgumentCaptor.capture(), defendantJudicialResultArgumentCaptor.capture(), courtCentreArgumentCaptor.capture(), hearingIdCaptor.capture(), hearingTypeCaptor.capture(), jurisdictionTypeCaptor.capture(), isBoxHearingCaptor.capture());
        verify(listingService, atLeastOnce()).listCourtHearing(jsonEnvelopeArgumentCaptor.capture(), listCourtHearingArgumentCaptor.capture());
        assertThat(prosecutionCaseArgumentCaptor.getValue().getDefendants().size(), is(1));
        assertThat(prosecutionCaseArgumentCaptor.getValue().getDefendants().get(0).getId(), is(defendantUuid1));
        assertThat(listCourtHearingArgumentCaptor.getValue().getShadowListedOffences().size(), is(2));
        assertThat(listCourtHearingArgumentCaptor.getValue().getShadowListedOffences(), hasItems(offenceUuid1, offenceUuid2));
        assertThat(hearingIdCaptor.getValue(), is(hearingId));
        assertThat(hearingTypeCaptor.getValue().getDescription(), is("Trial"));
        assertThat(jurisdictionTypeCaptor.getValue(), is(JurisdictionType.CROWN));
    }

    @Test
    public void shouldPassCommittingCourtIsMagsAndSentToCC() throws IOException {
        final UUID defendantUUUID = randomUUID();
        final UUID offenceUUID = randomUUID();
        final List<ProsecutionCase> prosecutionCases = mockPublicHearingResultedWithSendingCourtOffenceResult(defendantUUUID, offenceUUID, "CCSC", "SentToCC");

        sendToCrownCourt(prosecutionCases, defendantUUUID);
    }

    @Test
    public void shouldPassCommittingCourtIsMagsAndSentToCCWhenResultDefinitionGroupHasMultipleCommaSeparatedValues() throws IOException {
        final UUID defendantUUUID = randomUUID();
        final UUID offenceUUID = randomUUID();
        final List<ProsecutionCase> prosecutionCases = mockPublicHearingResultedWithSendingCourtOffenceResult(defendantUUUID, offenceUUID, "CCSC", "SentToCC, ELMON");

        sendToCrownCourt(prosecutionCases, defendantUUUID);
    }

    @Test
    public void shouldPassCommittingCourtIsMagsAndCommittedToCC() throws IOException {
        final UUID defendantUUUID = randomUUID();
        final UUID offenceUUID = randomUUID();
        final List<ProsecutionCase> prosecutionCases = mockPublicHearingResultedWithSendingCourtOffenceResult(defendantUUUID, offenceUUID, "CCIC", "CommittedToCC");

        sendToCrownCourt(prosecutionCases, defendantUUUID);
    }

    @Test
    public void shouldExtendHearingWhenJudicialResultUnderApplicationHasNextHearing() {
        final UUID courtApplicationId = randomUUID();

        final List<CourtApplication> courtApplications = singletonList(courtApplication()
                .withJudicialResults(asList(JudicialResult.judicialResult()
                        .withCategory(JudicialResultCategory.FINAL).build(), JudicialResult.judicialResult()
                        .withCategory(JudicialResultCategory.ANCILLARY)
                        .withNextHearing(NextHearing.nextHearing()
                                .withExistingHearingId(randomUUID())
                                .build()).build()))
                .withId(courtApplicationId)
                .withType(CourtApplicationType.courtApplicationType().withLinkType(LinkType.LINKED).build())
                .build());

        runTestsForJudicialResultUnderApplication(courtApplications, false);
    }

    @Test
    public void shouldExtendHearingWhenJudicialResultUnderCourtOrderHasNextHearing() {
        final UUID courtApplicationId = randomUUID();

        final List<CourtApplication> courtApplications = singletonList(courtApplication()
                .withCourtOrder(CourtOrder.courtOrder()
                        .withCourtOrderOffences(singletonList(CourtOrderOffence.courtOrderOffence()
                                .withOffence(Offence.offence()
                                        .withJudicialResults(asList(JudicialResult.judicialResult()
                                                .withCategory(JudicialResultCategory.FINAL).build(), JudicialResult.judicialResult()
                                                .withCategory(JudicialResultCategory.ANCILLARY)
                                                .withNextHearing(NextHearing.nextHearing()
                                                        .withExistingHearingId(randomUUID())
                                                        .build()).build()))
                                        .build())
                                .build()))
                        .build())
                .withId(courtApplicationId)
                .withType(CourtApplicationType.courtApplicationType().withLinkType(LinkType.LINKED).build())
                .build());

        runTestsForJudicialResultUnderApplication(courtApplications, false);
    }

    @Test
    public void shouldExtendHearingWhenJudicialResultUnderCourtApplicationCasesHasNextHearing() {
        final UUID courtApplicationId = randomUUID();

        final List<CourtApplication> courtApplications = singletonList(courtApplication()
                .withCourtApplicationCases(singletonList(CourtApplicationCase.courtApplicationCase()
                        .withOffences(singletonList(Offence.offence()
                                .withJudicialResults(asList(JudicialResult.judicialResult()
                                        .withCategory(JudicialResultCategory.FINAL).build(), JudicialResult.judicialResult()
                                        .withCategory(JudicialResultCategory.ANCILLARY)
                                        .withNextHearing(NextHearing.nextHearing()
                                                .withExistingHearingId(randomUUID())
                                                .build()).build()))
                                .build()))
                        .build()))
                .withId(courtApplicationId)
                .withType(CourtApplicationType.courtApplicationType().withLinkType(LinkType.LINKED).build())
                .build());

        runTestsForJudicialResultUnderApplication(courtApplications, false);
    }

    @Test
    public void shouldCreateNewHearingWhenJudicialResultUnderApplicationHasNextHearing() {
        final UUID courtApplicationId = randomUUID();

        final List<CourtApplication> courtApplications = singletonList(courtApplication()
                .withJudicialResults(asList(JudicialResult.judicialResult()
                        .withCategory(JudicialResultCategory.FINAL).build(), JudicialResult.judicialResult()
                        .withCategory(JudicialResultCategory.ANCILLARY)
                        .withNextHearing(NextHearing.nextHearing()
                                .build()).build()))
                .withId(courtApplicationId)
                .withType(CourtApplicationType.courtApplicationType().withLinkType(LinkType.LINKED).build())
                .build());

        runTestsForJudicialResultUnderApplication(courtApplications, true);
    }

    @Test
    public void shouldCreateNewHearingWhenJudicialResultUnderCourtOrderHasNextHearing() {
        final UUID courtApplicationId = randomUUID();

        final List<CourtApplication> courtApplications = singletonList(courtApplication()
                .withCourtOrder(CourtOrder.courtOrder()
                        .withCourtOrderOffences(singletonList(CourtOrderOffence.courtOrderOffence()
                                .withOffence(Offence.offence()
                                        .withJudicialResults(asList(JudicialResult.judicialResult()
                                                .withCategory(JudicialResultCategory.FINAL).build(), JudicialResult.judicialResult()
                                                .withCategory(JudicialResultCategory.ANCILLARY)
                                                .withNextHearing(NextHearing.nextHearing()
                                                        .build()).build()))
                                        .build())
                                .build()))
                        .build())
                .withId(courtApplicationId)
                .withType(CourtApplicationType.courtApplicationType().withLinkType(LinkType.LINKED).build())
                .build());

        runTestsForJudicialResultUnderApplication(courtApplications, true);
    }

    @Test
    public void shouldCreateNewHearingWhenJudicialResultUnderCourtApplicationCasesHasNextHearing() {
        final UUID courtApplicationId = randomUUID();

        final List<CourtApplication> courtApplications = singletonList(courtApplication()
                .withCourtApplicationCases(singletonList(CourtApplicationCase.courtApplicationCase()
                        .withOffences(singletonList(Offence.offence()
                                .withJudicialResults(asList(JudicialResult.judicialResult()
                                        .withCategory(JudicialResultCategory.FINAL).build(), JudicialResult.judicialResult()
                                        .withCategory(JudicialResultCategory.ANCILLARY)
                                        .withNextHearing(NextHearing.nextHearing()
                                                .build()).build()))
                                .build()))
                        .build()))
                .withId(courtApplicationId)
                .withType(CourtApplicationType.courtApplicationType().withLinkType(LinkType.LINKED).build())
                .build());

        runTestsForJudicialResultUnderApplication(courtApplications, true);
    }

    private void runTestsForJudicialResultUnderApplication(final List<CourtApplication> courtApplications, boolean newHearing) {
        final CommittingCourt committingCourt = TestHelper.buildCommittingCourt();
        final ProsecutionCasesResulted prosecutionCasesResulted = ProsecutionCasesResulted.prosecutionCasesResulted()
                .withHearing(hearing()
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
                .withCommittingCourt(committingCourt)
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.prosecution-cases-resulted"),
                objectToJsonObjectConverter.convert(prosecutionCasesResulted));

        eventProcessor.handleHearingResultedPublicEvent(event);
        verify(this.sender, times(1)).send(this.envelopeArgumentCaptor.capture());
    }

    private void sendToCrownCourt(final List<ProsecutionCase> prosecutionCases, final UUID defendantUUUID) throws IOException {
        final CommittingCourt committingCourt = TestHelper.buildCommittingCourt();
        final UUID hearingId = randomUUID();
        final ProsecutionCasesResulted prosecutionCasesResulted = prosecutionCasesResulted()
                .withHearing(hearing()
                        .withId(hearingId)
                        .withType(hearingType().withDescription("Trial").build())
                        .withProsecutionCases(prosecutionCases)
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .build())
                .withCommittingCourt(committingCourt)
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.prosecution-cases-resulted"),
                objectToJsonObjectConverter.convert(prosecutionCasesResulted));

        final List<HearingListingNeeds> hearingListingNeedsList = singletonList(HearingListingNeeds.hearingListingNeeds()
                .withId(randomUUID())
                .build());
        final NextHearingDetails nextHearingDetails = new NextHearingDetails(hearingListingNeedsList, null);
        final List<HearingListingNeeds> hearingListingNeedsForNextHearings = singletonList(HearingListingNeeds.hearingListingNeeds().build());

        when(nextHearingService.getNextHearingDetails(any(), any(), any())).thenReturn(nextHearingDetails);
        when(hearingToHearingListingNeedsTransformer.transform(any(), any(), any())).thenReturn(hearingListingNeedsForNextHearings);
        when(hearingResultHelper.doHearingContainNextHearingResults(any())).thenReturn(true);

        this.eventProcessor.handleProsecutionCasesResulted(event);

        verify(this.sender).send(this.envelopeArgumentCaptor.capture());
        verify(nextHearingService, atLeastOnce()).getNextHearingDetails(any(), Mockito.eq(true), any());
        verify(progressionService, atLeastOnce()).updateCase(jsonEnvelopeArgumentCaptor.capture(), prosecutionCaseArgumentCaptor.capture(), courtApplicationsArgumentCaptor.capture(), defendantJudicialResultArgumentCaptor.capture(), courtCentreArgumentCaptor.capture(), hearingIdCaptor.capture(), hearingTypeCaptor.capture(), jurisdictionTypeCaptor.capture(), isBoxHearingCaptor.capture());
        verify(listingService, atLeastOnce()).listCourtHearing(jsonEnvelopeArgumentCaptor.capture(), listCourtHearingArgumentCaptor.capture());

        assertThat(prosecutionCaseArgumentCaptor.getValue().getDefendants().size(), is(1));
        assertThat(prosecutionCaseArgumentCaptor.getValue().getDefendants().get(0).getId(), is(defendantUUUID));
        assertThat(hearingIdCaptor.getValue(), is(hearingId));
        assertThat(hearingTypeCaptor.getValue().getDescription(), is("Trial"));
        assertThat(jurisdictionTypeCaptor.getValue(), is(JurisdictionType.CROWN));
    }

    private List<ProsecutionCase> mockPublicHearingResultedWithSendingCourtOffenceResult(final UUID defendantUUUID, final UUID offenceUUID, final String cjsCode, final String resultDefinitionGroup) {

        return singletonList(prosecutionCase()
                .withDefendants(singletonList(defendant()
                        .withId(defendantUUUID)
                        .withOffences(singletonList(offence()
                                .withId(offenceUUID)
                                .withJudicialResults(singletonList(judicialResult()
                                        .withCjsCode(cjsCode)
                                        .withResultDefinitionGroup(resultDefinitionGroup)
                                        .build()))
                                .build()))
                        .build()))
                .build());

    }

    private Optional<JsonObject> getHearingJson() {
        final Hearing hearing = hearing()
                .withId(randomUUID())
                .withHasSharedResults(false)
                .build();
        final JsonObject hearingJson = createObjectBuilder()
                .add("hearing", objectToJsonObjectConverter.convert(hearing))
                .build();
        return Optional.of(hearingJson);
    }

    private JsonEnvelope generateResultDefinitionsJson() throws IOException {

        final String jsonString = Resources.toString(Resources.getResource("referencedata.get-all-result-definitions.json"), Charset.defaultCharset());
        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName(REFERENCEDATA_GET_ALL_RESULT_DEFINITIONS);

        final JsonObject payload = Json.createReader(
                        new ByteArrayInputStream(jsonString.getBytes()))
                .readObject();

        return envelopeFrom(metadataBuilder, payload);
    }
}
