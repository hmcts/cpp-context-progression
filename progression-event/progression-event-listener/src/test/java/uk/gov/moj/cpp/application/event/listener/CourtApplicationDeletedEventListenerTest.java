package uk.gov.moj.cpp.application.event.listener;


import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.CourtApplicationHearingDeleted;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourtApplicationDeletedEventListenerTest {

    @Mock
    private HearingApplicationRepository hearingApplicationRepository;

    @Mock
    private CourtApplicationRepository courtApplicationRepository;

    @Mock
    private CourtApplicationCaseRepository courtApplicationCaseRepository;

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private Envelope<CourtApplicationHearingDeleted> envelope;

    @InjectMocks
    private CourtApplicationDeletedEventListener courtApplicationDeletedEventListener;

    @Test
    public void shouldProcessCourtApplicationDeletedEvent() {
        final UUID applicationId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID seedingHearingId = randomUUID();

        when(envelope.payload()).thenReturn(CourtApplicationHearingDeleted.courtApplicationHearingDeleted()
                .withApplicationId(applicationId)
                .withHearingId(hearingId)
                .withSeedingHearingId(seedingHearingId)
                .build()
        );

        courtApplicationDeletedEventListener.processCourtApplicationDeletedEvent(envelope);

        verify(hearingApplicationRepository).removeByApplicationId(applicationId);
        verify(courtApplicationCaseRepository).removeByApplicationId(applicationId);
        verify(courtApplicationRepository).removeByApplicationId(applicationId);
        verify(hearingRepository).removeByHearingId(hearingId);

    }

}
