package uk.gov.moj.cpp.progression.processor.document;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.moj.cpp.progression.processor.document.CourtDocumentUploadedProcessor.PUBLIC_COURT_DOCUMENT_UPLOADED;

import uk.gov.justice.core.courts.CourtsDocumentUploaded;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.MaterialService;

import java.util.function.Function;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourtDocumentUploadedProcessorTest {
    @InjectMocks
    private CourtDocumentUploadedProcessor eventProcessor;

    @Mock
    private Sender sender;

    @Mock
    private MaterialService materialService;

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObject courtDocumentUploadJson;

    @Mock
    private CourtsDocumentUploaded courtsDocumentUploaded;

    @Mock
    private JsonEnvelope finalEnvelope;

    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;
    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    @Spy
    @InjectMocks
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }
    @Test
    public void shouldProcessUploadCourtDocumentMessage(){
        when(envelope.payloadAsJsonObject()).thenReturn(courtDocumentUploadJson);
        when(jsonObjectToObjectConverter.convert(courtDocumentUploadJson,CourtsDocumentUploaded.class)).thenReturn(courtsDocumentUploaded);
        when(courtsDocumentUploaded.getFileServiceId()).thenReturn(randomUUID());
        when(courtsDocumentUploaded.getMaterialId() ).thenReturn(randomUUID());
        when(enveloper.withMetadataFrom(envelope, PUBLIC_COURT_DOCUMENT_UPLOADED)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);
        //When
        this.eventProcessor.handleCourtDocumentUploadEvent(envelope);

        //Then
        verify(sender).send(finalEnvelope);
    }
}
