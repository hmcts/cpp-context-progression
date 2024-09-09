package uk.gov.moj.cpp.application.event.listener;

import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.CourtApplicationType.courtApplicationType;
import static uk.gov.justice.core.courts.CourtOrderOffence.courtOrderOffence;
import static uk.gov.justice.core.courts.HearingResultedApplicationUpdated.hearingResultedApplicationUpdated;
import static uk.gov.justice.core.courts.Offence.offence;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ApplicationEjected;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationAddedToCase;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationCreated;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationProceedingsEdited;
import uk.gov.justice.core.courts.CourtApplicationProceedingsInitiated;
import uk.gov.justice.core.courts.CourtApplicationStatusChanged;
import uk.gov.justice.core.courts.CourtApplicationUpdated;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.DefendantAddressOnApplicationUpdated;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.HearingResultedApplicationUpdated;
import uk.gov.justice.core.courts.InitiateCourtApplicationProceedings;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.LinkType;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingApplicationKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.InitiateCourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.InitiateCourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.mapping.SearchProsecutionCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.minidev.json.JSONValue;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
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
public class CourtApplicationEventListenerTest {

    public static final String FOR_NOWS = "for Nows";
    public static final String FOR_NOT_NOWS = "for Not Nows";

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Mock
    private CourtApplicationRepository repository;

    @Mock
    private CourtApplicationCaseRepository courtApplicationCaseRepository;

    @Mock
    private HearingApplicationRepository hearingApplicationRepository;

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private InitiateCourtApplicationRepository initiateCourtApplicationRepository;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObject jsonObject;

    @Mock
    private CourtApplicationCreated courtApplicationCreated;

    @Mock
    private CourtApplicationAddedToCase courtApplicationAddedToCase;

    @Mock
    private CourtApplicationUpdated courtApplicationUpdated;

    @Mock
    private CourtApplicationEntity courtApplicationEntity;

    @Mock
    private CourtApplication courtApplication;

    @Mock
    private SearchProsecutionCase searchApplication;

    @Captor
    private ArgumentCaptor<CourtApplicationEntity> argumentCaptor;

    @Captor
    private ArgumentCaptor<HearingEntity> hearingEntityArgumentCaptor;

    @Captor
    private ArgumentCaptor<CourtApplicationCaseEntity> courtApplicationCaseArgumentCaptor;

    @Captor
    private ArgumentCaptor<CourtApplication> courtApplicationArgumentCaptor;

    @Mock
    private JsonObject payload;

    @Mock
    private Metadata metadata;

    @InjectMocks
    private CourtApplicationEventListener eventListener;

    @Spy
    private ListToJsonArrayConverter<?> jsonConverter;

    private static final String COURT_APPLICATIONS = "courtApplications";
    private static final String APPLICATION_STATUS = "applicationStatus";

    @Before
    public void initMocks() {
        setField(this.jsonConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonConverter, "stringToJsonObjectConverter", new StringToJsonObjectConverter());
    }

    @Test
    public void shouldHandleCourtApplicationCreatedEvent() {

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, CourtApplicationCreated.class)).thenReturn(courtApplicationCreated);
        when(envelope.metadata()).thenReturn(metadata);
        when(courtApplication.getId()).thenReturn(UUID.randomUUID());
        when(courtApplicationCreated.getCourtApplication()).thenReturn(courtApplication);
        when(objectToJsonObjectConverter.convert(any(CourtApplication.class))).thenReturn(jsonObject);
        eventListener.processCourtApplicationCreated(envelope);
        verify(repository).save(argumentCaptor.capture());
    }

    @Test
    public void shouldHandleCourtApplicationStatusChangedEvent() {
        final UUID applicationId = UUID.randomUUID();
        final CourtApplicationEntity persistedEntity = new CourtApplicationEntity();
        persistedEntity.setApplicationId(applicationId);
        persistedEntity.setPayload(payload.toString());

        when(repository.findByApplicationId(applicationId)).thenReturn(persistedEntity);
        when(stringToJsonObjectConverter.convert(payload.toString())).thenReturn(payload);
        when(envelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(jsonObjectToObjectConverter.convert(jsonObject, CourtApplicationStatusChanged.class))
                .thenReturn(CourtApplicationStatusChanged.courtApplicationStatusChanged()
                        .withId(applicationId)
                        .withApplicationStatus(ApplicationStatus.LISTED)
                        .build());
        when(jsonObjectToObjectConverter.convert(payload, CourtApplication.class)).thenReturn(courtApplication);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(payload);
        when(envelope.metadata()).thenReturn(metadata);

        eventListener.processCourtApplicationStatusChanged(envelope);
        verify(repository).save(argumentCaptor.capture());

    }

    @Test
    public void shouldHandleCourtApplicationUpdatedEvent() {

        when(envelope.payloadAsJsonObject()).thenReturn(payload);

        when(jsonObjectToObjectConverter.convert(payload, CourtApplicationUpdated.class))
                .thenReturn(courtApplicationUpdated);
        when(envelope.metadata()).thenReturn(metadata);
        when(courtApplication.getId()).thenReturn(UUID.randomUUID());
        when(courtApplicationUpdated.getCourtApplication()).thenReturn(courtApplication);
        when(repository.findByApplicationId(any())).thenReturn(courtApplicationEntity);
        when(objectToJsonObjectConverter.convert(any(CourtApplication.class))).thenReturn(jsonObject);
        eventListener.processCourtApplicationUpdated(envelope);
        verify(repository).save(argumentCaptor.capture());
    }


    @Test
    public void shouldProcessCourtApplicationProceedingsInitiated() {

        final UUID applicationId = randomUUID();
        final CourtApplication courtApplication = CourtApplication.courtApplication().withId(applicationId).build();
        final CourtApplicationProceedingsInitiated courtApplicationProceedingsInitiated = CourtApplicationProceedingsInitiated.courtApplicationProceedingsInitiated().
                withCourtApplication(courtApplication).build();
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, CourtApplicationProceedingsInitiated.class))
                .thenReturn(courtApplicationProceedingsInitiated);
        when(objectToJsonObjectConverter.convert(courtApplicationProceedingsInitiated)).thenReturn(createObjectBuilder().build());
        when(envelope.metadata()).thenReturn(metadata);
        eventListener.processCourtApplicationProceedingsInitiated(envelope);
        final ArgumentCaptor<InitiateCourtApplicationEntity> initiateCourtApplicationEntityArgumentCaptor = ArgumentCaptor.forClass(InitiateCourtApplicationEntity.class);
        verify(initiateCourtApplicationRepository).save(initiateCourtApplicationEntityArgumentCaptor.capture());
    }

    @Test
    public void shouldProcessCourtApplicationProceedingsEdited() {

        final UUID applicationId = randomUUID();
        final CourtApplication courtApplication = CourtApplication.courtApplication().withId(applicationId).build();
        final CourtApplicationProceedingsEdited courtApplicationProceedingsEdited = CourtApplicationProceedingsEdited.courtApplicationProceedingsEdited().
                withCourtApplication(courtApplication).build();
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, CourtApplicationProceedingsEdited.class))
                .thenReturn(courtApplicationProceedingsEdited);
        final InitiateCourtApplicationEntity initiateCourtApplicationEntity = Mockito.mock(InitiateCourtApplicationEntity.class);
        when(initiateCourtApplicationRepository.findBy(applicationId)).thenReturn(initiateCourtApplicationEntity);
        when(envelope.metadata()).thenReturn(metadata);
        final JsonObject entityPayload = createObjectBuilder().build();
        when(stringToJsonObjectConverter.convert(initiateCourtApplicationEntity.getPayload())).thenReturn(entityPayload);
        when(jsonObjectToObjectConverter.convert(eq(entityPayload), eq(InitiateCourtApplicationProceedings.class))).thenReturn(Mockito.mock(InitiateCourtApplicationProceedings.class));
        when(objectToJsonObjectConverter.convert(any(InitiateCourtApplicationProceedings.class))).thenReturn(createObjectBuilder().build());

        when(repository.findByApplicationId(any())).thenReturn(courtApplicationEntity);
        when(objectToJsonObjectConverter.convert(any(CourtApplication.class))).thenReturn(jsonObject);

        eventListener.processCourtApplicationProceedingsEdited(envelope);
        final ArgumentCaptor<InitiateCourtApplicationEntity> initiateCourtApplicationEntityArgumentCaptor = ArgumentCaptor.forClass(InitiateCourtApplicationEntity.class);
        verify(initiateCourtApplicationRepository).save(initiateCourtApplicationEntityArgumentCaptor.capture());
        verify(repository).save(argumentCaptor.capture());

    }

    @Test
    public void shouldHandleApplicationEjectedEvent() {
        final UUID applicationId = fromString("f5decee0-27b5-4dc7-8c42-66dfbc6168d6");
        final UUID hearingId = UUID.randomUUID();
        final CourtApplicationEntity persistedEntity = new CourtApplicationEntity();
        persistedEntity.setApplicationId(applicationId);
        persistedEntity.setPayload(payload.toString());
        final InitiateCourtApplicationEntity initiateCourtApplicationEntity = new InitiateCourtApplicationEntity();
        initiateCourtApplicationEntity.setApplicationId(applicationId);
        initiateCourtApplicationEntity.setPayload(payload.toString());
        when(repository.findByApplicationId(applicationId)).thenReturn(persistedEntity);

        final CourtApplication courtApplication =
                CourtApplication.courtApplication()
                        .withId(applicationId)
                        .build();

        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings =
                InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings()
                        .withCourtApplication(courtApplication)
                        .build();

        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);

        final HearingApplicationEntity hearingApplicationEntity = new HearingApplicationEntity();
        hearingApplicationEntity.setId(new HearingApplicationKey(applicationId, hearingId));
        hearingApplicationEntity.setHearing(hearingEntity);
        final List<CourtApplication> courtApplicationList = new ArrayList<>();
        courtApplicationList.add(courtApplication);

        Hearing hearing = Hearing.hearing().withId(hearingId)
                .withCourtApplications(courtApplicationList).build();

        when(hearingApplicationRepository.findByApplicationId(applicationId)).thenReturn(Collections.singletonList(hearingApplicationEntity));
        when(initiateCourtApplicationRepository.findBy(applicationId)).thenReturn(initiateCourtApplicationEntity);
        when(stringToJsonObjectConverter.convert(payload.toString())).thenReturn(payload);
        when(envelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(jsonObjectToObjectConverter.convert(jsonObject, ApplicationEjected.class))
                .thenReturn(ApplicationEjected.applicationEjected()
                        .withApplicationId(applicationId)
                        .withRemovalReason("Legal")
                        .build());
        when(stringToJsonObjectConverter.convert(hearingEntity.getPayload())).thenReturn(jsonObject);
        when(jsonObjectToObjectConverter.convert(payload, CourtApplication.class)).thenReturn(courtApplication);
        when(jsonObjectToObjectConverter.convert(payload, InitiateCourtApplicationProceedings.class)).thenReturn(initiateCourtApplicationProceedings);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(payload);
        when(envelope.metadata()).thenReturn(metadata);
        when(jsonObjectToObjectConverter.convert(jsonObject, Hearing.class)).thenReturn(hearing);
        when(objectToJsonObjectConverter.convert(any(Hearing.class))).thenReturn(createObjectBuilder()
                .add("id", randomUUID().toString())
                .add(COURT_APPLICATIONS, createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("id", applicationId.toString())
                                .add(APPLICATION_STATUS, ApplicationStatus.EJECTED.name())))
                .build());


        eventListener.processCourtApplicationEjected(envelope);
        verify(repository).save(argumentCaptor.capture());
        verify(hearingRepository).save(hearingEntityArgumentCaptor.capture());
        final HearingEntity updatedHearingEntity = hearingEntityArgumentCaptor.getValue();
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
        final JsonNode hearingNode = mapper.valueToTree(JSONValue.parse(updatedHearingEntity.getPayload()));
        Assert.assertEquals("Check if the application status is ejected", ApplicationStatus.EJECTED.name(), hearingNode.path(COURT_APPLICATIONS).get(0).path(APPLICATION_STATUS).asText());

    }

    @Test
    public void shouldHandleCourtApplicationAddedToCaseEventWihCaseURN() {
        final UUID applicationId = randomUUID();
        final CourtApplicationEntity persistedEntity = new CourtApplicationEntity();
        persistedEntity.setApplicationId(applicationId);
        persistedEntity.setPayload(payload.toString());
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withCourtApplicationCases(
                        Collections.singletonList(
                                CourtApplicationCase.courtApplicationCase()
                                        .withProsecutionCaseId(randomUUID())
                                        .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                                                .withCaseURN("CaseURN")
                                                .build())
                                        .build()
                        )
                )
                .build();

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, CourtApplicationAddedToCase.class))
                .thenReturn(courtApplicationAddedToCase);
        when(envelope.metadata()).thenReturn(metadata);
        when(courtApplicationAddedToCase.getCourtApplication()).thenReturn(courtApplication);
        when(objectToJsonObjectConverter.convert(any(CourtApplication.class))).thenReturn(jsonObject);
        when(repository.findByApplicationId(applicationId)).thenReturn(persistedEntity);

        eventListener.processCourtApplicationAddedToCase(envelope);

        verify(courtApplicationCaseRepository).save(courtApplicationCaseArgumentCaptor.capture());
        final CourtApplicationCaseEntity courtApplicationCaseEntity = courtApplicationCaseArgumentCaptor.getValue();
        assertThat(courtApplicationCaseEntity.getCaseReference(), is("CaseURN"));
    }

    @Test
    public void shouldHandleCourtApplicationAddedToCaseEventWihCaseURNAndCourtOrderFromSameCase() {
        final UUID applicationId = randomUUID();
        final CourtApplicationEntity persistedEntity = new CourtApplicationEntity();
        persistedEntity.setApplicationId(applicationId);
        persistedEntity.setPayload(payload.toString());
        final UUID caseId = randomUUID();
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withCourtApplicationCases(
                        singletonList(
                                CourtApplicationCase.courtApplicationCase()
                                        .withProsecutionCaseId(caseId)
                                        .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                                                .withCaseURN("CaseURN")
                                                .build())


                                        .build()
                        )
                )
                .withCourtOrder(CourtOrder.courtOrder().withId(randomUUID())
                        .withOrderingCourt(CourtCentre.courtCentre().withId(randomUUID()).build())
                        .withCourtOrderOffences(singletonList(courtOrderOffence().withOffence(offence()
                                        .withJudicialResults(null)

                                        .build())
                                .withProsecutionCaseId(caseId)
                                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN("CaseURN").build())
                                .build()))
                        .build())
                .build();

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, CourtApplicationAddedToCase.class))
                .thenReturn(courtApplicationAddedToCase);
        when(envelope.metadata()).thenReturn(metadata);
        when(courtApplicationAddedToCase.getCourtApplication()).thenReturn(courtApplication);
        when(objectToJsonObjectConverter.convert(any(CourtApplication.class))).thenReturn(jsonObject);
        when(repository.findByApplicationId(applicationId)).thenReturn(persistedEntity);
        eventListener.processCourtApplicationAddedToCase(envelope);
        verify(courtApplicationCaseRepository, times(1)).save(courtApplicationCaseArgumentCaptor.capture());
        final CourtApplicationCaseEntity courtApplicationCaseEntity = courtApplicationCaseArgumentCaptor.getValue();
        assertThat(courtApplicationCaseEntity.getCaseReference(), is("CaseURN"));
    }

    @Test
    public void shouldHandleCourtApplicationAddedToCaseEventWithProAuthRef() {
        final UUID applicationId = randomUUID();
        final CourtApplicationEntity persistedEntity = new CourtApplicationEntity();
        persistedEntity.setApplicationId(applicationId);
        persistedEntity.setPayload(payload.toString());
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withCourtApplicationCases(
                        Collections.singletonList(
                                CourtApplicationCase.courtApplicationCase()
                                        .withProsecutionCaseId(randomUUID())
                                        .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                                                .withProsecutionAuthorityReference("ProAuthRef")
                                                .build())
                                        .build()
                        )
                )
                .build();

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, CourtApplicationAddedToCase.class))
                .thenReturn(courtApplicationAddedToCase);
        when(envelope.metadata()).thenReturn(metadata);
        when(courtApplicationAddedToCase.getCourtApplication()).thenReturn(courtApplication);
        when(objectToJsonObjectConverter.convert(any(CourtApplication.class))).thenReturn(jsonObject);
        when(repository.findByApplicationId(applicationId)).thenReturn(persistedEntity);

        eventListener.processCourtApplicationAddedToCase(envelope);

        verify(courtApplicationCaseRepository).save(courtApplicationCaseArgumentCaptor.capture());
        final CourtApplicationCaseEntity courtApplicationCaseEntity = courtApplicationCaseArgumentCaptor.getValue();
        assertThat(courtApplicationCaseEntity.getCaseReference(), is("ProAuthRef"));
    }


    @Test
    public void shouldHandleHearingResultedApplicationUpdated() {
        final UUID applicationId = randomUUID();
        final CourtApplicationEntity persistedEntity = new CourtApplicationEntity();
        persistedEntity.setApplicationId(applicationId);
        persistedEntity.setPayload(payload.toString());
        when(jsonObjectToObjectConverter.convert(payload, HearingResultedApplicationUpdated.class)).thenReturn(hearingResultedApplicationUpdated()
                .withCourtApplication(CourtApplication.courtApplication().withId(applicationId).build()).build());
        when(objectToJsonObjectConverter.convert(any(CourtApplication.class))).thenReturn(jsonObject);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(repository.findByApplicationId(applicationId)).thenReturn(persistedEntity);
        eventListener.processHearingResultedApplicationUpdated(envelope);
    }

    @Test
    public void shouldHandleHearingResultedApplicationUpdatedWithForNowsResults() {
        final List<JudicialResult> judicialResults = new ArrayList<>();
        judicialResults.add(JudicialResult.judicialResult()
                .withResultText(FOR_NOWS)
                .withPublishedForNows(true)
                .build());
        judicialResults.add(JudicialResult.judicialResult()
                .withResultText(FOR_NOT_NOWS)
                .build());

        final UUID applicationId = randomUUID();
        final CourtApplicationEntity persistedEntity = new CourtApplicationEntity();
        persistedEntity.setApplicationId(applicationId);
        persistedEntity.setPayload(payload.toString());
        when(jsonObjectToObjectConverter.convert(payload, HearingResultedApplicationUpdated.class)).thenReturn(hearingResultedApplicationUpdated()
                .withCourtApplication(CourtApplication.courtApplication()
                        .withApplicationStatus(ApplicationStatus.IN_PROGRESS)
                        .withId(applicationId)
                        .withApplicant(CourtApplicationParty.courtApplicationParty().build())
                        .withCourtApplicationCases(
                                singletonList(CourtApplicationCase.courtApplicationCase().withProsecutionCaseId(randomUUID())
                                        .withOffences(singletonList(offence()
                                                .withJudicialResults(judicialResults)
                                                .build()))
                                        .build()))
                        .withCourtOrder(CourtOrder.courtOrder().withId(randomUUID())
                                .withOrderingCourt(CourtCentre.courtCentre().withId(randomUUID()).build())
                                .withCourtOrderOffences(singletonList(courtOrderOffence().withOffence(offence()
                                        .withJudicialResults(judicialResults)
                                        .build())
                                        .build()))
                                .build())
                        .withRespondents(singletonList(CourtApplicationParty.courtApplicationParty().build()))
                        .withJudicialResults(judicialResults)
                        .build()).build());
        when(objectToJsonObjectConverter.convert(any(CourtApplication.class))).thenReturn(jsonObject);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(repository.findByApplicationId(applicationId)).thenReturn(persistedEntity);
        eventListener.processHearingResultedApplicationUpdated(envelope);
        verify(objectToJsonObjectConverter).convert(courtApplicationArgumentCaptor.capture());
        final CourtApplication courtApplication = courtApplicationArgumentCaptor.getValue();
        assertThat(courtApplication.getJudicialResults().size(), is(1));
        assertThat(courtApplication.getJudicialResults().get(0).getResultText(), is(FOR_NOT_NOWS));
        assertThat(courtApplication.getCourtApplicationCases().get(0).getOffences().get(0).getJudicialResults().size(), Matchers.is(1));
        assertThat(courtApplication.getCourtApplicationCases().get(0).getOffences().get(0).getJudicialResults().get(0).getResultText(), Matchers.is(FOR_NOT_NOWS));
        assertThat(courtApplication.getCourtOrder().getCourtOrderOffences().get(0).getOffence().getJudicialResults().size(), Matchers.is(1));
        assertThat(courtApplication.getCourtOrder().getCourtOrderOffences().get(0).getOffence().getJudicialResults().get(0).getResultText(), Matchers.is(FOR_NOT_NOWS));

    }

    @Test
    public void shouldHandleHearingResultedApplicationUpdatedWithForNowsResultsOnlyApplicationResults() {
        final List<JudicialResult> judicialResults = new ArrayList<>();
        judicialResults.add(JudicialResult.judicialResult()
                .withResultText(FOR_NOWS)
                .withPublishedForNows(true)
                .build());
        judicialResults.add(JudicialResult.judicialResult()
                .withResultText(FOR_NOT_NOWS)
                .build());

        final UUID applicationId = randomUUID();
        final CourtApplicationEntity persistedEntity = new CourtApplicationEntity();
        persistedEntity.setApplicationId(applicationId);
        persistedEntity.setPayload(payload.toString());
        when(jsonObjectToObjectConverter.convert(payload, HearingResultedApplicationUpdated.class)).thenReturn(hearingResultedApplicationUpdated()
                .withCourtApplication(CourtApplication.courtApplication()
                        .withApplicationStatus(ApplicationStatus.IN_PROGRESS)
                        .withId(applicationId)
                        .withApplicant(CourtApplicationParty.courtApplicationParty().build())
                        .withCourtApplicationCases(
                                singletonList(CourtApplicationCase.courtApplicationCase().withProsecutionCaseId(randomUUID())
                                        .withOffences(singletonList(offence()
                                                .build()))
                                        .build()))
                        .withCourtOrder(CourtOrder.courtOrder().withId(randomUUID())
                                .withOrderingCourt(CourtCentre.courtCentre().withId(randomUUID()).build())
                                .withCourtOrderOffences(singletonList(courtOrderOffence().withOffence(offence()
                                        .build())
                                        .build()))
                                .build())
                        .withRespondents(singletonList(CourtApplicationParty.courtApplicationParty().build()))
                        .withJudicialResults(judicialResults)
                        .build()).build());
        when(objectToJsonObjectConverter.convert(any(CourtApplication.class))).thenReturn(jsonObject);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(repository.findByApplicationId(applicationId)).thenReturn(persistedEntity);
        eventListener.processHearingResultedApplicationUpdated(envelope);
        verify(objectToJsonObjectConverter).convert(courtApplicationArgumentCaptor.capture());
        final CourtApplication courtApplication = courtApplicationArgumentCaptor.getValue();
        assertThat(courtApplication.getJudicialResults().size(), is(1));
        assertThat(courtApplication.getJudicialResults().get(0).getResultText(), is(FOR_NOT_NOWS));
        assertThat(courtApplication.getCourtApplicationCases().get(0).getOffences().get(0).getJudicialResults(), nullValue());
        assertThat(courtApplication.getCourtOrder().getCourtOrderOffences().get(0).getOffence().getJudicialResults(), nullValue());

    }

    @Test
    public void shouldNotUpdateDefendantAddressOnApplication_ApplicationNotExists(){
        final UUID applicationId = randomUUID();
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, DefendantAddressOnApplicationUpdated.class)).thenReturn(
                DefendantAddressOnApplicationUpdated.defendantAddressOnApplicationUpdated()
                        .withApplicationId(applicationId)
                                .withDefendant(DefendantUpdate.defendantUpdate().build()).build());
        when(repository.findByApplicationId(applicationId)).thenReturn(null);
        eventListener.processDefendantAddressOnApplicationUpdated(envelope);
        verify(repository, times(0)).save(any());
    }

    @Test
    public void shouldUpdateApplicantAddressOnApplication_PersonDefendant(){
        final UUID applicationId = randomUUID();
        final UUID masterDefendantId = randomUUID();
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, DefendantAddressOnApplicationUpdated.class)).thenReturn(
                DefendantAddressOnApplicationUpdated.defendantAddressOnApplicationUpdated()
                        .withApplicationId(applicationId)
                        .withDefendant(buildUpdatedDefendant(true, masterDefendantId)).build());
        final CourtApplicationEntity persistedEntity = new CourtApplicationEntity();
        persistedEntity.setApplicationId(applicationId);
        persistedEntity.setPayload(payload.toString());
        when(repository.findByApplicationId(applicationId)).thenReturn(persistedEntity);
        when(stringToJsonObjectConverter.convert(payload.toString())).thenReturn(payload);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, CourtApplication.class)).thenReturn(CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(courtApplicationType().withLinkType(LinkType.LINKED).build())
                .withApplicant(buildOriginalDefendant(true, masterDefendantId))
                .build());

        final InitiateCourtApplicationEntity initiateCourtApplicationEntity = Mockito.mock(InitiateCourtApplicationEntity.class);
        when(initiateCourtApplicationRepository.findBy(applicationId)).thenReturn(initiateCourtApplicationEntity);
        final JsonObject entityPayload = createObjectBuilder().build();
        when(stringToJsonObjectConverter.convert(initiateCourtApplicationEntity.getPayload())).thenReturn(entityPayload);
        when(jsonObjectToObjectConverter.convert(eq(entityPayload), eq(InitiateCourtApplicationProceedings.class))).thenReturn(Mockito.mock(InitiateCourtApplicationProceedings.class));
        when(objectToJsonObjectConverter.convert(any(InitiateCourtApplicationProceedings.class))).thenReturn(createObjectBuilder().build());

        eventListener.processDefendantAddressOnApplicationUpdated(envelope);
        verify(repository).save(argumentCaptor.capture());
        final ArgumentCaptor<InitiateCourtApplicationEntity> initiateCourtApplicationEntityArgumentCaptor = ArgumentCaptor.forClass(InitiateCourtApplicationEntity.class);
        verify(initiateCourtApplicationRepository).save(initiateCourtApplicationEntityArgumentCaptor.capture());
    }

    @Test
    public void shouldUpdateSubjectAddressOnApplication_PersonDefendant(){
        final UUID applicationId = randomUUID();
        final UUID masterDefendantId = randomUUID();
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, DefendantAddressOnApplicationUpdated.class)).thenReturn(
                DefendantAddressOnApplicationUpdated.defendantAddressOnApplicationUpdated()
                        .withApplicationId(applicationId)
                        .withDefendant(buildUpdatedDefendant(true, masterDefendantId)).build());
        final CourtApplicationEntity persistedEntity = new CourtApplicationEntity();
        persistedEntity.setApplicationId(applicationId);
        persistedEntity.setPayload(payload.toString());
        when(repository.findByApplicationId(applicationId)).thenReturn(persistedEntity);
        when(stringToJsonObjectConverter.convert(payload.toString())).thenReturn(payload);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, CourtApplication.class)).thenReturn(CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(courtApplicationType().withLinkType(LinkType.LINKED).build())
                .withSubject(buildOriginalDefendant(true, masterDefendantId))
                .build());
        final InitiateCourtApplicationEntity initiateCourtApplicationEntity = Mockito.mock(InitiateCourtApplicationEntity.class);
        when(initiateCourtApplicationRepository.findBy(applicationId)).thenReturn(initiateCourtApplicationEntity);
        final JsonObject entityPayload = createObjectBuilder().build();
        when(stringToJsonObjectConverter.convert(initiateCourtApplicationEntity.getPayload())).thenReturn(entityPayload);
        when(jsonObjectToObjectConverter.convert(eq(entityPayload), eq(InitiateCourtApplicationProceedings.class))).thenReturn(Mockito.mock(InitiateCourtApplicationProceedings.class));
        when(objectToJsonObjectConverter.convert(any(InitiateCourtApplicationProceedings.class))).thenReturn(createObjectBuilder().build());

        eventListener.processDefendantAddressOnApplicationUpdated(envelope);
        verify(repository).save(argumentCaptor.capture());
        final ArgumentCaptor<InitiateCourtApplicationEntity> initiateCourtApplicationEntityArgumentCaptor = ArgumentCaptor.forClass(InitiateCourtApplicationEntity.class);
        verify(initiateCourtApplicationRepository).save(initiateCourtApplicationEntityArgumentCaptor.capture());
    }

    @Test
    public void shouldUpdateRespondentsAddressOnApplication_LegalEntityDefendant(){
        final UUID applicationId = randomUUID();
        final UUID masterDefendantId = randomUUID();
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, DefendantAddressOnApplicationUpdated.class)).thenReturn(
                DefendantAddressOnApplicationUpdated.defendantAddressOnApplicationUpdated()
                        .withApplicationId(applicationId)
                        .withDefendant(buildUpdatedDefendant(false, masterDefendantId)).build());
        final CourtApplicationEntity persistedEntity = new CourtApplicationEntity();
        persistedEntity.setApplicationId(applicationId);
        persistedEntity.setPayload(payload.toString());
        when(repository.findByApplicationId(applicationId)).thenReturn(persistedEntity);
        when(stringToJsonObjectConverter.convert(payload.toString())).thenReturn(payload);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, CourtApplication.class)).thenReturn(CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(courtApplicationType().withLinkType(LinkType.LINKED).build())
                .withSubject(buildOriginalDefendant(true, randomUUID()))
                .withRespondents(Arrays.asList(buildOriginalDefendant(false, masterDefendantId)))
                .build());
        final InitiateCourtApplicationEntity initiateCourtApplicationEntity = Mockito.mock(InitiateCourtApplicationEntity.class);
        when(initiateCourtApplicationRepository.findBy(applicationId)).thenReturn(initiateCourtApplicationEntity);
        final JsonObject entityPayload = createObjectBuilder().build();
        when(stringToJsonObjectConverter.convert(initiateCourtApplicationEntity.getPayload())).thenReturn(entityPayload);
        when(jsonObjectToObjectConverter.convert(eq(entityPayload), eq(InitiateCourtApplicationProceedings.class))).thenReturn(Mockito.mock(InitiateCourtApplicationProceedings.class));
        when(objectToJsonObjectConverter.convert(any(InitiateCourtApplicationProceedings.class))).thenReturn(createObjectBuilder().build());

        eventListener.processDefendantAddressOnApplicationUpdated(envelope);
        verify(repository).save(argumentCaptor.capture());
        final ArgumentCaptor<InitiateCourtApplicationEntity> initiateCourtApplicationEntityArgumentCaptor = ArgumentCaptor.forClass(InitiateCourtApplicationEntity.class);
        verify(initiateCourtApplicationRepository).save(initiateCourtApplicationEntityArgumentCaptor.capture());
    }

    private static CourtApplicationParty buildOriginalDefendant(boolean isPersonDefendant, final UUID masterDefendantId) {
        final Address originalAddress = Address.address().withAddress1("Old Address 1").withAddress2("Old Address 2").withPostcode("RG2 1WE").build();
        if(isPersonDefendant) {
            return CourtApplicationParty.courtApplicationParty()
                    .withMasterDefendant(MasterDefendant.masterDefendant()
                            .withMasterDefendantId(masterDefendantId)
                            .withPersonDefendant(PersonDefendant.personDefendant()
                                    .withPersonDetails(Person.person()
                                            .withAddress(originalAddress)
                                            .build())
                                    .build())
                            .build())
                    .build();
        }else{
            return CourtApplicationParty.courtApplicationParty()
                    .withMasterDefendant(MasterDefendant.masterDefendant()
                            .withMasterDefendantId(masterDefendantId)
                            .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                                    .withOrganisation(Organisation.organisation()
                                            .withAddress(originalAddress)
                                            .build())
                                    .build())
                            .build())
                    .build();
        }
    }

    private static DefendantUpdate buildUpdatedDefendant(boolean isPersonDefendant, final UUID masterDefendantId) {
        final Address updatedAddress = Address.address().withAddress1("New Address 1").withAddress2("New Address 2").withPostcode("RG1 2PQ").build();
        if(isPersonDefendant){
            return DefendantUpdate.defendantUpdate()
                    .withId(randomUUID())
                    .withMasterDefendantId(masterDefendantId)
                    .withPersonDefendant(PersonDefendant.personDefendant()
                            .withPersonDetails(Person.person()
                                    .withAddress(updatedAddress)
                                    .build())
                            .build())
                    .build();
        }else{
            return DefendantUpdate.defendantUpdate()
                    .withId(randomUUID())
                    .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                            .withOrganisation(Organisation.organisation()
                                    .withAddress(updatedAddress)
                                    .build())
                            .build())
                    .build();
        }
    }

}
