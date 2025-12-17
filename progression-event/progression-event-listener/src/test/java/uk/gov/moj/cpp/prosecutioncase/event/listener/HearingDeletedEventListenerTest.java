package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.progression.courts.HearingDeleted;
import uk.gov.justice.progression.courts.HearingDeletedForProsecutionCase;
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingDeletedEventListenerTest {

    @Mock
    private Envelope<HearingDeleted> hearingDeletedEnvelope;

    @Mock
    private Envelope<HearingDeletedForProsecutionCase> hearingDeletedForProsecutionCaseEnvelope;

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    @Mock
    private HearingApplicationRepository hearingApplicationRepository;

    @Mock
    private MatchDefendantCaseHearingRepository matchDefendantCaseHearingRepository;

    @Captor
    private ArgumentCaptor<UUID> defendantArgumentCaptor;

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
}
