package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.hearing.courts.Initiate;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingConfirmedEventListenerTest {

    @Mock
    private Envelope<Initiate> initiateEnvelope;

    @Mock
    private HearingRepository hearingRepository;

    @Captor
    private ArgumentCaptor<HearingEntity> hearingEntityArgumentCaptor;

    @InjectMocks
    private HearingConfirmedEventListener hearingConfirmedEventListener;

    @Test
    public void shouldProcessHearingInitiatedConfirmDateEvent() {
        final UUID hearingId = randomUUID();
        final HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingId(hearingId);

        when(initiateEnvelope.payload()).thenReturn(Initiate.initiate().withHearing(Hearing.hearing()
                                                                                    .withId(hearingId).build()).build());
        when(hearingRepository.findBy(hearingId)).thenReturn(hearingEntity);

        hearingConfirmedEventListener.processHearingInitiatedEnrichedEvent(initiateEnvelope);

        verify(hearingRepository).save(hearingEntityArgumentCaptor.capture());

        final LocalDate confirmedDate = hearingEntityArgumentCaptor.getValue().getConfirmedDate();

        assertThat(confirmedDate.toString(), is(LocalDate.now().toString()));
    }
}
