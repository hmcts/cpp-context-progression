package uk.gov.moj.cpp.progression.processor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.HearingUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import javax.json.JsonObject;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HearingUpdatedEventProcessorTest {

    private ArgumentCaptor<HearingUpdated> argumentCaptor = ArgumentCaptor.forClass(HearingUpdated.class);
    private ArgumentCaptor<JsonEnvelope> argumentCaptorJsonEnvelope = ArgumentCaptor.forClass(JsonEnvelope.class);

    @InjectMocks
    private HearingUpdatedEventProcessor eventProcessor;
    @Mock
    private Sender sender;
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

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldHandleHearingConfirmedEventMessage() throws Exception {
        //Given
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), HearingUpdated.class))
                .thenReturn(hearingUpdated);

        //When
        eventProcessor.publishHearingDetailChangedPublicEvent(envelope);

        //Then
        verify(progressionService, times(1)).updateHearingListingStatusToHearingUpdate(argumentCaptorJsonEnvelope
                .capture(), argumentCaptor.capture());

        HearingUpdated actual = argumentCaptor.getValue();
        assertEquals(hearingUpdated, actual);

        verify(progressionService, times(1)).publishHearingDetailChangedPublicEvent(argumentCaptorJsonEnvelope
                .capture(), argumentCaptor.capture());
        actual = argumentCaptor.getValue();
        assertEquals(hearingUpdated, actual);
    }

}