package uk.gov.moj.cpp.prosecution.event.listener;

import static java.util.UUID.randomUUID;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationRespondent;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.HearingResultedCaseUpdated;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecutioncase.event.listener.ProsecutionCaseDefendantUpdatedEventListener;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.mapping.SearchProsecutionCase;

import java.util.ArrayList;
import java.util.List;
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
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class ProsecutionDefendantUpdatedEventListenerTest {

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private ProsecutionCaseRepository repository;

    @Mock
    private CourtApplicationRepository courtApplicationRepository;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private ProsecutionCaseDefendantUpdated prosecutionCaseDefendantUpdated;

    @Mock
    private HearingResultedCaseUpdated hearingResultedCaseUpdated;

    @Mock
    private DefendantUpdate defendant;


    @Mock
    private List<Defendant> defandantsList;

    @Mock
    private ProsecutionCaseEntity prosecutionCaseEntity;

    @Mock
    private List<CourtApplicationEntity> applicationEntities;

    @Captor
    private ArgumentCaptor<ProsecutionCaseEntity> argumentCaptor;

    @Mock
    private JsonObject payload;

    @Mock
    private Metadata metadata;

    @Mock
    private ProsecutionCase prosecutionCase;

    @InjectMocks
    private ProsecutionCaseDefendantUpdatedEventListener eventListener;

    @Mock
    private StringToJsonObjectConverter stringToJsonObjectConverterMock;

    @Mock
    private ObjectMapper objectMapper;

    @Spy
    private ListToJsonArrayConverter jsonConverter;

    @Mock
    private SearchProsecutionCase searchProsecutionCase;

    @Before
    public void initMocks() {

        setField(this.jsonConverter, "mapper",
                new ObjectMapperProducer().objectMapper());
        setField(this.jsonConverter, "stringToJsonObjectConverter",
                new StringToJsonObjectConverter());
    }


    @Test
    public void shouldHandleProsecutionCaseDefendantUpdatedEvent() throws Exception {

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, ProsecutionCaseDefendantUpdated.class))
                .thenReturn(prosecutionCaseDefendantUpdated);
        when(envelope.metadata()).thenReturn(metadata);
        when(defendant.getId()).thenReturn(randomUUID());
        when(prosecutionCaseDefendantUpdated.getDefendant()).thenReturn(defendant);
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("payload", Json.createObjectBuilder()
                        .add("defendants", Json.createArrayBuilder().add(Json.createObjectBuilder()
                                .add("id", defendant.getId().toString()).build())
                                .build())
                        .build()).build();

        when(jsonObjectToObjectConverter.convert(jsonObject, ProsecutionCase.class))
                .thenReturn(prosecutionCase);
        when(prosecutionCaseEntity.getPayload()).thenReturn(jsonObject.toString());
        when(objectToJsonObjectConverter.convert(defendant)).thenReturn(jsonObject);
        when(courtApplicationRepository.findByLinkedCaseId(defendant.getProsecutionCaseId())).thenReturn(applicationEntities);
        when(objectToJsonObjectConverter.convert(prosecutionCase)).thenReturn(jsonObject);
        when(repository.findByCaseId(defendant.getProsecutionCaseId())).thenReturn(prosecutionCaseEntity);
        eventListener.processProsecutionCaseDefendantUpdated(envelope);
        verify(repository).save(argumentCaptor.capture());

    }

    @Test
    public void shouldUpdateMatchedRespondents() {

        List<CourtApplicationRespondent> courtApplicationRespondentList = new ArrayList<>();
        UUID commonUUID = randomUUID();
        Defendant defendant1 = Defendant.defendant().withId(commonUUID).build();
        Defendant defendant2 = Defendant.defendant().withId(commonUUID).build();
        Defendant defendant3 = Defendant.defendant().withId(randomUUID()).build();

        List<Defendant> defendantList = getDefendants(commonUUID, commonUUID, commonUUID, randomUUID());
        defendantList.add(defendant1);
        defendantList.add(defendant2);
        defendantList.add(defendant3);

        CourtApplicationParty party1 = CourtApplicationParty.courtApplicationParty().withDefendant(defendant1).build();
        CourtApplicationParty party2 = CourtApplicationParty.courtApplicationParty().withDefendant(defendant2).build();
        CourtApplicationParty party3 = CourtApplicationParty.courtApplicationParty().withDefendant(defendant3).build();

        CourtApplicationRespondent courtApplicationRespondent1 = CourtApplicationRespondent.courtApplicationRespondent().withPartyDetails(party1).build();
        CourtApplicationRespondent courtApplicationRespondent2 = CourtApplicationRespondent.courtApplicationRespondent().withPartyDetails(party2).build();
        CourtApplicationRespondent courtApplicationRespondent3 = CourtApplicationRespondent.courtApplicationRespondent().withPartyDetails(party3).build();

        courtApplicationRespondentList.add(courtApplicationRespondent1);
        courtApplicationRespondentList.add(courtApplicationRespondent2);
        courtApplicationRespondentList.add(courtApplicationRespondent3);

        CourtApplication courtApplication = CourtApplication.courtApplication().withRespondents(courtApplicationRespondentList).build();

        DefendantUpdate defendantUpdate = DefendantUpdate.defendantUpdate().withId(commonUUID).build();

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, ProsecutionCaseDefendantUpdated.class)).thenReturn(prosecutionCaseDefendantUpdated);
        when(envelope.metadata()).thenReturn(metadata);
        when(defendant.getId()).thenReturn(randomUUID());
        when(prosecutionCaseDefendantUpdated.getDefendant()).thenReturn(defendantUpdate);
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("payload", Json.createObjectBuilder()
                        .add("defendants", Json.createArrayBuilder().add(Json.createObjectBuilder()
                                .add("id", defendant.getId().toString()).build())
                                .build())
                        .build()).build();

        ProsecutionCase prosecutionCase1 = ProsecutionCase.prosecutionCase().withDefendants(defendantList).build();
        List<CourtApplicationEntity> courtApplicationEntityList = new ArrayList<>();
        CourtApplicationEntity courtApplicationEntity = new CourtApplicationEntity();
        courtApplicationEntityList.add(courtApplicationEntity);

        when(jsonObjectToObjectConverter.convert(jsonObject, ProsecutionCase.class)).thenReturn(prosecutionCase1);
        when(jsonObjectToObjectConverter.convert(jsonObject, CourtApplication.class)).thenReturn(courtApplication);
        when(stringToJsonObjectConverterMock.convert(courtApplicationEntity.getPayload())).thenReturn(jsonObject);
        when(prosecutionCaseEntity.getPayload()).thenReturn(jsonObject.toString());
        when(objectToJsonObjectConverter.convert(defendant)).thenReturn(jsonObject);
        when(courtApplicationRepository.findByLinkedCaseId(defendant1.getProsecutionCaseId())).thenReturn(courtApplicationEntityList);
        when(objectToJsonObjectConverter.convert(prosecutionCase1)).thenReturn(jsonObject);
        when(objectToJsonObjectConverter.convert(any(CourtApplication.class))).thenReturn(jsonObject);
        when(repository.findByCaseId(defendant.getProsecutionCaseId())).thenReturn(prosecutionCaseEntity);
        when(searchProsecutionCase.makeSearchable(prosecutionCase1, defendant1)).thenReturn(null);
        eventListener.processProsecutionCaseDefendantUpdated(envelope);
        verify(repository).save(argumentCaptor.capture());

    }

    @Test
    public void shouldHandleProsecutionCaseUpdatedEvent() throws Exception {

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, HearingResultedCaseUpdated.class)).thenReturn(hearingResultedCaseUpdated);
        when(envelope.metadata()).thenReturn(metadata);
        when(defendant.getId()).thenReturn(randomUUID());

        final UUID def1 = randomUUID();
        final UUID def2 = randomUUID();
        final UUID def3 = randomUUID();
        final UUID prosecutionCaseId = randomUUID();

        final List<Defendant> defsList = getDefendants(def1, def2, def3, prosecutionCaseId);

        when(hearingResultedCaseUpdated.getProsecutionCase()).thenReturn(prosecutionCase);
        when(prosecutionCase.getDefendants()).thenReturn(defsList);

        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("payload", Json.createObjectBuilder()
                        .add("defendants", Json.createArrayBuilder().add(Json.createObjectBuilder()
                                .add("id", defendant.getId().toString()).build())
                                .build())
                        .build()).build();

        final ProsecutionCase prosCase = ProsecutionCase.prosecutionCase().withDefendants(getDefendants(def1, def2, def3, prosecutionCaseId)).build();
        when(jsonObjectToObjectConverter.convert(jsonObject, ProsecutionCase.class)).thenReturn(prosCase);

        when(objectToJsonObjectConverter.convert(any(CourtApplication.class))).thenReturn(jsonObject);

        when(repository.findByCaseId(defendant.getProsecutionCaseId())).thenReturn(prosecutionCaseEntity);

        when(prosecutionCaseEntity.getPayload()).thenReturn(jsonObject.toString());
        when(prosecutionCaseEntity.getCaseId()).thenReturn(prosecutionCaseId);
        when(objectToJsonObjectConverter.convert(defendant)).thenReturn(jsonObject);
        when(repository.findByCaseId(prosecutionCaseId)).thenReturn(prosecutionCaseEntity);

        eventListener.processProsecutionCaseUpdated(envelope);
        verify(repository).save(argumentCaptor.capture());

    }

    private List<Defendant> getDefendants(final UUID defandantId1, final UUID defandantId2, final UUID defandantId3, final UUID prosecutionCaseId) {

        final UUID commonUUID = randomUUID();
        final Defendant defendant1 = Defendant.defendant().withId(defandantId1).withProsecutionCaseId(prosecutionCaseId).build();
        final Defendant defendant2 = Defendant.defendant().withId(defandantId2).withProsecutionCaseId(prosecutionCaseId).build();
        final Defendant defendant3 = Defendant.defendant().withId(defandantId3).withProsecutionCaseId(prosecutionCaseId).build();

        final List<Defendant> defsList = new ArrayList<>();
        defsList.add(defendant1);
        defsList.add(defendant2);
        defsList.add(defendant3);
        return defsList;
    }


}