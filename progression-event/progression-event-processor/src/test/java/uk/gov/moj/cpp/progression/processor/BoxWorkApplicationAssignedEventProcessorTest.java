package uk.gov.moj.cpp.progression.processor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.function.Function;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class BoxWorkApplicationAssignedEventProcessorTest {

    @InjectMocks
    private BoxWorkApplicationAssignedEventProcessor boxWorkApplicationAssignedEventProcessor;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private JsonObject payload;

    @Mock
    private Sender sender;

    @Mock
    private JsonEnvelope finalEnvelope;

    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Mock
    private Logger logger;

    @Test
    public void shouldHandleProcessBoxWAssignmentChanged(){
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);
        when(enveloper.withMetadataFrom(jsonEnvelope, "public.progression.boxwork-assignment-changed")).thenReturn(enveloperFunction);

        boxWorkApplicationAssignedEventProcessor.processBoxWAssignmentChanged(jsonEnvelope);

        verify(sender).send(finalEnvelope);
    }
}
