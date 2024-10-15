package uk.gov.moj.cpp.progression.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.progression.event.DefendantUpdatedListener.DEFENDANT_UPDATED_PUBLIC_EVENT;
import static uk.gov.moj.cpp.progression.event.EventProcessorConstants.CASE_ID;
import static uk.gov.moj.cpp.progression.event.EventProcessorConstants.DEFENDANT_ID;

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
public class DefendantUpdatedListenerTest {

    @InjectMocks
    private DefendantUpdatedListener listener;

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
        when(enveloper.withMetadataFrom(jsonEnvelope, DEFENDANT_UPDATED_PUBLIC_EVENT)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);

        listener.handleDefendantUpdatedEvent(jsonEnvelope);
        verify(sender).send(finalEnvelope);
        verify(payload).getString(DEFENDANT_ID);
        verify(payload).getString(CASE_ID);
    }

}