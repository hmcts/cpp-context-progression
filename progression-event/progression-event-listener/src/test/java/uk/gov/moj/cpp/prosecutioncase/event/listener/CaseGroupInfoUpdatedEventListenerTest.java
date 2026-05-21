package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.CaseGroupInfoUpdated;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonReader;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseGroupInfoUpdatedEventListenerTest {
    @InjectMocks
    private CaseGroupInfoUpdatedEventListener listener;

    @Mock
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldUpdateCaseGroupInfo() {
        final UUID groupId = randomUUID();
        final UUID caseId = randomUUID();

        List<CaseDefendantHearingEntity> mockCaseDefendantHearingEntities = getMockCaseDefendantHearingEntity(groupId, caseId, false);
        when(caseDefendantHearingRepository.findByCaseId(caseId)).thenReturn(mockCaseDefendantHearingEntities);

        final CaseGroupInfoUpdated event = getEvent(groupId, caseId, false, false);

        listener.processEvent(envelopeFrom(metadataWithRandomUUID("progression.event.case-group-info-updated"),
                objectToJsonObjectConverter.convert(event)));

        final ArgumentCaptor<ProsecutionCaseEntity> caseArgumentCaptor = ArgumentCaptor.forClass(ProsecutionCaseEntity.class);
        verify(this.prosecutionCaseRepository).save(caseArgumentCaptor.capture());

        final ArgumentCaptor<HearingEntity> hearingArgumentCaptor = ArgumentCaptor.forClass(HearingEntity.class);
        verify(this.hearingRepository).save(hearingArgumentCaptor.capture());

        final ProsecutionCaseEntity caseEntity = caseArgumentCaptor.getValue();
        final ProsecutionCase savedCase = jsonObjectToObjectConverter.convert(jsonFromString(caseEntity.getPayload()), ProsecutionCase.class);

        assertThat(caseEntity.getCaseId(), is(caseId));
        assertThat(savedCase.getId(), is(caseId));
        assertThat(savedCase.getGroupId(), is(groupId));
        assertThat(savedCase.getIsGroupMaster(), is(false));
        assertThat(savedCase.getIsGroupMember(), is(false));

        final HearingEntity hearingEntity = hearingArgumentCaptor.getValue();
        final Hearing savedHaring = jsonObjectToObjectConverter.convert(jsonFromString(hearingEntity.getPayload()), Hearing.class);

        assertThat(hearingEntity.getHearingId(), is(mockCaseDefendantHearingEntities.get(0).getHearing().getHearingId()));
        assertThat(savedHaring.getProsecutionCases().get(0).getId(), is(caseId));
        assertThat(savedHaring.getProsecutionCases().get(0).getGroupId(), is(groupId));
        assertThat(savedHaring.getProsecutionCases().get(0).getIsGroupMaster(), is(false));
        assertThat(savedHaring.getProsecutionCases().get(0).getIsGroupMember(), is(false));
    }

    private CaseGroupInfoUpdated getEvent(final UUID groupId, final UUID caseId, final Boolean isGroupMaster, final Boolean isGroupMember) {
        return CaseGroupInfoUpdated.caseGroupInfoUpdated()
                .withProsecutionCase(ProsecutionCase.prosecutionCase()
                        .withId(caseId)
                        .withGroupId(groupId)
                        .withIsGroupMember(isGroupMember)
                        .withIsGroupMaster(isGroupMaster)
                        .build())
                .build();
    }

    private static JsonObject jsonFromString(String jsonObjectStr) {
        final JsonReader jsonReader = JsonObjects.createReader(new StringReader(jsonObjectStr));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();
        return object;
    }

    private List<CaseDefendantHearingEntity> getMockCaseDefendantHearingEntity(final UUID groupId, final UUID caseId, final Boolean isGroupMaster) {
        List<CaseDefendantHearingEntity> caseEntities = new ArrayList<>();
        final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();

        final UUID hearingId = randomUUID();
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);

        List<ProsecutionCase> prosecutionCases = new ArrayList<>();
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withIsCivil(true)
                .withGroupId(groupId)
                .withIsGroupMember(true)
                .withIsGroupMaster(isGroupMaster)
                .withCaseStatus("status")
                .build();
        prosecutionCases.add(prosecutionCase);

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withProsecutionCases(prosecutionCases)
                .build();
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());

        CaseDefendantHearingKey caseDefendantHearingKey = new CaseDefendantHearingKey();
        caseDefendantHearingKey.setCaseId(caseId);
        caseDefendantHearingKey.setHearingId(hearingId);
        caseDefendantHearingKey.setDefendantId(randomUUID());

        caseDefendantHearingEntity.setId(caseDefendantHearingKey);
        caseDefendantHearingEntity.setHearing(hearingEntity);
        caseEntities.add(caseDefendantHearingEntity);

        return caseEntities;
    }
}