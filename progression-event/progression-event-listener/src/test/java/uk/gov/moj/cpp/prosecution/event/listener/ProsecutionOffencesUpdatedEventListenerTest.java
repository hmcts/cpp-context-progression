package uk.gov.moj.cpp.prosecution.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Mockito.times;
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
import uk.gov.justice.core.courts.ReportingRestriction;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecutioncase.event.listener.ProsecutionCaseOffencesUpdatedEventListener;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArray;
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
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonConverter, "stringToJsonObjectConverter",
                new StringToJsonObjectConverter());
    }


    @Test
    public void shouldHandleProsecutionCaseOffencesUpdatedEvent() throws Exception {
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
        final UUID offenceId = randomUUID();
        final UUID reportingRestrictionId1 = randomUUID();
        final UUID reportingRestrictionId2 = randomUUID();
        final String reportingRestrictionLabel = "RRLabel";

        final DefendantCaseOffences defendantCaseOffences = DefendantCaseOffences.defendantCaseOffences()
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
                        .withReportingRestrictions(Stream.of(prepareReportingRestriction(reportingRestrictionId1, reportingRestrictionLabel))
                                .collect(Collectors.toList()))
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
                                .withReportingRestrictions(Stream.of(prepareReportingRestriction(reportingRestrictionId1, reportingRestrictionLabel),
                                        prepareReportingRestriction(reportingRestrictionId2, reportingRestrictionLabel))
                                        .collect(Collectors.toList()))
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

        final JsonNode reportingRestrictionListJsonNode = prosecutionCaseNode.path("defendants").get(0).path("offences").get(0).path("reportingRestrictions");
        assertThat(reportingRestrictionListJsonNode.size(), is(1));

        final JsonNode reportingRestrictionJsonNode = reportingRestrictionListJsonNode.get(0);
        assertThat(reportingRestrictionJsonNode.path("id").asText(), is(reportingRestrictionId1.toString()));
        assertThat(reportingRestrictionJsonNode.path("label").asText(), is(reportingRestrictionLabel));
        assertThat(reportingRestrictionJsonNode.path("orderedDate").asText(), is(LocalDate.now().toString()));
    }


    @Test
    public void shouldNotUpdateWithdrawnOffenceInAdjournedHearing() {
        final UUID hearing1Id = randomUUID();
        final UUID hearing2Id = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID offence3Id = randomUUID();


        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId)
                .withDefendants(new ArrayList<>(Arrays.asList(Defendant.defendant()
                        .withId(defendantId)
                        .withOffences(new ArrayList<>(Arrays.asList(Offence.offence()
                                        .withId(offence1Id)
                                        .build(),
                                Offence.offence()
                                        .withId(offence2Id)
                                        .build(),
                                Offence.offence()
                                        .withId(offence3Id)
                                        .build())))
                        .build())))
                .build();

        final JsonObject prosecutionCaseJson = objectToJsonObjectConverter.convert(prosecutionCase);

        final ProsecutionCaseOffencesUpdated prosecutionCaseOffencesUpdated = ProsecutionCaseOffencesUpdated.prosecutionCaseOffencesUpdated()
                .withDefendantCaseOffences(DefendantCaseOffences.defendantCaseOffences()
                        .withProsecutionCaseId(prosecutionCaseId)
                        .withDefendantId(defendantId)
                        .withOffences(new ArrayList<>(Arrays.asList(
                                Offence.offence().withId(offence1Id).withWording("retest").build(),
                                Offence.offence().withId(offence2Id).withWording("retest").build(),
                                Offence.offence().withId(offence3Id).withWording("retest").build())))
                        .build())
                .build();
        final JsonObject eventPayload = objectToJsonObjectConverter.convert(prosecutionCaseOffencesUpdated);
        when(envelope.payloadAsJsonObject()).thenReturn(eventPayload);

        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(prosecutionCaseId);
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        when(repository.findByCaseId(prosecutionCaseId)).thenReturn(prosecutionCaseEntity);

        final CaseDefendantHearingEntity caseDefendantHearing1Entity = new CaseDefendantHearingEntity();
        caseDefendantHearing1Entity.setId(new CaseDefendantHearingKey(prosecutionCaseId, defendantId, hearing1Id));
        final HearingEntity hearing1Entity = new HearingEntity();
        hearing1Entity.setHearingId(hearing1Id);
        final Hearing hearing1 = Hearing.hearing()
                .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                        .withId(prosecutionCaseId)
                        .withDefendants(new ArrayList<>(Arrays.asList(Defendant.defendant()
                                .withId(defendantId)
                                .withOffences(new ArrayList<>(Arrays.asList(Offence.offence().withId(offence1Id).build(),
                                        Offence.offence().withId(offence2Id).build(),
                                        Offence.offence().withId(offence3Id).build())))
                                .build())))
                        .build()))
                .build();

        hearing1Entity.setPayload(objectToJsonObjectConverter.convert(hearing1).toString());
        caseDefendantHearing1Entity.setHearing(hearing1Entity);

        final CaseDefendantHearingEntity caseDefendantHearing2Entity = new CaseDefendantHearingEntity();
        caseDefendantHearing2Entity.setId(new CaseDefendantHearingKey(prosecutionCaseId, defendantId, hearing2Id));
        final HearingEntity hearing2Entity = new HearingEntity();
        hearing1Entity.setHearingId(hearing2Id);
        final Hearing hearing2 = Hearing.hearing()
                .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                        .withId(prosecutionCaseId)
                        .withDefendants(new ArrayList<>(Arrays.asList(Defendant.defendant()
                                .withId(defendantId)
                                .withOffences(new ArrayList<>(Arrays.asList(Offence.offence().withId(offence1Id).build(),
                                        Offence.offence().withId(offence2Id).build())))
                                .build())))
                        .build()))
                .build();

        hearing2Entity.setPayload(objectToJsonObjectConverter.convert(hearing2).toString());
        caseDefendantHearing2Entity.setHearing(hearing2Entity);
        when(caseDefendantHearingRepository.findByDefendantId(defendantId)).thenReturn(Arrays.asList(caseDefendantHearing1Entity, caseDefendantHearing2Entity));
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, ProsecutionCaseOffencesUpdated.class)).thenReturn(prosecutionCaseOffencesUpdated);

        JsonObject hearing1Json = objectToJsonObjectConverter.convert(hearing1);
        when(jsonObjectToObjectConverter.convert(hearing1Json, Hearing.class)).thenReturn(hearing1);
        JsonObject hearing2Json = objectToJsonObjectConverter.convert(hearing2);
        when(jsonObjectToObjectConverter.convert(hearing2Json, Hearing.class)).thenReturn(hearing2);

        when(jsonObjectToObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class)).thenReturn(prosecutionCase);
        eventListener.processProsecutionCaseOffencesUpdated(envelope);


        verify(repository).save(argumentCaptor.capture());
        verify(hearingRepository, times(2)).save(hearingEntityArgumentCaptor.capture());

        List<HearingEntity> hearingEntityList = hearingEntityArgumentCaptor.getAllValues();
        JsonObject jsonObjectHearing1 = stringToJsonObjectConverter.convert(hearingEntityList.get(0).getPayload());
        JsonObject jsonObjectHearing2 = stringToJsonObjectConverter.convert(hearingEntityList.get(1).getPayload());

        JsonArray jsonHearing1OffencesArray = jsonObjectHearing1.getJsonArray("prosecutionCases").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonArray("offences");
        assertThat(jsonHearing1OffencesArray.size(), is(3));
        assertThat(jsonHearing1OffencesArray.getJsonObject(0).getJsonString("id").getString(), is(offence1Id.toString()));
        assertThat(jsonHearing1OffencesArray.getJsonObject(1).getJsonString("id").getString(), is(offence2Id.toString()));
        assertThat(jsonHearing1OffencesArray.getJsonObject(2).getJsonString("id").getString(), is(offence3Id.toString()));

        JsonArray jsonHearing2OffencesArray = jsonObjectHearing2.getJsonArray("prosecutionCases").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonArray("offences");
        assertThat(jsonHearing2OffencesArray.size(), is(2));
        assertThat(jsonHearing2OffencesArray.getJsonObject(0).getJsonString("id").getString(), is(offence1Id.toString()));
        assertThat(jsonHearing2OffencesArray.getJsonObject(1).getJsonString("id").getString(), is(offence2Id.toString()));

    }

    @Test
    public void shouldAddOffencesToBothFirstHearingAndAdjournedHearing() {
        final UUID hearing1Id = randomUUID();
        final UUID hearing2Id = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID offence3Id = randomUUID();


        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId)
                .withDefendants(new ArrayList<>(Arrays.asList(Defendant.defendant()
                        .withId(defendantId)
                        .withOffences(new ArrayList<>(Arrays.asList(Offence.offence()
                                        .withId(offence1Id)
                                        .build(),
                                Offence.offence()
                                        .withId(offence2Id)
                                        .build())))
                        .build())))
                .build();

        final JsonObject prosecutionCaseJson = objectToJsonObjectConverter.convert(prosecutionCase);

        final ProsecutionCaseOffencesUpdated prosecutionCaseOffencesUpdated = ProsecutionCaseOffencesUpdated.prosecutionCaseOffencesUpdated()
                .withDefendantCaseOffences(DefendantCaseOffences.defendantCaseOffences()
                        .withProsecutionCaseId(prosecutionCaseId)
                        .withDefendantId(defendantId)
                        .withOffences(new ArrayList<>(Arrays.asList(
                                Offence.offence().withId(offence1Id).withWording("retest").build(),
                                Offence.offence().withId(offence2Id).withWording("retest").build(),
                                Offence.offence().withId(offence3Id).withWording("add").build())))
                        .build())
                .build();
        final JsonObject eventPayload = objectToJsonObjectConverter.convert(prosecutionCaseOffencesUpdated);
        when(envelope.payloadAsJsonObject()).thenReturn(eventPayload);

        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(prosecutionCaseId);
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        when(repository.findByCaseId(prosecutionCaseId)).thenReturn(prosecutionCaseEntity);

        final CaseDefendantHearingEntity caseDefendantHearing1Entity = new CaseDefendantHearingEntity();
        caseDefendantHearing1Entity.setId(new CaseDefendantHearingKey(prosecutionCaseId, defendantId, hearing1Id));
        final HearingEntity hearing1Entity = new HearingEntity();
        hearing1Entity.setHearingId(hearing1Id);
        final Hearing hearing1 = Hearing.hearing()
                .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                        .withId(prosecutionCaseId)
                        .withDefendants(new ArrayList<>(Arrays.asList(Defendant.defendant()
                                .withId(defendantId)
                                .withOffences(new ArrayList<>(Arrays.asList(Offence.offence().withId(offence1Id).build(),
                                        Offence.offence().withId(offence2Id).build())))
                                .build())))
                        .build()))
                .build();

        hearing1Entity.setPayload(objectToJsonObjectConverter.convert(hearing1).toString());
        caseDefendantHearing1Entity.setHearing(hearing1Entity);

        final CaseDefendantHearingEntity caseDefendantHearing2Entity = new CaseDefendantHearingEntity();
        caseDefendantHearing2Entity.setId(new CaseDefendantHearingKey(prosecutionCaseId, defendantId, hearing2Id));
        final HearingEntity hearing2Entity = new HearingEntity();
        hearing1Entity.setHearingId(hearing2Id);
        final Hearing hearing2 = Hearing.hearing()
                .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                        .withId(prosecutionCaseId)
                        .withDefendants(new ArrayList<>(Arrays.asList(Defendant.defendant()
                                .withId(defendantId)
                                .withOffences(new ArrayList<>(Arrays.asList(Offence.offence().withId(offence1Id).build(),
                                        Offence.offence().withId(offence2Id).build())))
                                .build())))
                        .build()))
                .build();

        hearing2Entity.setPayload(objectToJsonObjectConverter.convert(hearing2).toString());
        caseDefendantHearing2Entity.setHearing(hearing2Entity);
        when(caseDefendantHearingRepository.findByDefendantId(defendantId)).thenReturn(Arrays.asList(caseDefendantHearing1Entity, caseDefendantHearing2Entity));
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, ProsecutionCaseOffencesUpdated.class)).thenReturn(prosecutionCaseOffencesUpdated);

        JsonObject hearing1Json = objectToJsonObjectConverter.convert(hearing1);
        when(jsonObjectToObjectConverter.convert(hearing1Json, Hearing.class)).thenReturn(hearing1);
        JsonObject hearing2Json = objectToJsonObjectConverter.convert(hearing2);
        when(jsonObjectToObjectConverter.convert(hearing2Json, Hearing.class)).thenReturn(hearing2);

        when(jsonObjectToObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class)).thenReturn(prosecutionCase);
        eventListener.processProsecutionCaseOffencesUpdated(envelope);


        verify(repository).save(argumentCaptor.capture());
        verify(hearingRepository, times(2)).save(hearingEntityArgumentCaptor.capture());

        List<HearingEntity> hearingEntityList = hearingEntityArgumentCaptor.getAllValues();
        JsonObject jsonObjectHearing1 = stringToJsonObjectConverter.convert(hearingEntityList.get(0).getPayload());
        JsonObject jsonObjectHearing2 = stringToJsonObjectConverter.convert(hearingEntityList.get(1).getPayload());

        JsonArray jsonHearing1OffencesArray = jsonObjectHearing1.getJsonArray("prosecutionCases").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonArray("offences");
        assertThat(jsonHearing1OffencesArray.size(), is(3));
        assertThat(jsonHearing1OffencesArray.getJsonObject(0).getJsonString("id").getString(), is(offence1Id.toString()));
        assertThat(jsonHearing1OffencesArray.getJsonObject(1).getJsonString("id").getString(), is(offence2Id.toString()));
        assertThat(jsonHearing1OffencesArray.getJsonObject(2).getJsonString("id").getString(), is(offence3Id.toString()));

        JsonArray jsonHearing2OffencesArray = jsonObjectHearing2.getJsonArray("prosecutionCases").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonArray("offences");
        assertThat(jsonHearing2OffencesArray.size(), is(3));
        assertThat(jsonHearing2OffencesArray.getJsonObject(0).getJsonString("id").getString(), is(offence1Id.toString()));
        assertThat(jsonHearing2OffencesArray.getJsonObject(1).getJsonString("id").getString(), is(offence2Id.toString()));
        assertThat(jsonHearing2OffencesArray.getJsonObject(2).getJsonString("id").getString(), is(offence3Id.toString()));

    }

    private ReportingRestriction prepareReportingRestriction(final UUID reportingRestrictionId,
                                                             final String label) {
        return ReportingRestriction.reportingRestriction()
                .withId(reportingRestrictionId)
                .withLabel(label)
                .withOrderedDate(LocalDate.now())
                .build();
    }
}