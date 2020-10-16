package uk.gov.moj.cpp.prosecution.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantCaseOffences;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.Offence;
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

import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;

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


@RunWith(MockitoJUnitRunner.class)
public class ProsecutionOffencesUpdatedEventListenerTest {

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
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


    @InjectMocks
    private ProsecutionCaseOffencesUpdatedEventListener eventListener;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Spy
    private ListToJsonArrayConverter jsonConverter;

    @Before
    public void initMocks() {

        setField(this.jsonConverter, "mapper",
                new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter,"mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonConverter, "stringToJsonObjectConverter",
                new StringToJsonObjectConverter());
    }


    @Test
    public void shouldHandleProsecutionCaseOffencesUpdatedEvent() throws Exception {
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
        final UUID offenceId  = randomUUID();
        final DefendantCaseOffences defendantCaseOffences =DefendantCaseOffences.defendantCaseOffences()
                .withDefendantId(randomUUID())
                .withProsecutionCaseId(randomUUID())
                .withLegalAidStatus("Withdrawn")
                .withOffences(Stream.of(Offence.offence()
                        .withId(offenceId)
                        .withLaaApplnReference(LaaReference.laaReference()
                                .withStatusCode("WD")
                                .withLaaContractNumber("LAA1234")
                                .withStatusDate(LocalDate.now())
                                .build())
                        .build()).collect(Collectors.toList()))
                .build();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(defendantCaseOffences.getProsecutionCaseId())
                .withDefendants(Stream.of(Defendant.defendant()
                        .withLegalAidStatus("Withdrawn")
                        .withProceedingsConcluded(true)
                        .withId(defendantCaseOffences.getDefendantId())
                        .withOffences(Stream.of(Offence.offence()
                                .withId(offenceId)
                                .withLaaApplnReference(LaaReference.laaReference()
                                        .withStatusCode("wd")
                                        .withLaaContractNumber("LAA1234")
                                        .withStatusDate(LocalDate.now())
                                        .build())
                                .withJudicialResults(Stream.of(JudicialResult.judicialResult()
                                        .withJudicialResultId(randomUUID())
                                        .withResultText("Some Text")
                                        .build()).collect(Collectors.toList()))

                        .build()).collect(Collectors.toList()))
                        .build()).collect(Collectors.toList()))
                .build();
        final Hearing hearing = Hearing.hearing()
                .withProsecutionCases(Stream.of(prosecutionCase).collect(Collectors.toList()))
                .withId(randomUUID())
                .build();
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, ProsecutionCaseOffencesUpdated.class))
                .thenReturn(prosecutionCaseOffencesUpdated);
        when(envelope.metadata()).thenReturn(metadata);


        when(prosecutionCaseOffencesUpdated.getDefendantCaseOffences()).thenReturn(defendantCaseOffences);
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("payload", Json.createObjectBuilder()
                        .add("defendants", Json.createArrayBuilder().add(Json.createObjectBuilder()
                                .add("id", defendantCaseOffences.getDefendantId().toString())
                                .add("defendantLevelLegalAidStatus", "Withdrawn")
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
        when(repository.findByCaseId(defendantCaseOffences.getProsecutionCaseId())).thenReturn(prosecutionCaseEntity);
        when(caseDefendantHearingRepository.findByDefendantId(defendantCaseOffences.getDefendantId()))
                .thenReturn(Arrays.asList(caseDefendantHearingEntity));
        when(caseDefendantHearingEntity.getHearing()).thenReturn(hearingEntity);

        when(hearingEntity.getPayload()).thenReturn(hearingJsonObject.toString());
        when(jsonObjectToObjectConverter.convert(hearingJsonObject, Hearing.class)).thenReturn(hearing);

        eventListener.processProsecutionCaseOffencesUpdated(envelope);
        verify(repository).save(argumentCaptor.capture());
        verify(hearingRepository).save(hearingEntityArgumentCaptor.capture());
        assertThat(argumentCaptor.getAllValues(), is(notNullValue()));
        final ProsecutionCaseEntity prosecutionCaseEntity = argumentCaptor.getAllValues().get(0);
        final JsonNode prosecutionCaseNode = mapper.valueToTree(JSONValue.parse(prosecutionCaseEntity.getPayload()));
        assertThat(prosecutionCaseNode.path("defendants").get(0).path("legalAidStatus").asText(), is(""));
        assertThat(prosecutionCaseNode.path("defendants").get(0).path("proceedingsConcluded").asBoolean(), is(true));
        assertThat(prosecutionCaseNode.path("defendants").get(0).path("offences").get(0).path("judicialResults").get(0).path("resultText").asText(), is("Some Text"));

    }
}