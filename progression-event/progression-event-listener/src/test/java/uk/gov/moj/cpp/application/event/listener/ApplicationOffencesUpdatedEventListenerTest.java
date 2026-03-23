package uk.gov.moj.cpp.application.event.listener;

import java.time.LocalDate;
import java.util.Arrays;
import javax.json.Json;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.core.courts.ApplicationOffencesUpdated;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationProceedingsInitiated;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.InitiateCourtApplicationProceedings;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.hearing.courts.Subject;
import uk.gov.justice.progression.events.ApplicationLaaReferenceUpdatedForHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.InitiateCourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;

import javax.json.JsonObject;
import java.util.List;
import java.util.UUID;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.InitiateCourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;


@ExtendWith(MockitoExtension.class)
public class ApplicationOffencesUpdatedEventListenerTest {

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObject jsonObject;

    @Mock
    private JsonObject payload;

    @Mock
    private CourtApplicationRepository courtApplicationRepository;

    @Mock
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Mock
    private InitiateCourtApplicationRepository initiateCourtApplicationRepository;

    @Mock
    private HearingEntity hearingEntity;

    @Captor
    private ArgumentCaptor<CourtApplication> courtApplicationArgumentCaptor;

    @Captor
    private ArgumentCaptor<InitiateCourtApplicationEntity> initiateCourtApplicationEntityArgumentCaptor;

    @Captor
    private ArgumentCaptor<ProsecutionCase> prosecutionCaseArgumentCaptor;

    @InjectMocks
    private ApplicationOffencesUpdatedEventListener listener;

    @Spy
    private ListToJsonArrayConverter<?> jsonConverter;

    @Mock
    private HearingRepository hearingRepository;

    @BeforeEach
    public void initMocks() {
        setField(this.jsonConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonConverter, "stringToJsonObjectConverter", new JsonObjectConvertersFactory().stringToJsonObjectConverter());
    }

    @Test
    public void shouldHandleApplicationOffencesUpdated() {

        UUID applicationId = UUID.randomUUID();
        UUID offenceId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID caseId = randomUUID();
        UUID masterDefendantId = randomUUID();

        LaaReference laaReference = LaaReference.laaReference().withApplicationReference("applicationReference")
                .withOffenceLevelStatus("Granted")
                .withStatusCode("statusCode").withStatusDescription("description").build();


        ApplicationOffencesUpdated applicationOffencesUpdated = ApplicationOffencesUpdated.applicationOffencesUpdated()
                .withApplicationId(applicationId)
                .withSubjectId(subjectId)
                .withOffenceId(offenceId)
                .withLaaReference(laaReference)
                .build();

        final CourtApplicationEntity applicationEntity = new CourtApplicationEntity();
        applicationEntity.setApplicationId(applicationId);
        applicationEntity.setPayload(payload.toString());

        ProsecutionCaseEntity caseEntity = new ProsecutionCaseEntity();
        caseEntity.setCaseId(caseId);
        caseEntity.setPayload(payload.toString());


        InitiateCourtApplicationEntity initiateCourtApplicationEntity = new InitiateCourtApplicationEntity();
        initiateCourtApplicationEntity.setApplicationId(applicationId);
        initiateCourtApplicationEntity.setPayload(payload.toString());
        when(jsonObjectToObjectConverter.convert(payload, ApplicationOffencesUpdated.class)).thenReturn(applicationOffencesUpdated);
        when(objectToJsonObjectConverter.convert(any(CourtApplication.class))).thenReturn(jsonObject);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(courtApplicationRepository.findByApplicationId(applicationId)).thenReturn(applicationEntity);
        when(initiateCourtApplicationRepository.findBy(applicationId)).thenReturn(initiateCourtApplicationEntity);
        when(prosecutionCaseRepository.findByCaseId(caseId)).thenReturn(caseEntity);


        final JsonObject applicationJson = createObjectBuilder().build();
        when(stringToJsonObjectConverter.convert(applicationEntity.getPayload())).thenReturn(applicationJson);
        CourtApplication persistedApplication = CourtApplication.courtApplication()
                .withSubject(CourtApplicationParty.courtApplicationParty().withId(subjectId)
                        .withMasterDefendant(MasterDefendant.masterDefendant().withMasterDefendantId(masterDefendantId).build())
                        .build())
                .withCourtApplicationCases(buildCourtApplicationCases(caseId,offenceId))
                .build();

        when(jsonObjectToObjectConverter.convert(applicationJson, CourtApplication.class)).thenReturn(persistedApplication);


        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings =
                InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings()
                        .withCourtApplication(persistedApplication)
                        .build();

        final JsonObject entityPayload = createObjectBuilder().build();
        when(stringToJsonObjectConverter.convert(initiateCourtApplicationEntity.getPayload())).thenReturn(entityPayload);
        when(jsonObjectToObjectConverter.convert(eq(entityPayload), eq(InitiateCourtApplicationProceedings.class))).thenReturn(initiateCourtApplicationProceedings);
        when(objectToJsonObjectConverter.convert(any(InitiateCourtApplicationProceedings.class))).thenReturn(createObjectBuilder().build());

        when(stringToJsonObjectConverter.convert(caseEntity.getPayload())).thenReturn(entityPayload);
        when(jsonObjectToObjectConverter.convert(entityPayload, ProsecutionCase.class)).thenReturn(buildProsecutionCase(caseId, masterDefendantId, offenceId));
        when(objectToJsonObjectConverter.convert(any(ProsecutionCase.class))).thenReturn(createObjectBuilder().build());

        listener.processApplicationOffencesUpdated(envelope);
        verify(objectToJsonObjectConverter, times(1)).convert(courtApplicationArgumentCaptor.capture());
        final CourtApplication courtApplication = courtApplicationArgumentCaptor.getAllValues().get(0);
        assertThat(courtApplication.getCourtApplicationCases().size(), is(3));
        assertThat(courtApplication.getCourtApplicationCases().get(0).getOffences().get(0).getId(), is(offenceId));
        assertThat(courtApplication.getCourtApplicationCases().get(0).getOffences().get(0).getLaaApplnReference(), is(laaReference));
        assertThat(courtApplication.getCourtApplicationCases().get(1).getOffences().get(0).getLaaApplnReference().getStatusCode(), is("404"));

        verify(initiateCourtApplicationRepository).save(initiateCourtApplicationEntityArgumentCaptor.capture());
        final InitiateCourtApplicationEntity entity = initiateCourtApplicationEntityArgumentCaptor.getValue();
        assertThat(entity.getApplicationId(), is(applicationId));

        verify(objectToJsonObjectConverter, times(1)).convert(prosecutionCaseArgumentCaptor.capture());
        final ProsecutionCase prosecutionCase = prosecutionCaseArgumentCaptor.getAllValues().get(0);

        assertThat(prosecutionCase.getDefendants(), hasSize(1));
        assertThat(prosecutionCase.getDefendants().get(0).getLegalAidStatus(), is("Granted"));
        assertThat(prosecutionCase.getDefendants().get(0).getOffences(), hasSize(1));
        assertThat(prosecutionCase.getDefendants().get(0).getOffences().get(0).getLaaApplnReference(), is(laaReference));

    }


    @Test
    void testUpdateApplicationLaaReferenceForHearing() {
        // Mock data
        final UUID hearingId = UUID.randomUUID();
        final UUID applicationId = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();
        final UUID subjectId = UUID.randomUUID();
        final LaaReference laaReference = LaaReference.laaReference().withApplicationReference("applicationReference")
                .withStatusCode("statusCode").withStatusDescription("description").build();
        final JsonObject updatedJsonObject = mock(JsonObject.class);
        final JsonObject hearingJsonObject = Json.createObjectBuilder().build();

        final ApplicationLaaReferenceUpdatedForHearing applicationLaaReferenceUpdatedForHearing = ApplicationLaaReferenceUpdatedForHearing.applicationLaaReferenceUpdatedForHearing()
                .withHearingId(hearingId)
                .withApplicationId(applicationId)
                .withSubjectId(subjectId)
                .withOffenceId(offenceId)
                .withLaaReference(laaReference)
                .build();

        Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withCourtApplications(List.of(CourtApplication.courtApplication()
                        .withId(applicationId)
                        .withCourtApplicationCases(buildCourtApplicationCases(offenceId))
                        .withSubject(CourtApplicationParty.courtApplicationParty().withId(subjectId).build()).build()))
                .build();

        when(jsonObjectToObjectConverter.convert(hearingJsonObject, Hearing.class)).thenReturn(hearing);
        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        when(hearingEntity.getPayload()).thenReturn(hearingJsonObject.toString());


        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, ApplicationLaaReferenceUpdatedForHearing.class)).thenReturn(applicationLaaReferenceUpdatedForHearing);
        when(objectToJsonObjectConverter.convert(any(Hearing.class))).thenReturn(updatedJsonObject);

        listener.updateApplicationLaaReferenceForHearing(envelope);

        verify(hearingRepository).save(hearingEntity);
        verify(hearingEntity).setPayload(updatedJsonObject.toString());
    }

    @Test
    void testUpdateApplicationLaaReferenceForHearingWhenOffenceIdIsNull() {
        // Mock data
        final UUID hearingId = UUID.randomUUID();
        final UUID applicationId = UUID.randomUUID();
        final UUID subjectId = UUID.randomUUID();
        final LaaReference laaReference = LaaReference.laaReference().withApplicationReference("applicationReference")
                .withStatusCode("statusCode").withStatusDescription("description").build();
        final JsonObject updatedJsonObject = mock(JsonObject.class);
        final JsonObject hearingJsonObject = Json.createObjectBuilder().build();

        final ApplicationLaaReferenceUpdatedForHearing applicationLaaReferenceUpdatedForHearing = ApplicationLaaReferenceUpdatedForHearing.applicationLaaReferenceUpdatedForHearing()
                .withHearingId(hearingId)
                .withApplicationId(applicationId)
                .withSubjectId(subjectId)
                .withLaaReference(laaReference)
                .build();

        Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withCourtApplications(List.of(CourtApplication.courtApplication()
                        .withId(applicationId)
                        .withSubject(CourtApplicationParty.courtApplicationParty().withId(subjectId).build()).build()))
                .build();

        when(jsonObjectToObjectConverter.convert(hearingJsonObject, Hearing.class)).thenReturn(hearing);
        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);
        when(hearingEntity.getPayload()).thenReturn(hearingJsonObject.toString());


        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, ApplicationLaaReferenceUpdatedForHearing.class)).thenReturn(applicationLaaReferenceUpdatedForHearing);
        when(objectToJsonObjectConverter.convert(any(Hearing.class))).thenReturn(updatedJsonObject);

        listener.updateApplicationLaaReferenceForHearing(envelope);

        verify(hearingRepository).save(hearingEntity);
        verify(hearingEntity).setPayload(updatedJsonObject.toString());
    }

    @Test
    void testNoInteractionUpdateApplicationLaaReferenceForHearingWhenHearingNotFound() {
        // Mock data
        final UUID hearingId = UUID.randomUUID();
        final UUID applicationId = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();
        final UUID subjectId = UUID.randomUUID();
        final LaaReference laaReference = LaaReference.laaReference().withApplicationReference("applicationReference")
                .withStatusCode("statusCode").withStatusDescription("description").build();
        final ApplicationLaaReferenceUpdatedForHearing applicationLaaReferenceUpdatedForHearing = ApplicationLaaReferenceUpdatedForHearing.applicationLaaReferenceUpdatedForHearing()
                .withHearingId(hearingId)
                .withApplicationId(applicationId)
                .withSubjectId(subjectId)
                .withOffenceId(offenceId)
                .withLaaReference(laaReference)
                .build();
        when(hearingRepository.findBy(hearingId)).thenReturn(null);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, ApplicationLaaReferenceUpdatedForHearing.class)).thenReturn(applicationLaaReferenceUpdatedForHearing);

        listener.updateApplicationLaaReferenceForHearing(envelope);

        verify(hearingRepository, never()).save(hearingEntity);
    }

    private List<CourtApplicationCase> buildCourtApplicationCases(UUID offenceId) {
        Offence offence1 = Offence.offence().withId(offenceId).withLaaApplnReference(LaaReference.laaReference().withStatusCode("404").build()).build();
        Offence offence2 = Offence.offence().withId(UUID.randomUUID()).withLaaApplnReference(LaaReference.laaReference().withStatusCode("404").build()).build();
        Offence offence3 = Offence.offence().withId(UUID.randomUUID()).withLaaApplnReference(LaaReference.laaReference().withStatusCode("404").build()).build();
        Offence offence4 = Offence.offence().withId(UUID.randomUUID()).withLaaApplnReference(LaaReference.laaReference().withStatusCode("404").build()).build();

        CourtApplicationCase courtApplicationCase1 = CourtApplicationCase.courtApplicationCase().withOffences(List.of(offence1, offence2)).build();
        CourtApplicationCase courtApplicationCase2 = CourtApplicationCase.courtApplicationCase().withOffences(List.of(offence3)).build();
        CourtApplicationCase courtApplicationCase3 = CourtApplicationCase.courtApplicationCase().withOffences(List.of(offence4)).build();
        return List.of(courtApplicationCase1, courtApplicationCase2, courtApplicationCase3);
    }

    private List<CourtApplicationCase> buildCourtApplicationCases(UUID caseId, UUID offenceId){
        Offence offence1 = Offence.offence().withId(offenceId).withLaaApplnReference(LaaReference.laaReference().withStatusCode("404").build()).build();
        Offence offence2 = Offence.offence().withId(randomUUID()).withLaaApplnReference(LaaReference.laaReference().withStatusCode("404").build()).build();
        Offence offence3 = Offence.offence().withId(randomUUID()).withLaaApplnReference(LaaReference.laaReference().withStatusCode("404").build()).build();
        Offence offence4 = Offence.offence().withId(randomUUID()).withLaaApplnReference(LaaReference.laaReference().withStatusCode("404").build()).build();

        CourtApplicationCase courtApplicationCase1 =CourtApplicationCase.courtApplicationCase().withOffences(List.of(offence1, offence2)).withProsecutionCaseId(caseId).build();
        CourtApplicationCase courtApplicationCase2 =CourtApplicationCase.courtApplicationCase().withOffences(List.of(offence3)).withProsecutionCaseId(randomUUID()).build();
        CourtApplicationCase courtApplicationCase3 =CourtApplicationCase.courtApplicationCase().withOffences(List.of(offence4)).withProsecutionCaseId(randomUUID()).build();
        return List.of(courtApplicationCase1, courtApplicationCase2, courtApplicationCase3);
    }

    private ProsecutionCase buildProsecutionCase(final UUID caseId, final UUID masterDefendantId, final UUID offenceId) {
        return prosecutionCase().withId(caseId)
                .withDefendants(asList(Defendant.defendant().withId(masterDefendantId)
                        .withMasterDefendantId(masterDefendantId)
                        .withOffences(asList(Offence.offence().withId(offenceId)
                                .withArrestDate(LocalDate.now())
                                .withChargeDate(LocalDate.now())
                                .withOffenceCode("TH68023A")
                                .build()))
                        .build()))
                .build();
    }
}