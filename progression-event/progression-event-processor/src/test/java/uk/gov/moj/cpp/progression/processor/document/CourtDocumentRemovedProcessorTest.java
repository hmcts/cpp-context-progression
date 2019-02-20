package uk.gov.moj.cpp.progression.processor.document;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.function.Function;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CourtDocumentRemovedProcessorTest {
    @InjectMocks
    private CourtDocumentRemovedProcessor eventProcessor;

    @Mock
    private Sender sender;

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObject courtDocumentRemovedJson;

    @Mock
    private JsonEnvelope finalEnvelope;

    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }
    @Test
    public void shouldProcessRemoveCourtDocumentMessage(){
        when(envelope.payloadAsJsonObject()).thenReturn(courtDocumentRemovedJson);
        when(enveloper.withMetadataFrom(envelope, CourtDocumentRemovedProcessor.PUBLIC_PROGRESSION_EVENTS_COURT_DOCUMENT_REMOVED)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);
        //When
        this.eventProcessor.handleCourtDocumentRemovedEvent(envelope);

        //Then
        verify(sender).send(finalEnvelope);
    }
}
