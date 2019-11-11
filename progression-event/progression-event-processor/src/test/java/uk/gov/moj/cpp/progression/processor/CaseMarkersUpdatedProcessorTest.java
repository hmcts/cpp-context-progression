package uk.gov.moj.cpp.progression.processor;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.function.Function;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseMarkersUpdatedProcessorTest {

    @Mock
    private Sender sender;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObject payload;

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;

    @Mock
    private JsonEnvelope finalEnvelope;

    @InjectMocks
    private CaseMarkersUpdatedProcessor processor;

    @Test
    public void processCaseMarkersUpdated() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(enveloper.withMetadataFrom(envelope, "public.progression.case-markers-updated")).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);

        processor.processCaseMarkerUpdated(envelope);

        verify(sender).send(finalEnvelope);
    }

}
