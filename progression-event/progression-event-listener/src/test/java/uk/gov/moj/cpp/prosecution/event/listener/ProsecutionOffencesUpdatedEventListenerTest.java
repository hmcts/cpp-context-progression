package uk.gov.moj.cpp.prosecution.event.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.minidev.json.JSONValue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.DefendantCaseOffences;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseOffencesUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecutioncase.event.listener.ProsecutionCaseOffencesUpdatedEventListener;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;


@RunWith(MockitoJUnitRunner.class)
public class ProsecutionOffencesUpdatedEventListenerTest {

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private ProsecutionCaseRepository repository;

    @Mock
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private ProsecutionCaseOffencesUpdated prosecutionCaseOffencesUpdated;

    @Mock
    private DefendantCaseOffences defendantCaseOffences;

    @Mock
    private ProsecutionCaseEntity prosecutionCaseEntity;

    @Mock
    private CaseDefendantHearingEntity caseDefendantHearingEntity;

    @Mock
    private HearingEntity hearingEntity;

    @Captor
    private ArgumentCaptor<ProsecutionCaseEntity> argumentCaptor;

    @Captor
    private ArgumentCaptor<HearingEntity> hearingEntityArgumentCaptor;

    @Mock
    private JsonObject payload;

    @Mock
    private Metadata metadata;

    @Mock
    private ProsecutionCase prosecutionCase;

    @Mock
    private Hearing hearing;

    @InjectMocks
    private ProsecutionCaseOffencesUpdatedEventListener eventListener;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Mock
    private ObjectMapper objectMapper;

    @Spy
    private ListToJsonArrayConverter jsonConverter;

    @Before
    public void initMocks() {

        setField(this.jsonConverter, "mapper",
                new ObjectMapperProducer().objectMapper());
        setField(this.jsonConverter, "stringToJsonObjectConverter",
                new StringToJsonObjectConverter());
    }


    @Test
    public void shouldHandleProsecutionCaseOffencesUpdatedEvent() throws Exception {
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, ProsecutionCaseOffencesUpdated.class))
                .thenReturn(prosecutionCaseOffencesUpdated);
        when(envelope.metadata()).thenReturn(metadata);
        when(defendantCaseOffences.getDefendantId()).thenReturn(UUID.randomUUID());
        when(defendantCaseOffences.getProsecutionCaseId()).thenReturn(UUID.randomUUID());
        when(prosecutionCaseOffencesUpdated.getDefendantCaseOffences()).thenReturn(defendantCaseOffences);
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("payload", Json.createObjectBuilder()
                        .add("defendants", Json.createArrayBuilder().add(Json.createObjectBuilder()
                                .add("id", defendantCaseOffences.getDefendantId().toString())
                                .add("defendantLevelLegalAidStatus", "Granted")
                                .add("proceedingConcluded", true)
                                .build())
                                .build())
                        .build()).build();
        final JsonObject hearingJsonObject = Json.createObjectBuilder()
                .add("payload", Json.createObjectBuilder()
                        .add("prosecutionCases", Json.createArrayBuilder().add(Json.createObjectBuilder()
                                .add("id", defendantCaseOffences.getProsecutionCaseId().toString())
                                .add("defendants", Json.createArrayBuilder().add(Json.createObjectBuilder()
                                        .add("id", defendantCaseOffences.getDefendantId().toString()).build()))
                                .build())
                        .build()).build()).build();

        when(jsonObjectToObjectConverter.convert(jsonObject, ProsecutionCase.class))
                .thenReturn(prosecutionCase);
        when(prosecutionCaseEntity.getPayload()).thenReturn(jsonObject.toString());
        when(objectToJsonObjectConverter.convert(defendantCaseOffences)).thenReturn(jsonObject);
        when(objectToJsonObjectConverter.convert(prosecutionCase)).thenReturn(jsonObject);
        when(repository.findByCaseId(defendantCaseOffences.getProsecutionCaseId())).thenReturn(prosecutionCaseEntity);
        when(caseDefendantHearingRepository.findByDefendantId(defendantCaseOffences.getDefendantId()))
                .thenReturn(Arrays.asList(caseDefendantHearingEntity));
        when(caseDefendantHearingEntity.getHearing()).thenReturn(hearingEntity);

        when(hearingEntity.getPayload()).thenReturn(hearingJsonObject.toString());
        when(objectToJsonObjectConverter.convert(hearingEntity.getPayload())).thenReturn(hearingJsonObject);
        when(jsonObjectToObjectConverter.convert(hearingJsonObject, Hearing.class)).thenReturn(hearing);
        when(hearing.getProsecutionCases()).thenReturn(Arrays.asList(prosecutionCase));
        when(objectToJsonObjectConverter.convert(hearing)).thenReturn(hearingJsonObject);

        eventListener.processProsecutionCaseOffencesUpdated(envelope);
        verify(repository).save(argumentCaptor.capture());
        verify(hearingRepository).save(hearingEntityArgumentCaptor.capture());
        assertThat(argumentCaptor.getAllValues(), is(notNullValue()));
        final ProsecutionCaseEntity prosecutionCaseEntity = argumentCaptor.getAllValues().get(0);
        final JsonNode prosecutionCaseNode = mapper.valueToTree(JSONValue.parse(prosecutionCaseEntity.getPayload()));
        assertThat(prosecutionCaseNode.path("payload").path("defendants").get(0).path("defendantLevelLegalAidStatus").asText(), is("Granted"));
        assertThat(prosecutionCaseNode.path("payload").path("defendants").get(0).path("proceedingConcluded").asBoolean(), is(true));


    }
}