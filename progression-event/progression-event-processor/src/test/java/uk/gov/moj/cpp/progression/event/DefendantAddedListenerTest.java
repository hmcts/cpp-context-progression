package uk.gov.moj.cpp.progression.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.progression.event.DefendantAddedListener.DEFENDANT_ADDED_PUBLIC_EVENT;
import static uk.gov.moj.cpp.progression.event.DefendantAddedListener.DEFENDANT_ADDITION_FAILED_PUBLIC_EVENT;
import static uk.gov.moj.cpp.progression.event.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.progression.event.EventProcessorConstants.DEFENDANT_ID;
import static uk.gov.moj.cpp.progression.event.EventProcessorConstants.DESCRIPTION;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;
import java.util.function.Function;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * 
 * @deprecated This is deprecated for Release 2.4
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
@ExtendWith(MockitoExtension.class)
public class DefendantAddedListenerTest {

    private static final String DUMMY_DESCRIPTION = "some description";
    @InjectMocks
    private DefendantAddedListener listener;

    @Mock
    private Sender sender;
    @Mock
    private Enveloper enveloper;
    @Mock
    private JsonEnvelope jsonEnvelope;
    @Mock
    private JsonObject payload;
    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;
    @Mock
    private JsonEnvelope finalEnvelope;



    @Test
    public void shouldHandleDefendantAddedEventMessage() throws Exception {
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(payload.getString(CASE_ID)).thenReturn(UUID.randomUUID().toString());
        when(payload.getString(DEFENDANT_ID)).thenReturn(UUID.randomUUID().toString());

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(enveloper.withMetadataFrom(jsonEnvelope, DEFENDANT_ADDED_PUBLIC_EVENT)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);

        listener.handleDefendantAddedEvent(jsonEnvelope);
        verify(sender).send(finalEnvelope);
        verify(payload).getString(DEFENDANT_ID);
        verify(payload).getString(CASE_ID);
    }

    @Test
    public void shouldHandleDefendantAdditionFailedEventMessage() throws Exception {
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(payload.getString(CASE_ID)).thenReturn(UUID.randomUUID().toString());
        when(payload.getString(DEFENDANT_ID)).thenReturn(UUID.randomUUID().toString());
        when(payload.getString(DESCRIPTION)).thenReturn(DUMMY_DESCRIPTION);

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(enveloper.withMetadataFrom(jsonEnvelope, DEFENDANT_ADDITION_FAILED_PUBLIC_EVENT)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);

        listener.handleDefendantAdditionFailedEvent(jsonEnvelope);
        verify(sender).send(finalEnvelope);
        verify(payload).getString(CASE_ID);
        verify(payload).getString(DEFENDANT_ID);
        verify(payload).getString(DESCRIPTION);
    }

}