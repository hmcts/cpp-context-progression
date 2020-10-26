package uk.gov.moj.cpp.prosecutioncase.event.listener;


import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import uk.gov.justice.progression.courts.HearingMarkedAsDuplicate;
import uk.gov.justice.progression.courts.HearingMarkedAsDuplicateForCase;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.util.Arrays;
import java.util.UUID;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class HearingMarkedAsDuplicateEventListenerTest {

    @Mock
    private Envelope<HearingMarkedAsDuplicate> envelope;

    @Mock
    private Envelope<HearingMarkedAsDuplicateForCase> envelopeForCase;

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    @Captor
    private ArgumentCaptor<UUID> defendantArgumentCaptor;

    @InjectMocks
    private HearingMarkedAsDuplicateEventListener hearingMarkedAsDuplicateEventListener;


    @Test
    public void shouldDeleteHearingWhenMarkedAsDuplicate() {
        final UUID hearingIdToBeDeleted = UUID.randomUUID();
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingIdToBeDeleted);

        when(envelope.payload()).thenReturn(HearingMarkedAsDuplicate.hearingMarkedAsDuplicate()
                .withHearingId(hearingIdToBeDeleted)
                .build());
        when(hearingRepository.findBy(hearingIdToBeDeleted)).thenReturn(hearingEntity);

        hearingMarkedAsDuplicateEventListener.hearingMarkedAsDuplicate(envelope);

        verify(hearingRepository).remove(hearingEntity);
    }

    @Test
    public void shouldNotDeleteHearingWhenHearingNotExistsInViewStore() {
        final UUID hearingIdToBeDeleted = UUID.randomUUID();
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingIdToBeDeleted);

        when(envelope.payload()).thenReturn(HearingMarkedAsDuplicate.hearingMarkedAsDuplicate()
                .withHearingId(hearingIdToBeDeleted)
                .build());
        when(hearingRepository.findBy(hearingIdToBeDeleted)).thenReturn(null);

        hearingMarkedAsDuplicateEventListener.hearingMarkedAsDuplicate(envelope);

        verify(hearingRepository, never()).remove(hearingEntity);

    }

    @Test
    public void shouldDeleteHearingCaseDefendantWhenMarkedAsDuplicate() {
        final UUID hearingId = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();
        final UUID defendant1Id = UUID.randomUUID();
        final UUID defendant2Id = UUID.randomUUID();

        final CaseDefendantHearingKey caseDefendantHearingKey = new CaseDefendantHearingKey(hearingId, caseId, defendant1Id);
        final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setId(caseDefendantHearingKey);

        when(envelopeForCase.payload()).thenReturn(HearingMarkedAsDuplicateForCase.hearingMarkedAsDuplicateForCase()
                .withHearingId(hearingId)
                .withCaseId(caseId)
                .withDefendantIds(Arrays.asList(defendant1Id, defendant2Id))
                .build());

        doNothing().when(caseDefendantHearingRepository).removeByHearingIdAndCaseIdAndDefendantId(eq(hearingId), eq(caseId), defendantArgumentCaptor.capture());

        hearingMarkedAsDuplicateEventListener.hearingMarkedAsDuplicateForCase(envelopeForCase);

        verify(caseDefendantHearingRepository, times(2)).removeByHearingIdAndCaseIdAndDefendantId(eq(hearingId), eq(caseId), any());
        assertThat(defendantArgumentCaptor.getAllValues().get(0), is(defendant1Id));
        assertThat(defendantArgumentCaptor.getAllValues().get(1), is(defendant2Id));


    }

}
