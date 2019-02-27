package uk.gov.moj.cpp.progression.processor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;
import java.util.function.Function;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;


@RunWith(MockitoJUnitRunner.class)
public class ProsecutionCaseOffencesUpdatedProcessorTest {

    @InjectMocks
    private ProsecutionCaseOffencesUpdatedProcessor eventProcessor;

    @Mock
    private Sender sender;

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObject payload;

    @Mock
    private JsonEnvelope finalEnvelope;

    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldHandleProsecutionCaseOffencesUpdatedEventMessage() throws Exception {
        //Given
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(enveloper.withMetadataFrom(envelope, "public.progression.defendant-offences-changed")).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);

        //When
        this.eventProcessor.handleProsecutionCaseOffencesUpdatedEvent(envelope);

        //Then
        verify(sender).send(finalEnvelope);

    }


}