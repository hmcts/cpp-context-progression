package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.courts.HearingDeleted;
import uk.gov.justice.progression.courts.HearingDeletedForProsecutionCase;
import uk.gov.justice.progression.courts.HearingRemovedForProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.MatchDefendantCaseHearingRepository;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingDeletedEventListenerTest {

    @Mock
    private Envelope<HearingDeleted> hearingDeletedEnvelope;

    @Mock
    private Envelope<HearingDeletedForProsecutionCase> hearingDeletedForProsecutionCaseEnvelope;

    @Mock
    private Envelope<HearingRemovedForProsecutionCase> hearingRemovedForProsecutionCaseEnvelope;

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    @Mock
    private HearingApplicationRepository hearingApplicationRepository;

    @Mock
    private MatchDefendantCaseHearingRepository matchDefendantCaseHearingRepository;

    @Spy
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @Spy
    private final JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @Captor
    private ArgumentCaptor<UUID> defendantArgumentCaptor;

    @Captor
    private ArgumentCaptor<HearingEntity> hearingEntityArgumentCaptor;

    @InjectMocks
    private HearingDeletedEventListener hearingDeletedEventListener;

    @Test
    public void shouldDeleteHearingWhenExistsInViewStore() {
        final UUID hearingIdToBeDeleted = randomUUID();
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingIdToBeDeleted);

        when(hearingDeletedEnvelope.payload()).thenReturn(HearingDeleted.hearingDeleted()
                .withHearingId(hearingIdToBeDeleted)
                .build());
        when(hearingRepository.findBy(hearingIdToBeDeleted)).thenReturn(hearingEntity);

        hearingDeletedEventListener.handleHearingDeletedEvent(hearingDeletedEnvelope);

        verify(hearingRepository).remove(hearingEntity);

        verify(hearingApplicationRepository).removeByHearingId(eq(hearingIdToBeDeleted));
        verify(caseDefendantHearingRepository).removeByHearingId(eq(hearingIdToBeDeleted));
        verify(matchDefendantCaseHearingRepository).removeByHearingId(eq(hearingIdToBeDeleted));
    }

    @Test
    public void shouldNotDeleteHearingWhenHearingNotExistsInViewStore() {
        final UUID hearingIdToBeDeleted = randomUUID();
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingIdToBeDeleted);

        when(hearingDeletedEnvelope.payload()).thenReturn(HearingDeleted.hearingDeleted()
                .withHearingId(hearingIdToBeDeleted)
                .build());
        when(hearingRepository.findBy(hearingIdToBeDeleted)).thenReturn(null);

        hearingDeletedEventListener.handleHearingDeletedEvent(hearingDeletedEnvelope);

        verify(hearingRepository, never()).remove(hearingEntity);
    }

    @Test
    public void shouldDeleteRelatedCasesWhenExistsInViewStore() {
        final UUID hearingIdToBeDeleted = randomUUID();
        final UUID prosecutionCaseIdToBeDeleted = randomUUID();
        final List<UUID> defendantIds = Arrays.asList(randomUUID());

        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingIdToBeDeleted);

        final CaseDefendantHearingKey caseDefendantHearingKey = new CaseDefendantHearingKey();
        caseDefendantHearingKey.setCaseId(prosecutionCaseIdToBeDeleted);

        final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setHearing(hearingEntity);
        caseDefendantHearingEntity.setId(caseDefendantHearingKey);

        when(hearingDeletedForProsecutionCaseEnvelope.payload()).thenReturn(HearingDeletedForProsecutionCase.hearingDeletedForProsecutionCase()
                .withHearingId(hearingIdToBeDeleted)
                .withProsecutionCaseId(prosecutionCaseIdToBeDeleted)
                .withDefendantIds(defendantIds)
                .build());

        hearingDeletedEventListener.handleHearingDeletedForProsecutionCase(hearingDeletedForProsecutionCaseEnvelope);

        verify(caseDefendantHearingRepository, atLeastOnce()).removeByHearingIdAndCaseIdAndDefendantId(hearingIdToBeDeleted, prosecutionCaseIdToBeDeleted, defendantIds.get(0));
        verify(matchDefendantCaseHearingRepository, atLeastOnce()).removeByHearingIdAndCaseIdAndDefendantId(hearingIdToBeDeleted, prosecutionCaseIdToBeDeleted, defendantIds.get(0));
    }

    @Test
    public void shouldRemoveProsecutionCaseFromViewStoreAndHearingPayload_WhenHearingRemovedForProsecutionCase() {
        final UUID hearingId = randomUUID();
        final UUID removedCaseId = randomUUID();
        final UUID remainingCaseId = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withProsecutionCases(List.of(
                        ProsecutionCase.prosecutionCase().withId(removedCaseId).build(),
                        ProsecutionCase.prosecutionCase().withId(remainingCaseId).build()))
                .build();

        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());

        when(hearingRemovedForProsecutionCaseEnvelope.payload()).thenReturn(HearingRemovedForProsecutionCase.hearingRemovedForProsecutionCase()
                .withHearingId(hearingId)
                .withProsecutionCaseId(removedCaseId)
                .build());
        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);

        hearingDeletedEventListener.handleHearingRemovedForProsecutionCase(hearingRemovedForProsecutionCaseEnvelope);

        verify(caseDefendantHearingRepository).removeByHearingIdAndCaseId(hearingId, removedCaseId);

        verify(hearingRepository).save(hearingEntityArgumentCaptor.capture());
        final Hearing updatedHearing = jsonObjectConverter.convert(stringToJsonObjectConverter.convert(hearingEntityArgumentCaptor.getValue().getPayload()), Hearing.class);
        final List<UUID> remainingCaseIds = updatedHearing.getProsecutionCases().stream().map(ProsecutionCase::getId).collect(Collectors.toList());

        assertThat(remainingCaseIds, containsInAnyOrder(remainingCaseId));
        assertThat(remainingCaseIds, not(hasItem(removedCaseId)));
    }

    @Test
    public void shouldNotTouchHearingPayload_WhenHearingNotFound_OnHearingRemovedForProsecutionCase() {
        final UUID hearingId = randomUUID();
        final UUID removedCaseId = randomUUID();

        when(hearingRemovedForProsecutionCaseEnvelope.payload()).thenReturn(HearingRemovedForProsecutionCase.hearingRemovedForProsecutionCase()
                .withHearingId(hearingId)
                .withProsecutionCaseId(removedCaseId)
                .build());
        when(hearingRepository.findBy(hearingId)).thenReturn(null);

        hearingDeletedEventListener.handleHearingRemovedForProsecutionCase(hearingRemovedForProsecutionCaseEnvelope);

        verify(caseDefendantHearingRepository).removeByHearingIdAndCaseId(hearingId, removedCaseId);
        verify(hearingRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
