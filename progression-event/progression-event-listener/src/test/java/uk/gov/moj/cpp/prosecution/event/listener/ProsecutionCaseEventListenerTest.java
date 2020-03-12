package uk.gov.moj.cpp.prosecution.event.listener;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CaseEjected;
import uk.gov.justice.core.courts.CaseNoteAdded;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseCreated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.application.event.listener.CourtApplicationEventListenerTest;
import uk.gov.moj.cpp.prosecutioncase.event.listener.ProsecutionCaseEventListener;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseNoteEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseNoteRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.mapping.SearchProsecutionCase;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.minidev.json.JSONValue;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class ProsecutionCaseEventListenerTest {

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
    private CourtApplicationRepository courtApplicationRepository;

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObject jsonObject;

    @Mock
    private ProsecutionCaseCreated prosecutionCaseCreated;

    @Mock
    private CaseEjected caseEjected;

    @Mock
    private ProsecutionCase prosecutionCase;

    @Mock
    private Defendant defendant;

    @Captor
    private ArgumentCaptor<ProsecutionCaseEntity> argumentCaptor;

    @Captor
    private ArgumentCaptor<HearingEntity> hearingEntityArgumentCaptor;

    @Captor
    private ArgumentCaptor<CourtApplicationEntity> courtApplicationEntityArgumentCaptor;

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

    private static final String APPLICATION_STATUS = "applicationStatus";
    private static final String PROSECUTION_CASES = "prosecutionCases";
    private static final String CASE_STATUS = "caseStatus";
    private static final String CASE_STATUS_EJECTED = "EJECTED";




    @Before
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
        when(envelope.metadata()).thenReturn(metadata);
        when(prosecutionCase.getId()).thenReturn(randomUUID());
        when(prosecutionCase.getDefendants()).thenReturn(singletonList(defendant));
        when(prosecutionCaseCreated.getProsecutionCase()).thenReturn(prosecutionCase);
        when(objectToJsonObjectConverter.convert(prosecutionCase)).thenReturn(jsonObject);
        eventListener.processProsecutionCaseCreated(envelope);
        verify(repository).save(argumentCaptor.capture());
    }

    @Test
    public void shouldHandleCaseEjectedEvent() throws IOException {
        final UUID caseId = fromString("b46ddab2-3e9d-4c8c-b9ea-386a6b93d23f");
        final UUID hearingId = UUID.randomUUID();
        final UUID defendentId = UUID.randomUUID();
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();

        final ProsecutionCaseEntity persistedEntity = new ProsecutionCaseEntity();
        persistedEntity.setCaseId(caseId);
        persistedEntity.setPayload(payload.toString());
        when(repository.findByCaseId(caseId)).thenReturn(persistedEntity);

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().withId(caseId).build();

        final CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntity.setLinkedCaseId(caseId);
        courtApplicationEntity.setPayload(createPayload("/json/courtApplicationData.json"));

        when(courtApplicationRepository.findByLinkedCaseId(caseId)). thenReturn(singletonList(courtApplicationEntity));
        when(stringToJsonObjectConverter.convert(courtApplicationEntity.getPayload().toString())).thenReturn(jsonObject);
        final CourtApplication courtApplication =
                CourtApplication.courtApplication().withLinkedCaseId(caseId)
                        .withId(UUID.randomUUID())
                        .build();
        when(jsonObjectToObjectConverter.convert(jsonObject, CourtApplication.class)).thenReturn(courtApplication);

        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(createPayload("/json/hearingDataProsecutionCase.json"));
        hearingEntity.setListingStatus(HearingListingStatus.HEARING_INITIALISED);

        final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setId(new CaseDefendantHearingKey(caseId, defendentId, hearingId));
        caseDefendantHearingEntity.setHearing(hearingEntity);

        List<ProsecutionCase> prosecutionCaseList = new ArrayList<>();
        prosecutionCaseList.add(prosecutionCase);

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
        when(envelope.metadata()).thenReturn(metadata);
        when(caseEjected.getProsecutionCaseId()).thenReturn(caseId);
        when(caseEjected.getRemovalReason()).thenReturn("Legal");
        when(objectToJsonObjectConverter.convert(any(ProsecutionCase.class))).thenReturn(jsonObject);
        when(stringToJsonObjectConverter.convert(payload.toString())).thenReturn(jsonObject);


        when(objectToJsonObjectConverter.convert(any(Hearing.class))).thenReturn(Json.createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("prosecutionCases", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("id", randomUUID().toString())
                                .add("caseStatus", CASE_STATUS_EJECTED)))
                .add("linkedCaseId", caseId.toString())
                .add(APPLICATION_STATUS, ApplicationStatus.EJECTED.name()).build());

        eventListener.processProsecutionCaseEjected(envelope);


        verify(repository).save(argumentCaptor.capture());
        verify(courtApplicationRepository).save(courtApplicationEntityArgumentCaptor.capture());
        final CourtApplicationEntity updatedCourtApplicationEntity = courtApplicationEntityArgumentCaptor.getValue();
        final JsonNode courtApplicationNode = mapper.valueToTree(JSONValue.parse(updatedCourtApplicationEntity.getPayload()));
        Assert.assertEquals("Check if the application status is ejected", ApplicationStatus.EJECTED.name(), courtApplicationNode.path(APPLICATION_STATUS).asText());


        verify(hearingRepository).save(hearingEntityArgumentCaptor.capture());

        final HearingEntity updatedHearingEntity = hearingEntityArgumentCaptor.getValue();

        final JsonNode hearingNode = mapper.valueToTree(JSONValue.parse(updatedHearingEntity.getPayload()));
        Assert.assertEquals("Check if the application status is ejected", CASE_STATUS_EJECTED, hearingNode.path(PROSECUTION_CASES).get(0).path(CASE_STATUS).asText());

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

    private void verifyCaseNoteAddedEventResults(final CaseNoteAdded caseNoteAdded) {
        verify(caseNoteRepository).save(caseNoteArgumentCaptor.capture());

        final CaseNoteEntity caseNoteEntity = caseNoteArgumentCaptor.getValue();
        assertThat(caseNoteEntity.getCaseId(), equalTo(caseNoteAdded.getCaseId()));
        assertThat(caseNoteEntity.getNote(), equalTo(caseNoteAdded.getNote()));
        assertThat(caseNoteEntity.getCreatedDateTime(), equalTo(caseNoteAdded.getCreatedDateTime()));
        assertThat(caseNoteEntity.getFirstName(), equalTo(caseNoteAdded.getFirstName()));
        assertThat(caseNoteEntity.getLastName(), equalTo(caseNoteAdded.getLastName()));
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

    private String createPayload(final String payloadPath) throws IOException {
        final StringWriter writer = new StringWriter();
        InputStream inputStream = CourtApplicationEventListenerTest.class.getResourceAsStream(payloadPath);
        IOUtils.copy(inputStream, writer, UTF_8);
        inputStream.close();
        return writer.toString();

    }
}
