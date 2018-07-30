package uk.gov.moj.cpp.progression.activiti.workflow.initiatehearing.listener;

import static java.util.UUID.randomUUID;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.DefaultJsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.activiti.service.ActivitiService;


@RunWith(MockitoJUnitRunner.class)
public class HearingInitiatedEventListenerTest {

    @Mock
    private ActivitiService activitiService;


    @InjectMocks
    private HearingInitiatedEventListener hearingInitiatedEventListener;

    @Mock
    private JsonEnvelope event;

    @Mock
    private JsonObject jsonObject;

    private static final String USER_ID = randomUUID().toString();

    @Test
    public void shouldSignalProcessWithActivitiIdIfHearingIdPresent() {
        //Given
        final String hearingId = UUID.randomUUID().toString();
        when(event.payloadAsJsonObject()).thenReturn(jsonObject);
        when(jsonObject.containsKey("hearingId")).thenReturn(true);
        when(event.payloadAsJsonObject().getString("hearingId")).thenReturn(hearingId);


        //when
        hearingInitiatedEventListener.processEvent(event);
        //then
        verify(activitiService).signalProcessByActivitiIdAndFieldName(eq("recieveHearingInitiatedConfirmation"), eq("hearingId"), eq(hearingId));
    }

    @Test
    public void shouldNotSignalProcessWithActivitiIdIfHearingIdNotPresent() {
        //Given
        final String fakeId = UUID.randomUUID().toString();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("fakeId", fakeId)
                .build();
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUID("public.hearing.initiated")
                .withUserId(USER_ID), jsonObject);

        //when
        hearingInitiatedEventListener.processEvent(jsonEnvelope);
        //then
        verify(activitiService, never()).signalProcessByActivitiIdAndFieldName(any(), any(), any());
    }

}
