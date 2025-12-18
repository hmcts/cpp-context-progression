package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CaseEjected;
import uk.gov.justice.core.courts.CaseEjectedViaBdf;
import uk.gov.justice.core.courts.CaseNoteAdded;
import uk.gov.justice.core.courts.CaseNoteAddedV2;
import uk.gov.justice.core.courts.CaseNoteEdited;
import uk.gov.justice.core.courts.CaseNoteEditedV2;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.InitiateCourtApplicationProceedings;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseCreated;
import uk.gov.justice.progression.courts.CaseInsertedBdf;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.application.event.listener.CourtApplicationEventListenerTest;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseNoteEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationCaseKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.InitiateCourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseNoteRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.InitiateCourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.mapping.SearchProsecutionCase;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.minidev.json.JSONValue;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionCaseEventListenerTest {

    private static final String APPLICATION_STATUS = "applicationStatus";
    private static final String PROSECUTION_CASES = "prosecutionCases";
    private static final String CASE_STATUS = "caseStatus";
    private static final String CASE_STATUS_EJECTED = "EJECTED";
    private static final String CPS_ORGANISATION = "cpsOrganisation";
    private static final String CPS_ORGANISATION_VALUE = "A01";
    private static final String TRIAL_RECEIPT_TYPE = "trialReceiptType";

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Mock
    private StringToJsonObjectConverter stringToJsonObjectConverter;
    @Mock
    private ProsecutionCaseRepository repository;
    @Mock
    private CaseNoteRepository caseNoteRepository;
    @Mock
    private CaseDefendantHearingRepository caseDefendantHearingRepository;
    @Mock
    private CourtApplicationCaseRepository courtApplicationCaseRepository;
    @Mock
    private HearingRepository hearingRepository;
    @Mock
    private JsonEnvelope envelope;
    @Mock
    private JsonObject jsonObject;
    @Mock
    private ProsecutionCaseCreated prosecutionCaseCreated;
    @Mock
    private CaseInsertedBdf caseInsertedBdf;
    @Mock
    private CaseEjected caseEjected;
    @Mock
    private CaseEjectedViaBdf caseEjectedViaBdf;
    @Mock
    private ProsecutionCase prosecutionCase;
    @Mock
    private InitiateCourtApplicationRepository initiateCourtApplicationRepository;
    @Mock
    private Defendant defendant;
    @Captor
    private ArgumentCaptor<ProsecutionCaseEntity> argumentCaptor;
    @Captor
    private ArgumentCaptor<HearingEntity> hearingEntityArgumentCaptor;
    @Captor
    private ArgumentCaptor<CourtApplicationCaseEntity> courtApplicationCaseEntityArgumentCaptor;
    @Captor
    private ArgumentCaptor<CaseNoteEntity> caseNoteArgumentCaptor;
    @Mock
    private JsonObject payload;
    @Mock
    private Metadata metadata;
    @InjectMocks
    private ProsecutionCaseEventListener eventListener;
    @Spy
    private ListToJsonArrayConverter jsonConverter;
    @Mock
    private SearchProsecutionCase searchCase;

    @BeforeEach
    public void initMocks() {

        setField(this.jsonConverter, "mapper",
                new ObjectMapperProducer().objectMapper());
        setField(this.jsonConverter, "stringToJsonObjectConverter",
                new StringToJsonObjectConverter());
    }

    @Test
    public void shouldHandleProsecutionCaseCreatedEvent() {

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, ProsecutionCaseCreated.class))
                .thenReturn(prosecutionCaseCreated);
        when(prosecutionCase.getId()).thenReturn(randomUUID());
        when(prosecutionCase.getDefendants()).thenReturn(singletonList(defendant));
        when(defendant.getPersonDefendant()).thenReturn(PersonDefendant.personDefendant().build());
        when(prosecutionCaseCreated.getProsecutionCase()).thenReturn(prosecutionCase);
        when(objectToJsonObjectConverter.convert(any(ProsecutionCase.class))).thenReturn(jsonObject);
        eventListener.processProsecutionCaseCreated(envelope);
        verify(repository).save(argumentCaptor.capture());
    }

    @Test
    public void shouldHandleProsecutionCaseInsertedEvent() {

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, CaseInsertedBdf.class))
                .thenReturn(caseInsertedBdf);
        when(caseInsertedBdf.getProsecutionCase()).thenReturn(prosecutionCase);
        when(repository.findOptionalByCaseId(any())).thenReturn(null);
        when(prosecutionCase.getId()).thenReturn(randomUUID());
        when(objectToJsonObjectConverter.convert(any(ProsecutionCase.class))).thenReturn(jsonObject);
        eventListener.prosecutionCaseInserted(envelope);
        verify(repository).save(argumentCaptor.capture());
    }

    @Test
    public void shouldNotHandleProsecutionCaseInsertedEventIfCaseIsThere() {

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, CaseInsertedBdf.class))
                .thenReturn(caseInsertedBdf);
        when(caseInsertedBdf.getProsecutionCase()).thenReturn(prosecutionCase);
        when(repository.findOptionalByCaseId(any())).thenReturn(new ProsecutionCaseEntity());
        when(prosecutionCase.getId()).thenReturn(randomUUID());
        eventListener.prosecutionCaseInserted(envelope);
        verify(repository, never()).save(argumentCaptor.capture());
    }

    @Test
    public void shouldHandleCaseEjectedEvent() throws IOException {
        final UUID caseId = fromString("b46ddab2-3e9d-4c8c-b9ea-386a6b93d23f");
        final UUID caseId2 = randomUUID();
        final UUID applicationId = fromString("f5decee0-27b5-4dc7-8c42-66dfbc6168d6");
        final UUID hearingId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();

        final ProsecutionCaseEntity persistedEntity = new ProsecutionCaseEntity();
        persistedEntity.setCaseId(caseId);
        persistedEntity.setPayload(payload.toString());
        when(repository.findByCaseId(caseId)).thenReturn(persistedEntity);

        final InitiateCourtApplicationEntity initiateCourtApplicationEntity = new InitiateCourtApplicationEntity();
        initiateCourtApplicationEntity.setApplicationId(applicationId);
        initiateCourtApplicationEntity.setPayload(payload.toString());

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().withId(caseId).build();
        final ProsecutionCase prosecutionCase2 = ProsecutionCase.prosecutionCase().withId(caseId2).build();

        final CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setPayload(createPayload("/json/courtApplicationData.json"));

        final CourtApplicationCaseEntity courtApplicationCaseEntity = new CourtApplicationCaseEntity();
        courtApplicationCaseEntity.setId(new CourtApplicationCaseKey(randomUUID(), randomUUID(), caseId));
        courtApplicationCaseEntity.setCourtApplication(courtApplicationEntity);

        when(courtApplicationCaseRepository.findByCaseId(caseId)).thenReturn(singletonList(courtApplicationCaseEntity));
        when(stringToJsonObjectConverter.convert(courtApplicationEntity.getPayload())).thenReturn(jsonObject);
        final CourtApplication courtApplication =
                CourtApplication.courtApplication()
                        .withCourtApplicationCases(
                                singletonList(CourtApplicationCase.courtApplicationCase().withProsecutionCaseId(randomUUID()).build()))
                        .withId(UUID.randomUUID())
                        .build();
        when(jsonObjectToObjectConverter.convert(jsonObject, CourtApplication.class)).thenReturn(courtApplication);

        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings =
                InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings()
                        .withCourtApplication(courtApplication)
                        .build();

        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(createPayload("/json/hearingDataProsecutionCase.json"));
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);

        final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setId(new CaseDefendantHearingKey(caseId, defendantId, hearingId));
        caseDefendantHearingEntity.setHearing(hearingEntity);

        List<ProsecutionCase> prosecutionCaseList = new ArrayList<>();
        prosecutionCaseList.add(prosecutionCase);
        prosecutionCaseList.add(prosecutionCase2);

        Hearing hearing = Hearing.hearing().withId(UUID.randomUUID())
                .withProsecutionCases(prosecutionCaseList)
                .withCourtApplications(singletonList(courtApplication)).build();

        when(stringToJsonObjectConverter.convert(hearingEntity.getPayload())).thenReturn(jsonObject);

        when(jsonObjectToObjectConverter.convert(jsonObject, Hearing.class)).thenReturn(hearing);

        when(caseDefendantHearingRepository.findByCaseId(caseId)).thenReturn(singletonList(caseDefendantHearingEntity));
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, CaseEjected.class))
                .thenReturn(caseEjected);
        when(jsonObjectToObjectConverter.convert(jsonObject, ProsecutionCase.class))
                .thenReturn(prosecutionCase);
        when(caseEjected.getProsecutionCaseId()).thenReturn(caseId);
        when(caseEjected.getRemovalReason()).thenReturn("Legal");
        when(objectToJsonObjectConverter.convert(any())).thenReturn(jsonObject);
        when(stringToJsonObjectConverter.convert(payload.toString())).thenReturn(jsonObject);
        when(initiateCourtApplicationRepository.findBy(any())).thenReturn(initiateCourtApplicationEntity);
        when(jsonObjectToObjectConverter.convert(jsonObject, InitiateCourtApplicationProceedings.class)).thenReturn(initiateCourtApplicationProceedings);

        when(objectToJsonObjectConverter.convert(any())).thenReturn(JsonObjects.createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("prosecutionCases", JsonObjects.createArrayBuilder()
                        .add(JsonObjects.createObjectBuilder()
                                .add("id", randomUUID().toString())
                                .add(CPS_ORGANISATION, CPS_ORGANISATION_VALUE)
                                .add(TRIAL_RECEIPT_TYPE, "Transfer")
                                .add("caseStatus", CASE_STATUS_EJECTED)))
                .add("linkedCaseId", caseId.toString())
                .add(APPLICATION_STATUS, ApplicationStatus.EJECTED.name()).build());

        eventListener.processProsecutionCaseEjected(envelope);

        verify(repository).save(argumentCaptor.capture());
        verify(courtApplicationCaseRepository).save(courtApplicationCaseEntityArgumentCaptor.capture());
        final CourtApplicationCaseEntity updatedCourtApplicationEntity = courtApplicationCaseEntityArgumentCaptor.getValue();
        final JsonNode courtApplicationNode = mapper.valueToTree(JSONValue.parse(updatedCourtApplicationEntity.getCourtApplication().getPayload()));
        assertThat(courtApplicationNode.path(APPLICATION_STATUS).asText(), is(ApplicationStatus.EJECTED.name()));

        verify(hearingRepository).save(hearingEntityArgumentCaptor.capture());
        final HearingEntity updatedHearingEntity = hearingEntityArgumentCaptor.getValue();

        final JsonNode hearingNode = mapper.valueToTree(JSONValue.parse(updatedHearingEntity.getPayload()));
        assertThat(hearingNode.path(PROSECUTION_CASES).get(0).path(CASE_STATUS).asText(), is(CASE_STATUS_EJECTED));
        assertThat(hearingNode.path(PROSECUTION_CASES).get(0).path(CPS_ORGANISATION).asText(), is(CPS_ORGANISATION_VALUE));
        assertThat(hearingNode.path(PROSECUTION_CASES).get(0).path(TRIAL_RECEIPT_TYPE).asText(), is("Transfer"));
    }

    @Test
    public void shouldHandleCaseEjectedViaBdfEvent() throws IOException {
        final UUID caseId = fromString("b46ddab2-3e9d-4c8c-b9ea-386a6b93d23f");
        final UUID caseId2 = randomUUID();
        final UUID applicationId = fromString("f5decee0-27b5-4dc7-8c42-66dfbc6168d6");
        final UUID hearingId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();

        final ProsecutionCaseEntity persistedEntity = new ProsecutionCaseEntity();
        persistedEntity.setCaseId(caseId);
        persistedEntity.setPayload(payload.toString());
        when(repository.findByCaseId(caseId)).thenReturn(persistedEntity);

        final InitiateCourtApplicationEntity initiateCourtApplicationEntity = new InitiateCourtApplicationEntity();
        initiateCourtApplicationEntity.setApplicationId(applicationId);
        initiateCourtApplicationEntity.setPayload(payload.toString());

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().withId(caseId).build();
        final ProsecutionCase prosecutionCase2 = ProsecutionCase.prosecutionCase().withId(caseId2).build();

        final CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setPayload(createPayload("/json/courtApplicationData.json"));

        final CourtApplicationCaseEntity courtApplicationCaseEntity = new CourtApplicationCaseEntity();
        courtApplicationCaseEntity.setId(new CourtApplicationCaseKey(randomUUID(), randomUUID(), caseId));
        courtApplicationCaseEntity.setCourtApplication(courtApplicationEntity);

        when(courtApplicationCaseRepository.findByCaseId(caseId)).thenReturn(singletonList(courtApplicationCaseEntity));
        when(stringToJsonObjectConverter.convert(courtApplicationEntity.getPayload())).thenReturn(jsonObject);
        final CourtApplication courtApplication =
                CourtApplication.courtApplication()
                        .withCourtApplicationCases(
                                singletonList(CourtApplicationCase.courtApplicationCase().withProsecutionCaseId(randomUUID()).build()))
                        .withId(UUID.randomUUID())
                        .build();
        when(jsonObjectToObjectConverter.convert(jsonObject, CourtApplication.class)).thenReturn(courtApplication);

        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings =
                InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings()
                        .withCourtApplication(courtApplication)
                        .build();

        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(createPayload("/json/hearingDataProsecutionCase.json"));
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);

        final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setId(new CaseDefendantHearingKey(caseId, defendantId, hearingId));
        caseDefendantHearingEntity.setHearing(hearingEntity);

        List<ProsecutionCase> prosecutionCaseList = new ArrayList<>();
        prosecutionCaseList.add(prosecutionCase);
        prosecutionCaseList.add(prosecutionCase2);

        Hearing hearing = Hearing.hearing().withId(UUID.randomUUID())
                .withProsecutionCases(prosecutionCaseList)
                .withCourtApplications(singletonList(courtApplication)).build();

        when(stringToJsonObjectConverter.convert(hearingEntity.getPayload())).thenReturn(jsonObject);

        when(jsonObjectToObjectConverter.convert(jsonObject, Hearing.class)).thenReturn(hearing);

        when(caseDefendantHearingRepository.findByCaseId(caseId)).thenReturn(singletonList(caseDefendantHearingEntity));
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, CaseEjectedViaBdf.class))
                .thenReturn(caseEjectedViaBdf);
        when(jsonObjectToObjectConverter.convert(jsonObject, ProsecutionCase.class))
                .thenReturn(prosecutionCase);
        when(caseEjectedViaBdf.getProsecutionCaseId()).thenReturn(caseId);
        when(caseEjectedViaBdf.getRemovalReason()).thenReturn("Legal");
        when(objectToJsonObjectConverter.convert(any())).thenReturn(jsonObject);
        when(stringToJsonObjectConverter.convert(payload.toString())).thenReturn(jsonObject);
        when(initiateCourtApplicationRepository.findBy(any())).thenReturn(initiateCourtApplicationEntity);
        when(jsonObjectToObjectConverter.convert(jsonObject, InitiateCourtApplicationProceedings.class)).thenReturn(initiateCourtApplicationProceedings);

        when(objectToJsonObjectConverter.convert(any())).thenReturn(JsonObjects.createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("prosecutionCases", JsonObjects.createArrayBuilder()
                        .add(JsonObjects.createObjectBuilder()
                                .add("id", randomUUID().toString())
                                .add(CPS_ORGANISATION, CPS_ORGANISATION_VALUE)
                                .add(TRIAL_RECEIPT_TYPE, "Transfer")
                                .add("caseStatus", CASE_STATUS_EJECTED)))
                .add("linkedCaseId", caseId.toString())
                .add(APPLICATION_STATUS, ApplicationStatus.EJECTED.name()).build());

        eventListener.processProsecutionCaseEjectedViaBDF(envelope);

        verify(repository).save(argumentCaptor.capture());
        verify(courtApplicationCaseRepository).save(courtApplicationCaseEntityArgumentCaptor.capture());
        final CourtApplicationCaseEntity updatedCourtApplicationEntity = courtApplicationCaseEntityArgumentCaptor.getValue();
        final JsonNode courtApplicationNode = mapper.valueToTree(JSONValue.parse(updatedCourtApplicationEntity.getCourtApplication().getPayload()));
        assertThat(courtApplicationNode.path(APPLICATION_STATUS).asText(), is(ApplicationStatus.EJECTED.name()));

        verify(hearingRepository).save(hearingEntityArgumentCaptor.capture());
        final HearingEntity updatedHearingEntity = hearingEntityArgumentCaptor.getValue();

        final JsonNode hearingNode = mapper.valueToTree(JSONValue.parse(updatedHearingEntity.getPayload()));
        assertThat(hearingNode.path(PROSECUTION_CASES).get(0).path(CASE_STATUS).asText(), is(CASE_STATUS_EJECTED));
        assertThat(hearingNode.path(PROSECUTION_CASES).get(0).path(CPS_ORGANISATION).asText(), is(CPS_ORGANISATION_VALUE));
        assertThat(hearingNode.path(PROSECUTION_CASES).get(0).path(TRIAL_RECEIPT_TYPE).asText(), is("Transfer"));
    }

    @Test
    public void shouldHandleCaseNoteAddedEvent() {

        //Given
        final CaseNoteAdded caseNoteAdded = createCaseNoteAddedEvent();

        //When
        eventListener.caseNoteAdded(envelope);

        //Then
        verifyCaseNoteAddedEventResults(caseNoteAdded);
    }

    @Test
    public void shouldHandleCaseNoteAddedV2Event() {

        //Given
        final CaseNoteAddedV2 caseNoteAddedV2 = createCaseNoteAddedEventV2();

        //When
        eventListener.caseNoteAddedV2(envelope);

        //Then
        verifyCaseNoteAddedV2EventResults(caseNoteAddedV2);
    }

    @Test
    public void shouldHandleCaseNoteEditedEvent() {

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        //Given
        final CaseNoteEntity caseNoteEntity = new CaseNoteEntity(UUID.randomUUID(), prosecutionCase.getId(), "note", "firstName", "lastName", ZonedDateTime.now(), false);
        final CaseNoteEdited caseNoteEdited = buildCaseNoteEdited(caseNoteEntity.getId());
        when(jsonObjectToObjectConverter.convert(payload, CaseNoteEdited.class)).thenReturn(caseNoteEdited);
        when(caseNoteRepository.findBy(caseNoteEntity.getId())).thenReturn(caseNoteEntity);
        //When
        eventListener.caseNoteEdited(envelope);
        //Then
        verifyCaseNoteEditedEventResults(caseNoteEdited);
    }

    @Test
    public void shouldHandleCaseNoteEditedEventV2() {

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        //Given
        final CaseNoteEntity caseNoteEntity = new CaseNoteEntity(UUID.randomUUID(), prosecutionCase.getId(), "note", "firstName", "lastName", ZonedDateTime.now(), false);
        final CaseNoteEditedV2 caseNoteEdited = buildCaseNoteEditedV2(caseNoteEntity.getId());
        when(jsonObjectToObjectConverter.convert(payload, CaseNoteEditedV2.class)).thenReturn(caseNoteEdited);
        when(caseNoteRepository.findBy(caseNoteEntity.getId())).thenReturn(caseNoteEntity);
        //When
        eventListener.caseNoteEditedV2(envelope);
        //Then
        verifyCaseNoteEditedEventResultsV2(caseNoteEdited);
    }

    private void verifyCaseNoteAddedEventResults(final CaseNoteAdded caseNoteAdded) {
        verify(caseNoteRepository).save(caseNoteArgumentCaptor.capture());

        final CaseNoteEntity caseNoteEntity = caseNoteArgumentCaptor.getValue();
        assertThat(caseNoteEntity.getCaseId(), equalTo(caseNoteAdded.getCaseId()));
        assertThat(caseNoteEntity.getNote(), equalTo(caseNoteAdded.getNote()));
        assertThat(caseNoteEntity.getCreatedDateTime(), equalTo(caseNoteAdded.getCreatedDateTime()));
        assertThat(caseNoteEntity.getFirstName(), equalTo(caseNoteAdded.getFirstName()));
        assertThat(caseNoteEntity.getLastName(), equalTo(caseNoteAdded.getLastName()));
    }

    private void verifyCaseNoteAddedV2EventResults(final CaseNoteAddedV2 caseNoteAdded) {
        verify(caseNoteRepository).save(caseNoteArgumentCaptor.capture());

        final CaseNoteEntity caseNoteEntity = caseNoteArgumentCaptor.getValue();
        assertThat(caseNoteEntity.getId(), equalTo(caseNoteAdded.getCaseNoteId()));
        assertThat(caseNoteEntity.getCaseId(), equalTo(caseNoteAdded.getCaseId()));
        assertThat(caseNoteEntity.getNote(), equalTo(caseNoteAdded.getNote()));
        assertThat(caseNoteEntity.getCreatedDateTime(), equalTo(caseNoteAdded.getCreatedDateTime()));
        assertThat(caseNoteEntity.getFirstName(), equalTo(caseNoteAdded.getFirstName()));
        assertThat(caseNoteEntity.getLastName(), equalTo(caseNoteAdded.getLastName()));
    }

    private void verifyCaseNoteEditedEventResults(final CaseNoteEdited caseNoteEdited) {
        verify(caseNoteRepository).save(caseNoteArgumentCaptor.capture());

        final CaseNoteEntity caseNoteEntity = caseNoteArgumentCaptor.getValue();
        assertThat(caseNoteEntity.getCaseId(), equalTo(caseNoteEdited.getCaseId()));
        assertThat(caseNoteEntity.getId(), equalTo(caseNoteEdited.getCaseNoteId()));
        assertThat(caseNoteEntity.getPinned(), equalTo(caseNoteEdited.getIsPinned()));
    }

    private void verifyCaseNoteEditedEventResultsV2(final CaseNoteEditedV2 caseNoteEditedV2) {
        verify(caseNoteRepository).save(caseNoteArgumentCaptor.capture());

        final CaseNoteEntity caseNoteEntity = caseNoteArgumentCaptor.getValue();
        assertThat(caseNoteEntity.getCaseId(), equalTo(caseNoteEditedV2.getCaseId()));
        assertThat(caseNoteEntity.getId(), equalTo(caseNoteEditedV2.getCaseNoteId()));
        assertThat(caseNoteEntity.getPinned(), equalTo(caseNoteEditedV2.getIsPinned()));
    }

    private CaseNoteAdded createCaseNoteAddedEvent() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        final CaseNoteAdded caseNoteAdded = CaseNoteAdded.caseNoteAdded()
                .withCaseId(prosecutionCase.getId())
                .withNote("Note")
                .withFirstName("firstName")
                .withLastName("lastName")
                .withCreatedDateTime(ZonedDateTime.now())
                .build();
        when(jsonObjectToObjectConverter.convert(payload, CaseNoteAdded.class))
                .thenReturn(caseNoteAdded);
        return caseNoteAdded;
    }

    private CaseNoteAddedV2 createCaseNoteAddedEventV2() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        final CaseNoteAddedV2 caseNoteAdded = CaseNoteAddedV2.caseNoteAddedV2()
                .withCaseNoteId(UUID.randomUUID())
                .withCaseId(prosecutionCase.getId())
                .withNote("Note")
                .withFirstName("firstName")
                .withLastName("lastName")
                .withCreatedDateTime(ZonedDateTime.now())
                .build();
        when(jsonObjectToObjectConverter.convert(payload, CaseNoteAddedV2.class))
                .thenReturn(caseNoteAdded);
        return caseNoteAdded;
    }

    private CaseNoteEdited buildCaseNoteEdited(final UUID caseNoteId) {
        return CaseNoteEdited.caseNoteEdited()
                .withCaseId(prosecutionCase.getId())
                .withCaseNoteId(caseNoteId)
                .withIsPinned(true)
                .build();
    }

    private CaseNoteEditedV2 buildCaseNoteEditedV2(final UUID caseNoteId) {
        return CaseNoteEditedV2.caseNoteEditedV2()
                .withCaseId(prosecutionCase.getId())
                .withCaseNoteId(caseNoteId)
                .withIsPinned(true)
                .build();
    }

    private String createPayload(final String payloadPath) throws IOException {
        final StringWriter writer = new StringWriter();
        InputStream inputStream = CourtApplicationEventListenerTest.class.getResourceAsStream(payloadPath);
        IOUtils.copy(inputStream, writer, UTF_8);
        inputStream.close();
        return writer.toString();

    }
}
