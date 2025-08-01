package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantPartialMatchCreated;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.events.DefendantMatched;
import uk.gov.moj.cpp.progression.events.DefendantUnmatched;
import uk.gov.moj.cpp.progression.events.DefendantUnmatchedV2;
import uk.gov.moj.cpp.progression.events.DefendantsMasterDefendantIdUpdated;
import uk.gov.moj.cpp.progression.events.MasterDefendantIdUpdated;
import uk.gov.moj.cpp.progression.events.MasterDefendantIdUpdatedV2;
import uk.gov.moj.cpp.progression.events.MatchedDefendants;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.DefendantPartialMatchEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.MatchDefendantCaseHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.DefendantPartialMatchRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.MatchDefendantCaseHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.io.StringReader;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefendantMatchingEventListenerTest {

    @Mock
    private DefendantPartialMatchRepository defendantPartialMatchRepository;

    @Mock
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Mock
    private MatchDefendantCaseHearingRepository matchDefendantCaseHearingRepository;

    @InjectMocks
    private DefendantMatchingEventListener listener;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Captor
    private ArgumentCaptor<ProsecutionCaseEntity> pcEntityCaptor;

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void createDefendantPartialMatch() {
        final UUID defendantId = UUID.randomUUID();
        final UUID prosecutionCaseId = UUID.randomUUID();
        final String defendantName = "Steve Harris Watson";
        final String caseReference = "CASE_REF_1234";
        final String payload = "PAYLOAD";

        final DefendantPartialMatchCreated event = DefendantPartialMatchCreated.defendantPartialMatchCreated()
                .withDefendantId(defendantId)
                .withProsecutionCaseId(prosecutionCaseId)
                .withDefendantName(defendantName)
                .withCaseReference(caseReference)
                .withPayload(payload)
                .build();

        listener.createDefendantPartialMatch(envelopeFrom(metadataWithRandomUUID("progression.event.defendant-partial-match-created"),
                objectToJsonObjectConverter.convert(event)));

        final ArgumentCaptor<DefendantPartialMatchEntity> argumentCaptor = ArgumentCaptor.forClass(DefendantPartialMatchEntity.class);

        verify(this.defendantPartialMatchRepository).save(argumentCaptor.capture());
        final DefendantPartialMatchEntity entity = argumentCaptor.getValue();

        assertThat(entity.getDefendantId(), is(defendantId));
        assertThat(entity.getProsecutionCaseId(), is(prosecutionCaseId));
        assertThat(entity.getDefendantName(), is(defendantName));
        assertThat(entity.getCaseReference(), is(caseReference));
        assertThat(entity.getPayload(), is(payload));
    }

    @Test
    public void deleteDefendantPartialMatch() {
        final UUID defendantId = UUID.randomUUID();
        final DefendantPartialMatchEntity entity = new DefendantPartialMatchEntity();
        entity.setDefendantId(defendantId);

        final DefendantMatched event = DefendantMatched.defendantMatched()
                .withDefendantId(defendantId)
                .withHasDefendantAlreadyBeenDeleted(Boolean.FALSE)
                .build();

        when(defendantPartialMatchRepository.findByDefendantId(defendantId)).thenReturn(entity);
        listener.deleteDefendantPartialMatch(envelopeFrom(metadataWithRandomUUID("progression.event.defendant-partial-match-deleted"),
                objectToJsonObjectConverter.convert(event)));

        final ArgumentCaptor<DefendantPartialMatchEntity> argumentCaptor = ArgumentCaptor.forClass(DefendantPartialMatchEntity.class);

        verify(this.defendantPartialMatchRepository).remove(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getDefendantId(), is(defendantId));
    }

    @Test
    public void deleteDefendantPartialMatchNoAnyDefendantPartialMatchEntityInDB() {
        final UUID defendantId = UUID.randomUUID();
        final DefendantMatched event = DefendantMatched.defendantMatched()
                .withDefendantId(defendantId)
                .withHasDefendantAlreadyBeenDeleted(Boolean.FALSE)
                .build();

        when(defendantPartialMatchRepository.findByDefendantId(defendantId)).thenReturn(null);
        listener.deleteDefendantPartialMatch(envelopeFrom(metadataWithRandomUUID("progression.event.defendant-partial-match-deleted"),
                objectToJsonObjectConverter.convert(event)));

        verify(this.defendantPartialMatchRepository).findByDefendantId(defendantId);
    }

    @Test
    public void unmatchDefendant() {
        final UUID defendantId = UUID.randomUUID();
        final UUID masterDefendantId = UUID.randomUUID();
        final UUID prosecutionCaseId = UUID.randomUUID();

        final MatchDefendantCaseHearingEntity entity = new MatchDefendantCaseHearingEntity();
        entity.setDefendantId(defendantId);
        entity.setMasterDefendantId(masterDefendantId);

        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(prosecutionCaseId);
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId)
                .withDefendants(createDefendants(defendantId))
                .build();
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        when(prosecutionCaseRepository.findByCaseId(prosecutionCaseId)).thenReturn(prosecutionCaseEntity);

        final DefendantUnmatched event = DefendantUnmatched.defendantUnmatched()
                .withDefendantId(defendantId)
                .withProsecutionCaseId(prosecutionCaseId)
                .build();

        when(matchDefendantCaseHearingRepository.findByProsecutionCaseIdAndDefendantId(prosecutionCaseId, defendantId)).thenReturn(Arrays.asList(entity));
        listener.defendantUnmatch(envelopeFrom(metadataWithRandomUUID("progression.event.defendant-unmatched"),
                objectToJsonObjectConverter.convert(event)));

        final ArgumentCaptor<MatchDefendantCaseHearingEntity> argumentCaptor = ArgumentCaptor.forClass(MatchDefendantCaseHearingEntity.class);

        verify(this.matchDefendantCaseHearingRepository).remove(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getDefendantId(), is(defendantId));
    }

    @Test
    public void shouldResetDefendantsMasterDefendantIdOnCase() {
        final UUID defendantId = UUID.randomUUID();
        final UUID masterDefendantId = UUID.randomUUID();
        final UUID prosecutionCaseId = UUID.randomUUID();

        final MatchDefendantCaseHearingEntity entity = new MatchDefendantCaseHearingEntity();
        entity.setDefendantId(defendantId);
        entity.setMasterDefendantId(masterDefendantId);

        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(prosecutionCaseId);

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId)
                .withDefendants(createDefendants(defendantId))
                .build();

        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        when(prosecutionCaseRepository.findByCaseId(prosecutionCaseId)).thenReturn(prosecutionCaseEntity);

        final DefendantUnmatchedV2 event = DefendantUnmatchedV2.defendantUnmatchedV2()
                .withDefendantId(defendantId)
                .withProsecutionCaseId(prosecutionCaseId)
                .withDefendant(Defendant.defendant()
                        .withId(defendantId)
                        .withMasterDefendantId(masterDefendantId)
                        .build())
                .build();

        when(matchDefendantCaseHearingRepository.findByProsecutionCaseIdAndDefendantId(prosecutionCaseId, defendantId)).thenReturn(Arrays.asList(entity));

        listener.defendantUnmatchedV2(envelopeFrom(metadataWithRandomUUID("progression.event.defendant-unmatched-v2"),
                objectToJsonObjectConverter.convert(event)));

        final ArgumentCaptor<MatchDefendantCaseHearingEntity> argumentCaptor = ArgumentCaptor.forClass(MatchDefendantCaseHearingEntity.class);

        verify(this.matchDefendantCaseHearingRepository).remove(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getDefendantId(), is(defendantId));
    }

    @Test
    public void shouldNotUpdateIncomingDefendantWithMasterDefendantIdWhenThereNoMatchedDefendants() {
        final UUID incomingDefendantId = UUID.randomUUID();
        final UUID incomingProsecutionCaseId = UUID.randomUUID();

        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(incomingProsecutionCaseId);
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(incomingProsecutionCaseId)
                .withDefendants(createDefendants(incomingDefendantId))
                .build();
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        final MasterDefendantIdUpdated event = MasterDefendantIdUpdated.masterDefendantIdUpdated()
                .withDefendantId(incomingDefendantId)
                .withProsecutionCaseId(incomingProsecutionCaseId)
                .withMatchedDefendants(new ArrayList<>())
                .build();

        listener.updateMasterDefendantId(envelopeFrom(metadataWithRandomUUID("progression.event.master-defendant-id-updated"),
                objectToJsonObjectConverter.convert(event)));

        final ArgumentCaptor<ProsecutionCaseEntity> prosecutionCaseEntityArgumentCaptor = ArgumentCaptor.forClass(ProsecutionCaseEntity.class);
        verify(this.prosecutionCaseRepository, times(0)).save(prosecutionCaseEntityArgumentCaptor.capture());

        final ArgumentCaptor<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntityArgumentCaptor = ArgumentCaptor.forClass(MatchDefendantCaseHearingEntity.class);
        verify(this.matchDefendantCaseHearingRepository, times(0)).save(matchDefendantCaseHearingEntityArgumentCaptor.capture());
    }

    @Test
    public void shouldUpdateIncomingDefendantWithMasterDefendantIdWhenThereIsOneMasterDefendant() {
        final UUID incomingDefendantId = UUID.randomUUID();
        final UUID incomingProsecutionCaseId = UUID.randomUUID();
        final UUID matchedDefendantId_1 = UUID.randomUUID();
        final UUID matchedProsecutionCaseId_1 = UUID.randomUUID();
        final UUID matchedMasterDefendantId_1 = UUID.randomUUID();
        final ZonedDateTime courtProceedingsInitiatedDate_1 = ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z");
        final UUID matchedDefendantId_2 = UUID.randomUUID();
        final UUID matchedProsecutionCaseId_2 = UUID.randomUUID();
        final ZonedDateTime courtProceedingsInitiatedDate_2 = ZonedDateTimes.fromString("2019-05-23T14:28:11.111Z");

        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(incomingProsecutionCaseId);
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(incomingProsecutionCaseId)
                .withDefendants(createDefendants(incomingDefendantId))
                .build();
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());

        final ProsecutionCaseEntity prosecutionCaseEntityForMatchedDefendant1 = new ProsecutionCaseEntity();
        prosecutionCaseEntityForMatchedDefendant1.setCaseId(matchedProsecutionCaseId_1);
        final ProsecutionCase prosecutionCaseForMatchedDefendant1 = ProsecutionCase.prosecutionCase()
                .withId(matchedProsecutionCaseId_1)
                .withDefendants(createDefendants(matchedDefendantId_1))
                .build();
        prosecutionCaseEntityForMatchedDefendant1.setPayload(objectToJsonObjectConverter.convert(prosecutionCaseForMatchedDefendant1).toString());

        final ProsecutionCaseEntity prosecutionCaseEntityForMatchedDefendant2 = new ProsecutionCaseEntity();
        prosecutionCaseEntityForMatchedDefendant2.setCaseId(matchedProsecutionCaseId_2);
        final ProsecutionCase prosecutionCaseForMatchedDefendant2 = ProsecutionCase.prosecutionCase()
                .withId(matchedProsecutionCaseId_2)
                .withDefendants(createDefendants(matchedDefendantId_2))
                .build();
        prosecutionCaseEntityForMatchedDefendant2.setPayload(objectToJsonObjectConverter.convert(prosecutionCaseForMatchedDefendant2).toString());

        when(prosecutionCaseRepository.findOptionalByCaseId(incomingProsecutionCaseId)).thenReturn(prosecutionCaseEntity);
        when(prosecutionCaseRepository.findOptionalByCaseId(matchedProsecutionCaseId_1)).thenReturn(prosecutionCaseEntityForMatchedDefendant1);
        when(prosecutionCaseRepository.findOptionalByCaseId(matchedProsecutionCaseId_2)).thenReturn(prosecutionCaseEntityForMatchedDefendant2);
        final MasterDefendantIdUpdated event = MasterDefendantIdUpdated.masterDefendantIdUpdated()
                .withDefendantId(incomingDefendantId)
                .withProsecutionCaseId(incomingProsecutionCaseId)
                .withMatchedDefendants(Arrays.asList(
                        MatchedDefendants.matchedDefendants()
                                .withDefendantId(matchedDefendantId_1)
                                .withProsecutionCaseId(matchedProsecutionCaseId_1)
                                .withMasterDefendantId(matchedMasterDefendantId_1)
                                .withCourtProceedingsInitiated(courtProceedingsInitiatedDate_1)
                                .build(),
                        MatchedDefendants.matchedDefendants()
                                .withDefendantId(matchedDefendantId_2)
                                .withProsecutionCaseId(matchedProsecutionCaseId_2)
                                .withMasterDefendantId(matchedMasterDefendantId_1)
                                .withCourtProceedingsInitiated(courtProceedingsInitiatedDate_2)
                                .build()))
                .build();

        listener.updateMasterDefendantId(envelopeFrom(metadataWithRandomUUID("progression.event.master-defendant-id-updated"),
                objectToJsonObjectConverter.convert(event)));

        final ArgumentCaptor<ProsecutionCaseEntity> prosecutionCaseEntityArgumentCaptor = ArgumentCaptor.forClass(ProsecutionCaseEntity.class);
        verify(this.prosecutionCaseRepository, times(3)).save(prosecutionCaseEntityArgumentCaptor.capture());

        final ArgumentCaptor<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntityArgumentCaptor = ArgumentCaptor.forClass(MatchDefendantCaseHearingEntity.class);
        verify(this.matchDefendantCaseHearingRepository, times(3)).save(matchDefendantCaseHearingEntityArgumentCaptor.capture());
    }

    @Test
    public void shouldUpdateIncomingDefendantWithMatchedMasterDefendantId() {
        final UUID incomingDefendantId = UUID.randomUUID();
        final UUID incomingProsecutionCaseId = UUID.randomUUID();
        final UUID matchedDefendantId_1 = UUID.randomUUID();
        final UUID matchedProsecutionCaseId_1 = UUID.randomUUID();
        final UUID matchedMasterDefendantId_1 = UUID.randomUUID();

        final ProsecutionCaseEntity incomingProsecutionCaseEntity = new ProsecutionCaseEntity();
        incomingProsecutionCaseEntity.setCaseId(incomingProsecutionCaseId);
        final ProsecutionCase incomingProsecutionCase = ProsecutionCase.prosecutionCase()
                .withId(incomingProsecutionCaseId)
                .withDefendants(createDefendants(incomingDefendantId))
                .build();
        incomingProsecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(incomingProsecutionCase).toString());
        when(prosecutionCaseRepository.findByCaseId(incomingProsecutionCaseId)).thenReturn(incomingProsecutionCaseEntity);
        when(prosecutionCaseRepository.findOptionalByCaseId(incomingProsecutionCaseId)).thenReturn(incomingProsecutionCaseEntity);

        final ProsecutionCaseEntity matchedProsecutionCaseEntity = new ProsecutionCaseEntity();
        matchedProsecutionCaseEntity.setCaseId(matchedProsecutionCaseId_1);
        final ProsecutionCase matchedProsecutionCase = ProsecutionCase.prosecutionCase()
                .withId(matchedProsecutionCaseId_1)
                .withDefendants(createDefendants(matchedDefendantId_1))
                .build();
        matchedProsecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(matchedProsecutionCase).toString());

        final DefendantsMasterDefendantIdUpdated event = DefendantsMasterDefendantIdUpdated.defendantsMasterDefendantIdUpdated()
                .withProsecutionCaseId(incomingProsecutionCaseId)
                .withProcessInactiveCase(false)
                .withDefendant(Defendant.defendant()
                        .withId(incomingDefendantId)
                        .withMasterDefendantId(matchedMasterDefendantId_1)
                        .build())
                .build();

        listener.defendantsMasterDefendantIdUpdated(envelopeFrom(metadataWithRandomUUID("progression.event.defendants-master-defendant-id-updated"),
                objectToJsonObjectConverter.convert(event)));

        verify(this.prosecutionCaseRepository, times(1)).save(pcEntityCaptor.capture());

        final List<ProsecutionCaseEntity> prosecutionCaseEntities = pcEntityCaptor.getAllValues();

        final ProsecutionCase prosecutionCase_1 = jsonObjectToObjectConverter.convert(jsonFromString(prosecutionCaseEntities.get(0).getPayload()), ProsecutionCase.class);
        assertThat(prosecutionCase_1.getId(), is(incomingProsecutionCaseId));
        assertThat(prosecutionCase_1.getDefendants().get(0).getId(), is(incomingDefendantId));
        assertThat(prosecutionCase_1.getDefendants().get(0).getMasterDefendantId(), is(matchedMasterDefendantId_1));

        final ArgumentCaptor<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntityArgumentCaptor = ArgumentCaptor.forClass(MatchDefendantCaseHearingEntity.class);
        verify(this.matchDefendantCaseHearingRepository, times(1)).save(matchDefendantCaseHearingEntityArgumentCaptor.capture());

        final List<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntities = matchDefendantCaseHearingEntityArgumentCaptor.getAllValues();

        assertThat(matchDefendantCaseHearingEntities.get(0).getProsecutionCaseId(), is(incomingProsecutionCaseId));
        assertThat(matchDefendantCaseHearingEntities.get(0).getDefendantId(), is(incomingDefendantId));
        assertThat(matchDefendantCaseHearingEntities.get(0).getMasterDefendantId(), is(matchedMasterDefendantId_1));
    }

    @Test
    void shouldUpdateMasterDefendantIdOfDefendant() {
        final UUID defendantId = UUID.randomUUID();
        final UUID incomingProsecutionCaseId = UUID.randomUUID();
        final UUID newMasterDefendantId = UUID.randomUUID();

        final ProsecutionCaseEntity incomingProsecutionCaseEntity = new ProsecutionCaseEntity();
        incomingProsecutionCaseEntity.setCaseId(incomingProsecutionCaseId);
        final ProsecutionCase incomingProsecutionCase = ProsecutionCase.prosecutionCase()
                .withId(incomingProsecutionCaseId)
                .withCaseStatus(CaseStatusEnum.CLOSED.toString())
                .withDefendants(createDefendants(defendantId))
                .build();
        incomingProsecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(incomingProsecutionCase).toString());
        when(prosecutionCaseRepository.findByCaseId(incomingProsecutionCaseId)).thenReturn(incomingProsecutionCaseEntity);
        when(prosecutionCaseRepository.findOptionalByCaseId(incomingProsecutionCaseId)).thenReturn(incomingProsecutionCaseEntity);

        listener.defendantsMasterDefendantIdUpdated(envelopeFrom(metadataWithRandomUUID("progression.event.defendants-master-defendant-id-updated"),
                objectToJsonObjectConverter.convert(DefendantsMasterDefendantIdUpdated.defendantsMasterDefendantIdUpdated()
                        .withProsecutionCaseId(incomingProsecutionCaseId)
                        .withProcessInactiveCase(true)
                        .withDefendant(Defendant.defendant()
                                .withId(defendantId)
                                .withMasterDefendantId(newMasterDefendantId)
                                .build())
                        .build())));

        verify(this.prosecutionCaseRepository, times(1)).save(pcEntityCaptor.capture());

        final ProsecutionCaseEntity prosecutionCaseEntity = pcEntityCaptor.getValue();

        final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(jsonFromString(prosecutionCaseEntity.getPayload()), ProsecutionCase.class);
        assertThat(prosecutionCase.getId(), is(incomingProsecutionCaseId));
        assertThat(prosecutionCase.getDefendants().get(0).getId(), is(defendantId));
        assertThat(prosecutionCase.getDefendants().get(0).getMasterDefendantId(), is(newMasterDefendantId));

        final ArgumentCaptor<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntityArgumentCaptor = ArgumentCaptor.forClass(MatchDefendantCaseHearingEntity.class);
        verify(this.matchDefendantCaseHearingRepository, times(1)).save(matchDefendantCaseHearingEntityArgumentCaptor.capture());

        final List<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntities = matchDefendantCaseHearingEntityArgumentCaptor.getAllValues();

        assertThat(matchDefendantCaseHearingEntities.get(0).getProsecutionCaseId(), is(incomingProsecutionCaseId));
        assertThat(matchDefendantCaseHearingEntities.get(0).getDefendantId(), is(defendantId));
        assertThat(matchDefendantCaseHearingEntities.get(0).getMasterDefendantId(), is(newMasterDefendantId));
    }

    @Test
    void shouldUpdateMasterDefendantIdOfDefendantWithExistingMatchDefendantCaseHearing() {
        final UUID defendantId = UUID.randomUUID();
        final UUID prosecutionCaseId = UUID.randomUUID();
        final UUID newMasterDefendantId = UUID.randomUUID();

        final ProsecutionCaseEntity prosecutionCaseEntityDB = new ProsecutionCaseEntity();
        prosecutionCaseEntityDB.setCaseId(prosecutionCaseId);
        final ProsecutionCase incomingProsecutionCase = ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId)
                .withCaseStatus(CaseStatusEnum.CLOSED.toString())
                .withDefendants(createDefendants(defendantId))
                .build();

        final MatchDefendantCaseHearingEntity matchDefendantCaseHearing = new MatchDefendantCaseHearingEntity();
        matchDefendantCaseHearing.setDefendantId(defendantId);
        matchDefendantCaseHearing.setProsecutionCaseId(prosecutionCaseId);
        matchDefendantCaseHearing.setProsecutionCase(prosecutionCaseEntityDB);
        prosecutionCaseEntityDB.setPayload(objectToJsonObjectConverter.convert(incomingProsecutionCase).toString());
        when(prosecutionCaseRepository.findOptionalByCaseId(prosecutionCaseId)).thenReturn(prosecutionCaseEntityDB);
        when(matchDefendantCaseHearingRepository.findByProsecutionCaseIdAndDefendantId(prosecutionCaseId,defendantId)).thenReturn(List.of(matchDefendantCaseHearing));

        listener.defendantsMasterDefendantIdUpdated(envelopeFrom(metadataWithRandomUUID("progression.event.defendants-master-defendant-id-updated"),
                objectToJsonObjectConverter.convert(DefendantsMasterDefendantIdUpdated.defendantsMasterDefendantIdUpdated()
                        .withProsecutionCaseId(prosecutionCaseId)
                        .withProcessInactiveCase(true)
                        .withDefendant(Defendant.defendant()
                                .withId(defendantId)
                                .withMasterDefendantId(newMasterDefendantId)
                                .build())
                        .build())));

        verify(this.prosecutionCaseRepository, times(1)).save(pcEntityCaptor.capture());

        final ProsecutionCaseEntity prosecutionCaseEntity = pcEntityCaptor.getValue();

        final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(jsonFromString(prosecutionCaseEntity.getPayload()), ProsecutionCase.class);
        assertThat(prosecutionCase.getId(), is(prosecutionCaseId));
        assertThat(prosecutionCase.getDefendants().get(0).getId(), is(defendantId));
        assertThat(prosecutionCase.getDefendants().get(0).getMasterDefendantId(), is(newMasterDefendantId));

        final ArgumentCaptor<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntityArgumentCaptor = ArgumentCaptor.forClass(MatchDefendantCaseHearingEntity.class);
        verify(this.matchDefendantCaseHearingRepository, times(1)).save(matchDefendantCaseHearingEntityArgumentCaptor.capture());

        final List<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntities = matchDefendantCaseHearingEntityArgumentCaptor.getAllValues();

        assertThat(matchDefendantCaseHearingEntities.get(0).getProsecutionCaseId(), is(prosecutionCaseId));
        assertThat(matchDefendantCaseHearingEntities.get(0).getDefendantId(), is(defendantId));
        assertThat(matchDefendantCaseHearingEntities.get(0).getMasterDefendantId(), is(newMasterDefendantId));

        verify(this.prosecutionCaseRepository, never()).findByCaseId(any());
    }

    @Test
    void shouldNotUpdateMasterDefendantIdOfInactiveCaseWhenProcessInActiveFalse() {
        final UUID defendantId = UUID.randomUUID();
        final UUID prosecutionCaseId = UUID.randomUUID();
        final UUID newMasterDefendantId = UUID.randomUUID();

        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(prosecutionCaseId);
        final ProsecutionCase incomingProsecutionCase = ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId)
                .withCaseStatus(CaseStatusEnum.INACTIVE.toString())
                .withDefendants(createDefendants(defendantId))
                .build();
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(incomingProsecutionCase).toString());
        when(prosecutionCaseRepository.findOptionalByCaseId(prosecutionCaseId)).thenReturn(prosecutionCaseEntity);

        listener.defendantsMasterDefendantIdUpdated(envelopeFrom(metadataWithRandomUUID("progression.event.defendants-master-defendant-id-updated"),
                objectToJsonObjectConverter.convert(DefendantsMasterDefendantIdUpdated.defendantsMasterDefendantIdUpdated()
                        .withProsecutionCaseId(prosecutionCaseId)
                        .withProcessInactiveCase(false)
                        .withDefendant(Defendant.defendant()
                                .withId(defendantId)
                                .withMasterDefendantId(newMasterDefendantId)
                                .build())
                        .build())));

        verify(this.prosecutionCaseRepository, never()).save(any());
        verify(this.prosecutionCaseRepository, never()).findByCaseId(any());
        verify(this.matchDefendantCaseHearingRepository, never()).save(any());
        verify(this.matchDefendantCaseHearingRepository, never()).findByHearingIdAndProsecutionCaseIdAndDefendantId(any(), any(), any());
    }

    @Test
    void shouldNotUpdateMasterDefendantIdOfClosedCaseWhenProcessInActiveFalse() {
        final UUID defendantId = UUID.randomUUID();
        final UUID prosecutionCaseId = UUID.randomUUID();
        final UUID newMasterDefendantId = UUID.randomUUID();

        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(prosecutionCaseId);
        final ProsecutionCase incomingProsecutionCase = ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId)
                .withCaseStatus(CaseStatusEnum.CLOSED.toString())
                .withDefendants(createDefendants(defendantId))
                .build();
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(incomingProsecutionCase).toString());
        when(prosecutionCaseRepository.findOptionalByCaseId(prosecutionCaseId)).thenReturn(prosecutionCaseEntity);

        listener.defendantsMasterDefendantIdUpdated(envelopeFrom(metadataWithRandomUUID("progression.event.defendants-master-defendant-id-updated"),
                objectToJsonObjectConverter.convert(DefendantsMasterDefendantIdUpdated.defendantsMasterDefendantIdUpdated()
                        .withProsecutionCaseId(prosecutionCaseId)
                        .withProcessInactiveCase(false)
                        .withDefendant(Defendant.defendant()
                                .withId(defendantId)
                                .withMasterDefendantId(newMasterDefendantId)
                                .build())
                        .build())));

        verify(this.prosecutionCaseRepository, never()).save(any());
        verify(this.prosecutionCaseRepository, never()).findByCaseId(any());
        verify(this.matchDefendantCaseHearingRepository, never()).save(any());
        verify(this.matchDefendantCaseHearingRepository, never()).findByHearingIdAndProsecutionCaseIdAndDefendantId(any(), any(), any());
    }

    @Test
    public void shouldUpdateIncomingDefendantAndNonMatchedDefendantWithMasterDefendantIdWhenThereAreTwoMasterDefendants() {
        final UUID incomingDefendantId = UUID.randomUUID();
        final UUID incomingProsecutionCaseId = UUID.randomUUID();
        final UUID matchedDefendantId_1 = UUID.randomUUID();
        final UUID matchedProsecutionCaseId_1 = UUID.randomUUID();
        final UUID matchedMasterDefendantId_1 = UUID.randomUUID();
        final ZonedDateTime courtProceedingsInitiatedDate_1 = ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z");
        final UUID matchedDefendantId_2 = UUID.randomUUID();
        final UUID matchedMasterDefendantId_2 = UUID.randomUUID();
        final ZonedDateTime courtProceedingsInitiatedDate_2 = ZonedDateTimes.fromString("2019-05-23T14:28:11.111Z");

        final ProsecutionCaseEntity incomingProsecutionCaseEntity = new ProsecutionCaseEntity();
        incomingProsecutionCaseEntity.setCaseId(incomingProsecutionCaseId);
        final ProsecutionCase incomingProsecutionCase = ProsecutionCase.prosecutionCase()
                .withId(incomingProsecutionCaseId)
                .withDefendants(createDefendants(incomingDefendantId))
                .build();
        incomingProsecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(incomingProsecutionCase).toString());
        when(prosecutionCaseRepository.findByCaseId(incomingProsecutionCaseId)).thenReturn(incomingProsecutionCaseEntity);
        when(prosecutionCaseRepository.findOptionalByCaseId(incomingProsecutionCaseId)).thenReturn(incomingProsecutionCaseEntity);

        final ProsecutionCaseEntity matchedProsecutionCaseEntity = new ProsecutionCaseEntity();
        matchedProsecutionCaseEntity.setCaseId(matchedProsecutionCaseId_1);
        final ProsecutionCase matchedProsecutionCase = ProsecutionCase.prosecutionCase()
                .withId(matchedProsecutionCaseId_1)
                .withDefendants(createDefendants(matchedDefendantId_1))
                .build();
        matchedProsecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(matchedProsecutionCase).toString());
        when(prosecutionCaseRepository.findByCaseId(matchedProsecutionCaseId_1)).thenReturn(matchedProsecutionCaseEntity);
        when(prosecutionCaseRepository.findOptionalByCaseId(matchedProsecutionCaseId_1)).thenReturn(matchedProsecutionCaseEntity);

        final MasterDefendantIdUpdated event = MasterDefendantIdUpdated.masterDefendantIdUpdated()
                .withDefendantId(incomingDefendantId)
                .withProsecutionCaseId(incomingProsecutionCaseId)
                .withMatchedDefendants(Arrays.asList(
                        MatchedDefendants.matchedDefendants()
                                .withDefendantId(matchedDefendantId_1)
                                .withProsecutionCaseId(matchedProsecutionCaseId_1)
                                .withMasterDefendantId(matchedMasterDefendantId_1)
                                .withCourtProceedingsInitiated(courtProceedingsInitiatedDate_1)
                                .build(),
                        MatchedDefendants.matchedDefendants()
                                .withDefendantId(matchedDefendantId_2)
                                .withProsecutionCaseId(matchedProsecutionCaseId_1)
                                .withMasterDefendantId(matchedMasterDefendantId_2)
                                .withCourtProceedingsInitiated(courtProceedingsInitiatedDate_2)
                                .build()))
                .build();

        listener.updateMasterDefendantId(envelopeFrom(metadataWithRandomUUID("progression.event.master-defendant-id-updated"),
                objectToJsonObjectConverter.convert(event)));

        final ArgumentCaptor<ProsecutionCaseEntity> prosecutionCaseEntityArgumentCaptor = ArgumentCaptor.forClass(ProsecutionCaseEntity.class);
        verify(this.prosecutionCaseRepository, times(3)).save(prosecutionCaseEntityArgumentCaptor.capture());

        final List<ProsecutionCaseEntity> prosecutionCaseEntities = prosecutionCaseEntityArgumentCaptor.getAllValues();

        final ProsecutionCase prosecutionCase_1 = jsonObjectToObjectConverter.convert(jsonFromString(prosecutionCaseEntities.get(0).getPayload()), ProsecutionCase.class);
        assertThat(prosecutionCase_1.getId(), is(incomingProsecutionCaseId));
        assertThat(prosecutionCase_1.getDefendants().get(0).getId(), is(incomingDefendantId));
        assertThat(prosecutionCase_1.getDefendants().get(0).getMasterDefendantId(), is(matchedMasterDefendantId_2));

        final ProsecutionCase prosecutionCase_2 = jsonObjectToObjectConverter.convert(jsonFromString(prosecutionCaseEntities.get(1).getPayload()), ProsecutionCase.class);
        assertThat(prosecutionCase_2.getId(), is(matchedProsecutionCaseId_1));
        assertThat(prosecutionCase_2.getDefendants().get(0).getId(), is(matchedDefendantId_1));
        assertThat(prosecutionCase_2.getDefendants().get(0).getMasterDefendantId(), is(matchedMasterDefendantId_2));

        final ArgumentCaptor<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntityArgumentCaptor = ArgumentCaptor.forClass(MatchDefendantCaseHearingEntity.class);
        verify(this.matchDefendantCaseHearingRepository, times(3)).save(matchDefendantCaseHearingEntityArgumentCaptor.capture());

        final List<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntities = matchDefendantCaseHearingEntityArgumentCaptor.getAllValues();

        assertThat(matchDefendantCaseHearingEntities.get(0).getProsecutionCaseId(), is(incomingProsecutionCaseId));
        assertThat(matchDefendantCaseHearingEntities.get(0).getDefendantId(), is(incomingDefendantId));
        assertThat(matchDefendantCaseHearingEntities.get(0).getMasterDefendantId(), is(matchedMasterDefendantId_2));

    }

    @Test
    public void shouldUpdateIncomingDefendantWithMasterDefendantIdWhenThereIsOneMasterDefendantWithNoCourtProceedingsInitiatedDateOnOneMatchedDefendant() {
        final UUID incomingDefendantId = UUID.randomUUID();
        final UUID incomingProsecutionCaseId = UUID.randomUUID();
        final UUID matchedDefendantId_1 = UUID.randomUUID();
        final UUID matchedProsecutionCaseId_1 = UUID.randomUUID();
        final UUID matchedMasterDefendantId_1 = UUID.randomUUID();
        final UUID matchedDefendantId_2 = UUID.randomUUID();
        final UUID matchedProsecutionCaseId_2 = UUID.randomUUID();
        final ZonedDateTime courtProceedingsInitiatedDate_2 = ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z");

        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(incomingProsecutionCaseId);
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(incomingProsecutionCaseId)
                .withDefendants(createDefendants(incomingDefendantId))
                .build();
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());

        final ProsecutionCaseEntity prosecutionCaseEntityForMatchedDefendant1 = new ProsecutionCaseEntity();
        prosecutionCaseEntityForMatchedDefendant1.setCaseId(matchedProsecutionCaseId_1);
        final ProsecutionCase prosecutionCaseForMatchedDefendant1 = ProsecutionCase.prosecutionCase()
                .withId(matchedProsecutionCaseId_1)
                .withDefendants(createDefendants(matchedDefendantId_1))
                .build();
        prosecutionCaseEntityForMatchedDefendant1.setPayload(objectToJsonObjectConverter.convert(prosecutionCaseForMatchedDefendant1).toString());

        final ProsecutionCaseEntity prosecutionCaseEntityForMatchedDefendant2 = new ProsecutionCaseEntity();
        prosecutionCaseEntityForMatchedDefendant2.setCaseId(matchedProsecutionCaseId_2);
        final ProsecutionCase prosecutionCaseForMatchedDefendant2 = ProsecutionCase.prosecutionCase()
                .withId(matchedProsecutionCaseId_2)
                .withDefendants(createDefendants(matchedDefendantId_2))
                .build();
        prosecutionCaseEntityForMatchedDefendant2.setPayload(objectToJsonObjectConverter.convert(prosecutionCaseForMatchedDefendant2).toString());

        when(prosecutionCaseRepository.findOptionalByCaseId(incomingProsecutionCaseId)).thenReturn(prosecutionCaseEntity);
        when(prosecutionCaseRepository.findOptionalByCaseId(matchedProsecutionCaseId_1)).thenReturn(prosecutionCaseEntityForMatchedDefendant1);
        when(prosecutionCaseRepository.findOptionalByCaseId(matchedProsecutionCaseId_2)).thenReturn(prosecutionCaseEntityForMatchedDefendant2);
        final MasterDefendantIdUpdated event = MasterDefendantIdUpdated.masterDefendantIdUpdated()
                .withDefendantId(incomingDefendantId)
                .withProsecutionCaseId(incomingProsecutionCaseId)
                .withMatchedDefendants(Arrays.asList(
                        MatchedDefendants.matchedDefendants()
                                .withDefendantId(matchedDefendantId_1)
                                .withProsecutionCaseId(matchedProsecutionCaseId_1)
                                .withMasterDefendantId(matchedMasterDefendantId_1)
                                .build(),
                        MatchedDefendants.matchedDefendants()
                                .withDefendantId(matchedDefendantId_2)
                                .withProsecutionCaseId(matchedProsecutionCaseId_2)
                                .withMasterDefendantId(matchedDefendantId_2)
                                .withCourtProceedingsInitiated(courtProceedingsInitiatedDate_2)
                                .build()))
                .build();

        listener.updateMasterDefendantId(envelopeFrom(metadataWithRandomUUID("progression.event.master-defendant-id-updated"),
                objectToJsonObjectConverter.convert(event)));

        final ArgumentCaptor<ProsecutionCaseEntity> prosecutionCaseEntityArgumentCaptor = ArgumentCaptor.forClass(ProsecutionCaseEntity.class);
        verify(this.prosecutionCaseRepository, times(3)).save(prosecutionCaseEntityArgumentCaptor.capture());

        final ArgumentCaptor<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntityArgumentCaptor = ArgumentCaptor.forClass(MatchDefendantCaseHearingEntity.class);
        verify(this.matchDefendantCaseHearingRepository, times(3)).save(matchDefendantCaseHearingEntityArgumentCaptor.capture());
    }


    private List<Defendant> createDefendants(final UUID matchedDefendantId) {
        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(Defendant.defendant()
                .withId(matchedDefendantId)
                .build());
        return defendants;
    }

    private static JsonObject jsonFromString(final String jsonObjectStr) {
        final JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();
        return object;
    }


    @Test
    public void shouldUpdateMasterDefendantId_filterDuplicates() {
        final UUID incomingDefendantId = UUID.randomUUID();
        final UUID incomingProsecutionCaseId = UUID.randomUUID();
        final UUID matchedMasterDefendantId_1 = UUID.randomUUID();
        final ZonedDateTime courtProceedingsInitiatedDate_1 = ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z");

        final ProsecutionCaseEntity incomingProsecutionCaseEntity = new ProsecutionCaseEntity();
        incomingProsecutionCaseEntity.setCaseId(incomingProsecutionCaseId);
        final ProsecutionCase incomingProsecutionCase = ProsecutionCase.prosecutionCase()
                .withId(incomingProsecutionCaseId)
                .withDefendants(createDefendants(incomingDefendantId))
                .build();
        incomingProsecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(incomingProsecutionCase).toString());
        when(prosecutionCaseRepository.findByCaseId(incomingProsecutionCaseId)).thenReturn(incomingProsecutionCaseEntity);
        when(prosecutionCaseRepository.findOptionalByCaseId(incomingProsecutionCaseId)).thenReturn(incomingProsecutionCaseEntity);

        final MasterDefendantIdUpdated event = MasterDefendantIdUpdated.masterDefendantIdUpdated()
                .withDefendantId(incomingDefendantId)
                .withProsecutionCaseId(incomingProsecutionCaseId)
                .withMatchedDefendants(Arrays.asList(
                        MatchedDefendants.matchedDefendants()
                                .withDefendantId(incomingDefendantId)
                                .withProsecutionCaseId(incomingProsecutionCaseId)
                                .withMasterDefendantId(matchedMasterDefendantId_1)
                                .withCourtProceedingsInitiated(courtProceedingsInitiatedDate_1)
                                .build()))
                .build();

        listener.updateMasterDefendantId(envelopeFrom(metadataWithRandomUUID("progression.event.master-defendant-id-updated"),
                objectToJsonObjectConverter.convert(event)));

        final ArgumentCaptor<ProsecutionCaseEntity> prosecutionCaseEntityArgumentCaptor = ArgumentCaptor.forClass(ProsecutionCaseEntity.class);
        verify(this.prosecutionCaseRepository, times(2)).save(prosecutionCaseEntityArgumentCaptor.capture());

        final List<ProsecutionCaseEntity> prosecutionCaseEntities = prosecutionCaseEntityArgumentCaptor.getAllValues();

        final ProsecutionCase prosecutionCase_1 = jsonObjectToObjectConverter.convert(jsonFromString(prosecutionCaseEntities.get(0).getPayload()), ProsecutionCase.class);
        assertThat(prosecutionCase_1.getId(), is(incomingProsecutionCaseId));
        assertThat(prosecutionCase_1.getDefendants().get(0).getId(), is(incomingDefendantId));
        assertThat(prosecutionCase_1.getDefendants().get(0).getMasterDefendantId(), is(matchedMasterDefendantId_1));

        final ArgumentCaptor<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntityArgumentCaptor = ArgumentCaptor.forClass(MatchDefendantCaseHearingEntity.class);
        verify(this.matchDefendantCaseHearingRepository, times(2)).save(matchDefendantCaseHearingEntityArgumentCaptor.capture());

        final List<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntities = matchDefendantCaseHearingEntityArgumentCaptor.getAllValues();

        assertThat(matchDefendantCaseHearingEntities.get(0).getProsecutionCaseId(), is(incomingProsecutionCaseId));
        assertThat(matchDefendantCaseHearingEntities.get(0).getDefendantId(), is(incomingDefendantId));
        assertThat(matchDefendantCaseHearingEntities.get(0).getMasterDefendantId(), is(matchedMasterDefendantId_1));
    }

    @Test
    public void shouldUpdateMasterDefendantIdV2() {
        final UUID incomingDefendantId = UUID.randomUUID();
        final UUID incomingProsecutionCaseId = UUID.randomUUID();
        final UUID matchedMasterDefendantId_1 = UUID.randomUUID();
        final ZonedDateTime courtProceedingsInitiatedDate_1 = ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z");

        final UUID matchedProsecutionCaseId_1 = UUID.randomUUID();
        final UUID matchedDefendantId_1 = UUID.randomUUID();

        final ProsecutionCaseEntity incomingProsecutionCaseEntity = new ProsecutionCaseEntity();
        incomingProsecutionCaseEntity.setCaseId(incomingProsecutionCaseId);
        final ProsecutionCase incomingProsecutionCase = ProsecutionCase.prosecutionCase()
                .withId(incomingProsecutionCaseId)
                .withDefendants(createDefendants(incomingDefendantId))
                .build();
        incomingProsecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(incomingProsecutionCase).toString());
        when(prosecutionCaseRepository.findOptionalByCaseId(incomingProsecutionCaseId)).thenReturn(incomingProsecutionCaseEntity);

        final ProsecutionCaseEntity matchedProsecutionCaseEntity = new ProsecutionCaseEntity();
        matchedProsecutionCaseEntity.setCaseId(matchedProsecutionCaseId_1);
        final ProsecutionCase matchedProsecutionCase = ProsecutionCase.prosecutionCase()
                .withId(matchedProsecutionCaseId_1)
                .withDefendants(createDefendants(matchedDefendantId_1))
                .build();
        matchedProsecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(matchedProsecutionCase).toString());
        when(prosecutionCaseRepository.findByCaseId(matchedProsecutionCaseId_1)).thenReturn(matchedProsecutionCaseEntity);
        when(prosecutionCaseRepository.findOptionalByCaseId(matchedProsecutionCaseId_1)).thenReturn(matchedProsecutionCaseEntity);

        final MasterDefendantIdUpdatedV2 event = MasterDefendantIdUpdatedV2.masterDefendantIdUpdatedV2()
                .withDefendant(Defendant.defendant()
                        .withId(incomingDefendantId)
                        .build())
                .withProsecutionCaseId(incomingProsecutionCaseId)
                .withMatchedDefendants(Arrays.asList(
                        MatchedDefendants.matchedDefendants()
                                .withDefendantId(incomingDefendantId)
                                .withProsecutionCaseId(incomingProsecutionCaseId)
                                .withMasterDefendantId(matchedMasterDefendantId_1)
                                .withCourtProceedingsInitiated(courtProceedingsInitiatedDate_1)
                                .build(),
                        MatchedDefendants.matchedDefendants()
                                .withDefendantId(matchedDefendantId_1)
                                .withProsecutionCaseId(matchedProsecutionCaseId_1)
                                .withMasterDefendantId(matchedMasterDefendantId_1)
                                .withCourtProceedingsInitiated(courtProceedingsInitiatedDate_1)
                                .build()))
                .build();
        final MatchDefendantCaseHearingEntity matchDefendantCaseHearingEntity = new MatchDefendantCaseHearingEntity();
        matchDefendantCaseHearingEntity.setDefendantId(incomingDefendantId);
        matchDefendantCaseHearingEntity.setProsecutionCaseId(incomingProsecutionCaseId);
        when(matchDefendantCaseHearingRepository.findByProsecutionCaseIdAndDefendantId(incomingProsecutionCaseId,incomingDefendantId)).thenReturn(Arrays.asList(matchDefendantCaseHearingEntity));

        listener.updateMasterDefendantIdV2(envelopeFrom(metadataWithRandomUUID("progression.event.master-defendant-id-updated-v2"),
                objectToJsonObjectConverter.convert(event)));

        final ArgumentCaptor<ProsecutionCaseEntity> prosecutionCaseEntityArgumentCaptor = ArgumentCaptor.forClass(ProsecutionCaseEntity.class);
        verify(this.prosecutionCaseRepository, times(3)).save(prosecutionCaseEntityArgumentCaptor.capture());

        final List<ProsecutionCaseEntity> prosecutionCaseEntities = prosecutionCaseEntityArgumentCaptor.getAllValues();

        final ProsecutionCase prosecutionCase_1 = jsonObjectToObjectConverter.convert(jsonFromString(prosecutionCaseEntities.get(0).getPayload()), ProsecutionCase.class);
        assertThat(prosecutionCase_1.getId(), is(incomingProsecutionCaseId));
        assertThat(prosecutionCase_1.getDefendants().get(0).getId(), is(incomingDefendantId));
        assertThat(prosecutionCase_1.getDefendants().get(0).getMasterDefendantId(), is(matchedMasterDefendantId_1));

        final ArgumentCaptor<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntityArgumentCaptor = ArgumentCaptor.forClass(MatchDefendantCaseHearingEntity.class);
        verify(this.matchDefendantCaseHearingRepository, times(3)).save(matchDefendantCaseHearingEntityArgumentCaptor.capture());

        final List<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntities = matchDefendantCaseHearingEntityArgumentCaptor.getAllValues();

        assertThat(matchDefendantCaseHearingEntities.get(0).getProsecutionCaseId(), is(incomingProsecutionCaseId));
        assertThat(matchDefendantCaseHearingEntities.get(0).getDefendantId(), is(incomingDefendantId));
        assertThat(matchDefendantCaseHearingEntities.get(0).getMasterDefendantId(), is(matchedMasterDefendantId_1));
    }
    @Test
    public void shouldUpdateMasterDefendantIdV2_filterDuplicates() {
        final UUID incomingDefendantId = UUID.randomUUID();
        final UUID incomingProsecutionCaseId = UUID.randomUUID();
        final UUID matchedMasterDefendantId_1 = UUID.randomUUID();
        final ZonedDateTime courtProceedingsInitiatedDate_1 = ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z");

        final ProsecutionCaseEntity incomingProsecutionCaseEntity = new ProsecutionCaseEntity();
        incomingProsecutionCaseEntity.setCaseId(incomingProsecutionCaseId);
        final ProsecutionCase incomingProsecutionCase = ProsecutionCase.prosecutionCase()
                .withId(incomingProsecutionCaseId)
                .withDefendants(createDefendants(incomingDefendantId))
                .build();
        incomingProsecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(incomingProsecutionCase).toString());
        when(prosecutionCaseRepository.findByCaseId(incomingProsecutionCaseId)).thenReturn(incomingProsecutionCaseEntity);
        when(prosecutionCaseRepository.findOptionalByCaseId(incomingProsecutionCaseId)).thenReturn(incomingProsecutionCaseEntity);

        final MasterDefendantIdUpdatedV2 event = MasterDefendantIdUpdatedV2.masterDefendantIdUpdatedV2()
                .withDefendant(Defendant.defendant()
                        .withId(incomingDefendantId)
                        .build())
                .withProsecutionCaseId(incomingProsecutionCaseId)
                .withMatchedDefendants(Arrays.asList(
                        MatchedDefendants.matchedDefendants()
                                .withDefendantId(incomingDefendantId)
                                .withProsecutionCaseId(incomingProsecutionCaseId)
                                .withMasterDefendantId(matchedMasterDefendantId_1)
                                .withCourtProceedingsInitiated(courtProceedingsInitiatedDate_1)
                                .build()))
                .build();

        listener.updateMasterDefendantIdV2(envelopeFrom(metadataWithRandomUUID("progression.event.master-defendant-id-updated-v2"),
                objectToJsonObjectConverter.convert(event)));

        final ArgumentCaptor<ProsecutionCaseEntity> prosecutionCaseEntityArgumentCaptor = ArgumentCaptor.forClass(ProsecutionCaseEntity.class);
        verify(this.prosecutionCaseRepository, times(2)).save(prosecutionCaseEntityArgumentCaptor.capture());

        final List<ProsecutionCaseEntity> prosecutionCaseEntities = prosecutionCaseEntityArgumentCaptor.getAllValues();

        final ProsecutionCase prosecutionCase_1 = jsonObjectToObjectConverter.convert(jsonFromString(prosecutionCaseEntities.get(0).getPayload()), ProsecutionCase.class);
        assertThat(prosecutionCase_1.getId(), is(incomingProsecutionCaseId));
        assertThat(prosecutionCase_1.getDefendants().get(0).getId(), is(incomingDefendantId));
        assertThat(prosecutionCase_1.getDefendants().get(0).getMasterDefendantId(), is(matchedMasterDefendantId_1));

        final ArgumentCaptor<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntityArgumentCaptor = ArgumentCaptor.forClass(MatchDefendantCaseHearingEntity.class);
        verify(this.matchDefendantCaseHearingRepository, times(2)).save(matchDefendantCaseHearingEntityArgumentCaptor.capture());

        final List<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntities = matchDefendantCaseHearingEntityArgumentCaptor.getAllValues();

        assertThat(matchDefendantCaseHearingEntities.get(0).getProsecutionCaseId(), is(incomingProsecutionCaseId));
        assertThat(matchDefendantCaseHearingEntities.get(0).getDefendantId(), is(incomingDefendantId));
        assertThat(matchDefendantCaseHearingEntities.get(0).getMasterDefendantId(), is(matchedMasterDefendantId_1));
    }
}
