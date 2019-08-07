package uk.gov.moj.cpp.progression.processor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.HearingUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import javax.json.JsonObject;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class HearingUpdatedEventProcessorTest {

    @Captor
    private ArgumentCaptor<HearingUpdated> hearingUpdatedArgumentCaptor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<Hearing> hearingArgumentCaptor;

    @Captor
    private ArgumentCaptor<List<UUID>> applicationIdsArgumentCaptor;

    @Captor
    private ArgumentCaptor<HearingListingStatus> hearingListingStatusArgumentCaptor;

    @InjectMocks
    private HearingUpdatedEventProcessor eventProcessor;

    @Mock
    private HearingUpdated hearingUpdated;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObject payload;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ConfirmedHearing confirmedHearing;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldHandleHearingConfirmedEventMessage() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), HearingUpdated.class))
                .thenReturn(hearingUpdated);

        when(hearingUpdated.getUpdatedHearing()).thenReturn(confirmedHearing);
        when(confirmedHearing.getCourtApplicationIds()).thenReturn(Arrays.asList(UUID.randomUUID()));
        when(confirmedHearing.getProsecutionCases()).thenReturn(Arrays.asList(ConfirmedProsecutionCase.confirmedProsecutionCase().build()));

        eventProcessor.publishHearingDetailChangedPublicEvent(envelope);

        verify(progressionService, times(1)).updateHearingListingStatusToHearingUpdate(envelopeArgumentCaptor.capture(), hearingUpdatedArgumentCaptor.capture());
        assertEquals(hearingUpdated, hearingUpdatedArgumentCaptor.getValue());

        verify(progressionService, times(1)).linkApplicationsToHearing(envelopeArgumentCaptor.capture(), hearingArgumentCaptor.capture(), applicationIdsArgumentCaptor.capture(), hearingListingStatusArgumentCaptor.capture());

        verify(progressionService, times(1)).publishHearingDetailChangedPublicEvent(envelopeArgumentCaptor.capture(), hearingUpdatedArgumentCaptor.capture());
        assertEquals(hearingUpdated, hearingUpdatedArgumentCaptor.getValue());
    }

}