package uk.gov.moj.cpp.progression.processor.document;

import static java.util.UUID.randomUUID;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.moj.cpp.progression.processor.document.CourtDocumentAddedProcessor.PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtsDocumentAdded;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ReferenceDataService;

import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CourtDocumentAddedProcessorTest {
    @InjectMocks
    private CourtDocumentAddedProcessor eventProcessor;

    @Mock
    private ReferenceDataService refDataService;

    @Mock
    private Sender sender;

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObject courtDocumentAddJson;

    @Mock
    private JsonObject courtDocumentJson;

    @Mock
    private CourtsDocumentAdded courtsDocumentAdded;

    @Mock
    private JsonEnvelope finalEnvelope;

    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;
    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private  ObjectToJsonObjectConverter objectToJsonObjectConverter;


    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }


    @Test
    public void shouldProcessUploadCourtDocumentMessage(){
        when(envelope.payloadAsJsonObject()).thenReturn(courtDocumentAddJson);
        when(jsonObjectToObjectConverter.convert(courtDocumentAddJson,CourtsDocumentAdded.class)).thenReturn(courtsDocumentAdded);
        final CourtDocument courtDocument = buildCourtDocument();
        when(courtsDocumentAdded.getCourtDocument()).thenReturn(courtDocument);
        when(refDataService.getDocumentTypeData(courtDocument.getDocumentTypeId(),envelope)).thenReturn(Optional.of(buildDocumentTypeData()));
        when(objectToJsonObjectConverter.convert(Mockito.any(CourtDocument.class))).thenReturn(courtDocumentJson);
        when(enveloper.withMetadataFrom(envelope, PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);
        //When
        this.eventProcessor.handleCourtDocumentAddEvent(envelope);

        //Then
        verify(sender).send(finalEnvelope);
    }

    private CourtDocument buildCourtDocument(){

        return CourtDocument.courtDocument().withName("SJP notice")
                .withDocumentTypeId(randomUUID()).withCourtDocumentId(randomUUID())
                .withMaterials(Collections.singletonList(Material.material().withId(randomUUID()).build()))
                .build();

    }
    private JsonObject buildDocumentTypeData(){
        return Json.createObjectBuilder().add("documentAccess", Json.createArrayBuilder().add("Listing Officer")).build();
    }
}
