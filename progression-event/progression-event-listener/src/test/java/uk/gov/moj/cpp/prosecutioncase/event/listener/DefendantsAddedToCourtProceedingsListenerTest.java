package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantsAddedToCourtProceedings;
import uk.gov.justice.core.courts.Hearing;
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

import java.util.Collections;
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
}
