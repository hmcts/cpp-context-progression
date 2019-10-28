package uk.gov.moj.cpp.progression.processor.document;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.moj.cpp.progression.processor.document.CourtDocumentAddedProcessor.PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtsDocumentAdded;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.service.ReferenceDataService;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
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
    private final Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private JsonObject courtDocumentAddJson;


    @Mock
    private JsonObject courtDocumentJson;

    @Mock
    private CourtsDocumentAdded courtsDocumentAdded;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;


    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @Test
    public void shouldProcessUploadCourtDocumentMessage() {

        JsonObject courtDocumentPayload = buildDocumentCategoryJsonObject();

        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-document-added"),
                courtDocumentPayload);

        CourtDocument courtDocumentPostTransformation = buildCourtDocument();
        when(jsonObjectToObjectConverter.convert(requestMessage.payloadAsJsonObject(), CourtsDocumentAdded.class))
                .thenReturn(courtsDocumentAdded);
        when(courtsDocumentAdded.getCourtDocument()).thenReturn(courtDocumentPostTransformation);
        when(refDataService.getDocumentTypeData(courtDocumentPostTransformation.getDocumentTypeId(), requestMessage))
                .thenReturn(Optional.of(buildDocumentTypeData()));
        when(objectToJsonObjectConverter.convert(Mockito.any(CourtDocument.class))).thenReturn(courtDocumentPayload);

        eventProcessor.handleCourtDocumentAddEvent(requestMessage);
        verify(sender, times(1)).send(envelopeCaptor.capture());

        Envelope<JsonObject> command = envelopeCaptor.getValue();
        final List<Envelope<JsonObject>> commands = envelopeCaptor.getAllValues();
        assertThat(commands.get(0).metadata(),
                withMetadataEnvelopedFrom(requestMessage).withName(PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT));
        JsonObject commandCreateCourtDocumentPayload = commands.get(0).payload();
        //This is an Error Payload Structure that is actually returned....
        assertFalse(commandCreateCourtDocumentPayload.getJsonObject("courtDocument").getJsonObject("courtDocument").getBoolean("containsFinancialMeans"));
    }

    private static JsonObject buildDocumentCategoryJsonObject() {

        final JsonObject documentCategory =
                createObjectBuilder().add("defendantDocument",
                        createObjectBuilder()
                                .add("prosecutionCaseId", "2279b2c3-b0d3-4889-ae8e-1ecc20c39e27")
                                .add("defendants", createArrayBuilder().add("e1d32d9d-29ec-4934-a932-22a50f223966"))).build();

        final JsonObject courtDocument =
                createObjectBuilder().add("courtDocument",
                        createObjectBuilder()
                                .add("courtDocumentId", "2279b2c3-b0d3-4889-ae8e-1ecc20c39e27")
                                .add("documentCategory", documentCategory)
                                .add("name", "SJP Notice")
                                .add("documentTypeId", "0bb7b276-9dc0-4af2-83b9-f4acef0c7898")
                                .add("documentTypeDescription", "SJP Notice")
                                .add("mimeType", "pdf")
                                .add("materials", createObjectBuilder().add("id", "5e1cc18c-76dc-47dd-99c1-d6f87385edf1"))
                                .add("containsFinancialMeans", false))
                        .build();

        return courtDocument;
    }

    private CourtDocument buildCourtDocument() {

        return CourtDocument.courtDocument().withName("SJP notice")
                .withDocumentTypeId(randomUUID()).withCourtDocumentId(randomUUID())
                .withMaterials(Collections.singletonList(Material.material().withId(randomUUID())
                        .withUploadDateTime(ZonedDateTime.now(ZoneOffset.UTC)).build()))
                .withContainsFinancialMeans(false)
                .build();

    }

    private JsonObject buildDocumentTypeData() {
        return Json.createObjectBuilder().add("documentAccess", Json.createArrayBuilder().add("Listing Officer")).build();
    }
}
