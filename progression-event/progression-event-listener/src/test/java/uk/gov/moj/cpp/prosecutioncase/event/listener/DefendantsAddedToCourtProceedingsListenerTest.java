package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantsAddedToCourtProceedings;
import uk.gov.justice.core.courts.DefendantsAddedToCourtProceedingsV2;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.cpp.progression.events.NewDefendantAddedToHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class DefendantsAddedToCourtProceedingsListenerTest {

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private ProsecutionCaseRepository repository;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private DefendantsAddedToCourtProceedings defendantsAddedToCourtProceedings;

    @Mock
    private DefendantsAddedToCourtProceedingsV2 defendantsAddedToCourtProceedingsV2;

    @Mock
    private NewDefendantAddedToHearing newDefendantAddedToHearing;

    @Mock
    private Defendant defendant;

    @Mock
    private ProsecutionCaseEntity prosecutionCaseEntity;

    @Captor
    private ArgumentCaptor<ProsecutionCaseEntity> argumentCaptor;

    @Mock
    private JsonObject payload;

    @Mock
    private ProsecutionCase prosecutionCase;

    @InjectMocks
    private DefendantsAddedToCourtProceedingsListener eventListener;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Mock
    private ObjectMapper objectMapper;

    @Spy
    private ListToJsonArrayConverter jsonConverter;

    @Mock
    private HearingRepository hearingRepository;

    @BeforeEach
    public void initMocks() {

        setField(this.jsonConverter, "mapper",
                objectMapper);
        setField(this.jsonConverter, "stringToJsonObjectConverter",
                stringToJsonObjectConverter);

    }

    @Test
    public void shouldHandleProsecutionCaseDefendantUpdatedEvent() throws Exception {

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, DefendantsAddedToCourtProceedings.class))
                .thenReturn(defendantsAddedToCourtProceedings);
        when(defendant.getId()).thenReturn(UUID.randomUUID());
        when(defendant.getProsecutionCaseId()).thenReturn(UUID.randomUUID());

        when(defendantsAddedToCourtProceedings.getDefendants()).thenReturn(Collections.singletonList(defendant));

        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("payload", Json.createObjectBuilder()
                        .add("defendants", Json.createArrayBuilder().add(Json.createObjectBuilder()
                                        .add("id", defendant.getId().toString())
                                        .add("prosecutionCaseId", defendant.getProsecutionCaseId().toString()).build())
                                .build())
                        .build()).build();

        when(jsonObjectToObjectConverter.convert(jsonObject, ProsecutionCase.class))
                .thenReturn(prosecutionCase);
        when(prosecutionCaseEntity.getPayload()).thenReturn(jsonObject.toString());
        when(objectToJsonObjectConverter.convert(prosecutionCase)).thenReturn(jsonObject);
        when(repository.findByCaseId(defendant.getProsecutionCaseId())).thenReturn(prosecutionCaseEntity);
        eventListener.processDefendantsAddedToCourtProceedings(envelope);
        verify(repository).save(argumentCaptor.capture());
    }

    @Test
    void shouldHandleProsecutionCaseDefendantUpdatedEventV2() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, DefendantsAddedToCourtProceedingsV2.class))
                .thenReturn(defendantsAddedToCourtProceedingsV2);
        when(defendant.getId()).thenReturn(UUID.randomUUID());
        when(defendant.getProsecutionCaseId()).thenReturn(UUID.randomUUID());

        when(defendantsAddedToCourtProceedingsV2.getDefendants()).thenReturn(Collections.singletonList(defendant));

        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("payload", Json.createObjectBuilder()
                        .add("defendants", Json.createArrayBuilder().add(Json.createObjectBuilder()
                                        .add("id", defendant.getId().toString())
                                        .add("prosecutionCaseId", defendant.getProsecutionCaseId().toString()).build())
                                .build())
                        .build()).build();

        when(jsonObjectToObjectConverter.convert(jsonObject, ProsecutionCase.class))
                .thenReturn(prosecutionCase);
        when(prosecutionCaseEntity.getPayload()).thenReturn(jsonObject.toString());
        when(objectToJsonObjectConverter.convert(prosecutionCase)).thenReturn(jsonObject);
        when(repository.findByCaseId(defendant.getProsecutionCaseId())).thenReturn(prosecutionCaseEntity);
        eventListener.processDefendantsAddedToCourtProceedingsV2(envelope);
        verify(repository).save(argumentCaptor.capture());
    }

    @Test
    public void newDefendantAddedShouldBeUpdatedInHearing() throws Exception {
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
        final ObjectToJsonObjectConverter objectToJsonObjectConverter1 = new ObjectToJsonObjectConverter(mapper);
        final UUID defendantId1 = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();
        final Defendant defendant = Defendant.defendant().withId(defendantId1).build();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().withId(caseId).withDefendants(Lists.newArrayList(defendant)).build();
        final Hearing hearing = Hearing.hearing().withProsecutionCases(Lists.newArrayList(prosecutionCase)).build();
        when(newDefendantAddedToHearing.getProsecutionCaseId()).thenReturn(caseId);
        final UUID newDefendantId = UUID.randomUUID();
        when(newDefendantAddedToHearing.getDefendants()).thenReturn(Lists.newArrayList(Defendant.defendant().withId(newDefendantId).build()));
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, NewDefendantAddedToHearing.class))
                .thenReturn(newDefendantAddedToHearing);

        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(UUID.randomUUID());
        hearingEntity.setPayload(objectToJsonObjectConverter1.convert(hearing).toString());
        when(hearingRepository.findBy(newDefendantAddedToHearing.getHearingId())).thenReturn(hearingEntity);

        when(stringToJsonObjectConverter.convert(hearingEntity.getPayload())).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), eq(Hearing.class))).thenReturn(hearing);
        when(objectToJsonObjectConverter.convert(hearing)).thenReturn(objectToJsonObjectConverter1.convert(hearing));
        eventListener.addNewDefendantToHearing(envelope);
        final ArgumentCaptor<HearingEntity> hearingEntityArgumentCaptor = ArgumentCaptor.forClass(HearingEntity.class);
        verify(hearingRepository).save(hearingEntityArgumentCaptor.capture());
    }

    @Test
    public void shouldFilterDuplicateOffencesByIdWhenProcessingDefendantsAddedToCourtProceedings() {
        // Given
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final UUID offenceId1 = UUID.randomUUID();
        final UUID offenceId2 = UUID.randomUUID();
        
        final Offence offence1 = Offence.offence()
                .withId(offenceId1)
                .withOffenceCode("TFL123")
                .withOffenceTitle("Test Offence 1")
                .build();
        
        final Offence duplicateOffence1 = Offence.offence()
                .withId(offenceId1) // Same ID as offence1
                .withOffenceCode("TFL456")
                .withOffenceTitle("Test Offence Duplicate")
                .build();
        
        final Offence offence2 = Offence.offence()
                .withId(offenceId2)
                .withOffenceCode("TFL789")
                .withOffenceTitle("Test Offence 2")
                .build();
        
        final List<Offence> offencesWithDuplicates = Lists.newArrayList(offence1, duplicateOffence1, offence2);
        
        final Defendant defendantWithDuplicateOffences = Defendant.defendant()
                .withId(defendantId)
                .withProsecutionCaseId(caseId)
                .withOffences(offencesWithDuplicates)
                .build();
        
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, DefendantsAddedToCourtProceedings.class))
                .thenReturn(defendantsAddedToCourtProceedings);
        when(defendantsAddedToCourtProceedings.getDefendants())
                .thenReturn(Collections.singletonList(defendantWithDuplicateOffences));
        
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withDefendants(new ArrayList<>())
                .build();
        
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("payload", Json.createObjectBuilder()
                        .add("defendants", Json.createArrayBuilder().build())
                        .build()).build();
        
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), eq(ProsecutionCase.class)))
                .thenReturn(prosecutionCase);
        when(prosecutionCaseEntity.getPayload()).thenReturn(jsonObject.toString());
        when(objectToJsonObjectConverter.convert(any(ProsecutionCase.class))).thenReturn(jsonObject);
        when(repository.findByCaseId(caseId)).thenReturn(prosecutionCaseEntity);
        
        // When
        eventListener.processDefendantsAddedToCourtProceedings(envelope);
        
        // Then
        verify(repository).save(argumentCaptor.capture());
        final ProsecutionCaseEntity savedEntity = argumentCaptor.getValue();
        assertThat(savedEntity, is(notNullValue()));
        // Verify that duplicate offences were filtered - should have only 2 unique offences
        assertThat(defendantWithDuplicateOffences.getOffences().size(), is(2));
        assertThat(defendantWithDuplicateOffences.getOffences().stream()
                .map(Offence::getId)
                .distinct()
                .count(), is(2L));
    }

    @Test
    public void shouldNotFilterOffencesWhenNoDuplicatesExist() {
        // Given
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final UUID offenceId1 = UUID.randomUUID();
        final UUID offenceId2 = UUID.randomUUID();
        final UUID offenceId3 = UUID.randomUUID();
        
        final List<Offence> uniqueOffences = Lists.newArrayList(
                Offence.offence().withId(offenceId1).withOffenceCode("TFL123").build(),
                Offence.offence().withId(offenceId2).withOffenceCode("TFL456").build(),
                Offence.offence().withId(offenceId3).withOffenceCode("TFL789").build()
        );
        
        final Defendant defendantWithUniqueOffences = Defendant.defendant()
                .withId(defendantId)
                .withProsecutionCaseId(caseId)
                .withOffences(uniqueOffences)
                .build();
        
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, DefendantsAddedToCourtProceedings.class))
                .thenReturn(defendantsAddedToCourtProceedings);
        when(defendantsAddedToCourtProceedings.getDefendants())
                .thenReturn(Collections.singletonList(defendantWithUniqueOffences));
        
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withDefendants(new ArrayList<>())
                .build();
        
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("payload", Json.createObjectBuilder()
                        .add("defendants", Json.createArrayBuilder().build())
                        .build()).build();
        
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), eq(ProsecutionCase.class)))
                .thenReturn(prosecutionCase);
        when(prosecutionCaseEntity.getPayload()).thenReturn(jsonObject.toString());
        when(objectToJsonObjectConverter.convert(any(ProsecutionCase.class))).thenReturn(jsonObject);
        when(repository.findByCaseId(caseId)).thenReturn(prosecutionCaseEntity);
        
        // When
        eventListener.processDefendantsAddedToCourtProceedings(envelope);
        
        // Then
        verify(repository).save(argumentCaptor.capture());
        // Verify that all offences are preserved when no duplicates exist
        assertThat(defendantWithUniqueOffences.getOffences().size(), is(3));
    }

    @Test
    public void shouldHandleNullOffencesListWhenProcessingDefendants() {
        // Given
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        
        final Defendant defendantWithNullOffences = Defendant.defendant()
                .withId(defendantId)
                .withProsecutionCaseId(caseId)
                .withOffences(null)
                .build();
        
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, DefendantsAddedToCourtProceedings.class))
                .thenReturn(defendantsAddedToCourtProceedings);
        when(defendantsAddedToCourtProceedings.getDefendants())
                .thenReturn(Collections.singletonList(defendantWithNullOffences));
        
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withDefendants(new ArrayList<>())
                .build();
        
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("payload", Json.createObjectBuilder()
                        .add("defendants", Json.createArrayBuilder().build())
                        .build()).build();
        
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), eq(ProsecutionCase.class)))
                .thenReturn(prosecutionCase);
        when(prosecutionCaseEntity.getPayload()).thenReturn(jsonObject.toString());
        when(objectToJsonObjectConverter.convert(any(ProsecutionCase.class))).thenReturn(jsonObject);
        when(repository.findByCaseId(caseId)).thenReturn(prosecutionCaseEntity);
        
        // When
        eventListener.processDefendantsAddedToCourtProceedings(envelope);
        
        // Then - should not throw exception
        verify(repository).save(argumentCaptor.capture());
    }

    @Test
    public void shouldHandleEmptyOffencesListWhenProcessingDefendants() {
        // Given
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        
        final Defendant defendantWithEmptyOffences = Defendant.defendant()
                .withId(defendantId)
                .withProsecutionCaseId(caseId)
                .withOffences(Collections.emptyList())
                .build();
        
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, DefendantsAddedToCourtProceedings.class))
                .thenReturn(defendantsAddedToCourtProceedings);
        when(defendantsAddedToCourtProceedings.getDefendants())
                .thenReturn(Collections.singletonList(defendantWithEmptyOffences));
        
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withDefendants(new ArrayList<>())
                .build();
        
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("payload", Json.createObjectBuilder()
                        .add("defendants", Json.createArrayBuilder().build())
                        .build()).build();
        
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), eq(ProsecutionCase.class)))
                .thenReturn(prosecutionCase);
        when(prosecutionCaseEntity.getPayload()).thenReturn(jsonObject.toString());
        when(objectToJsonObjectConverter.convert(any(ProsecutionCase.class))).thenReturn(jsonObject);
        when(repository.findByCaseId(caseId)).thenReturn(prosecutionCaseEntity);
        
        // When
        eventListener.processDefendantsAddedToCourtProceedings(envelope);
        
        // Then - should not throw exception
        verify(repository).save(argumentCaptor.capture());
    }

    @Test
    public void shouldFilterDuplicateOffencesByIdWhenProcessingDefendantsAddedToCourtProceedingsV2() {
        // Given
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final UUID offenceId1 = UUID.randomUUID();
        
        final Offence offence1 = Offence.offence()
                .withId(offenceId1)
                .withOffenceCode("TFL123")
                .withOffenceTitle("Test Offence 1")
                .build();
        
        final Offence duplicateOffence1 = Offence.offence()
                .withId(offenceId1) // Same ID as offence1
                .withOffenceCode("TFL456")
                .withOffenceTitle("Test Offence Duplicate")
                .build();
        
        final List<Offence> offencesWithDuplicates = Lists.newArrayList(offence1, duplicateOffence1);
        
        final Defendant defendantWithDuplicateOffences = Defendant.defendant()
                .withId(defendantId)
                .withProsecutionCaseId(caseId)
                .withOffences(offencesWithDuplicates)
                .build();
        
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, DefendantsAddedToCourtProceedingsV2.class))
                .thenReturn(defendantsAddedToCourtProceedingsV2);
        when(defendantsAddedToCourtProceedingsV2.getDefendants())
                .thenReturn(Collections.singletonList(defendantWithDuplicateOffences));
        
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withDefendants(new ArrayList<>())
                .build();
        
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("payload", Json.createObjectBuilder()
                        .add("defendants", Json.createArrayBuilder().build())
                        .build()).build();
        
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), eq(ProsecutionCase.class)))
                .thenReturn(prosecutionCase);
        when(prosecutionCaseEntity.getPayload()).thenReturn(jsonObject.toString());
        when(objectToJsonObjectConverter.convert(any(ProsecutionCase.class))).thenReturn(jsonObject);
        when(repository.findByCaseId(caseId)).thenReturn(prosecutionCaseEntity);
        
        // When
        eventListener.processDefendantsAddedToCourtProceedingsV2(envelope);
        
        // Then
        verify(repository).save(argumentCaptor.capture());
        // Verify that duplicate offences were filtered - should have only 1 unique offence
        assertThat(defendantWithDuplicateOffences.getOffences().size(), is(1));
    }
}
