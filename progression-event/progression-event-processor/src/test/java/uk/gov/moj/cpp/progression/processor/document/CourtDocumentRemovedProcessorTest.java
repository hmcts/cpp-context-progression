package uk.gov.moj.cpp.progression.processor.document;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;

import uk.gov.justice.core.courts.CourtsDocumentRemoved;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultJsonMetadata;
import uk.gov.moj.cpp.material.client.MaterialClient;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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

    @Mock
    private SystemUserProvider systemUserProvider;

    @Mock
    private MaterialClient materialClient;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;


    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }
    @Test
    public void shouldProcessRemoveCourtDocumentMessage(){
        when(envelope.payloadAsJsonObject()).thenReturn(courtDocumentRemovedJson);
        when(enveloper.withMetadataFrom(envelope, CourtDocumentRemovedProcessor.PUBLIC_PROGRESSION_EVENTS_COURT_DOCUMENT_REMOVED)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);

        final UUID randomSystemUUID = randomUUID();
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(randomSystemUUID));

        Metadata metadata =  DefaultJsonMetadata.metadataBuilder().withId(randomUUID()).withName("test").withUserId(randomUUID().toString()).build();
        CourtsDocumentRemoved courtsDocumentRemoved = CourtsDocumentRemoved.courtsDocumentRemoved().withCourtDocumentId(randomUUID()).withMaterialId(randomUUID()).withIsRemoved(true).build();
        when(jsonObjectConverter.convert(envelope.payloadAsJsonObject(), CourtsDocumentRemoved.class)).thenReturn(courtsDocumentRemoved);
        //When
        this.eventProcessor.handleCourtDocumentRemovedEvent(envelope);

        //Then
        verify(materialClient, times(1)).removeMaterial(eq(courtsDocumentRemoved.getMaterialId()), eq(randomSystemUUID), any(JsonObject.class));
        verify(sender).send(finalEnvelope);
    }
}
