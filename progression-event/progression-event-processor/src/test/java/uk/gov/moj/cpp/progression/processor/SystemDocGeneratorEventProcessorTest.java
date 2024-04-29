package uk.gov.moj.cpp.progression.processor;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonMetadata.ID;
import static uk.gov.justice.services.messaging.JsonMetadata.NAME;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.client.FileService;
import uk.gov.justice.services.fileservice.domain.FileReference;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.stream.JsonParsingException;

import org.apache.http.client.utils.DateUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;

@RunWith(MockitoJUnitRunner.class)
public class SystemDocGeneratorEventProcessorTest {

    @InjectMocks
    private SystemDocGeneratorEventProcessor systemDocGeneratorEventProcessor;

    @Mock
    private JsonEnvelope envelope;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private Sender sender;

    @Mock
    private FileService fileService;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @Before
    public void setUp() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldHandleDocumentAvailable() throws FileServiceException {
        final UUID courtCentreId = UUID.randomUUID();
        final JsonObject docPayload = documentAvailablePayload(UUID.randomUUID(), "OEE_Layout5", courtCentreId.toString(), UUID.randomUUID(), "CourtRegister");
        when(envelope.payloadAsJsonObject()).thenReturn(docPayload);
        when(envelope.metadata()).thenReturn(getMetadataFrom(randomUUID().toString(), courtCentreId));
        systemDocGeneratorEventProcessor.handleDocumentAvailable(envelope);

        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonObject> publicEvent = envelopeCaptor.getValue();

        assertThat(publicEvent.metadata().name(), is("progression.command.notify-court-register"));
    }

    @Test(expected = JsonParsingException.class)
    public void shouldThrowExceptionOnHandleDocument() throws FileServiceException, IOException {
        final UUID courtCentreId = UUID.randomUUID();
        final JsonObject docPayload = documentAvailablePayload(UUID.randomUUID(), "OEE_Layout5", courtCentreId.toString(), UUID.randomUUID(), "IndividualOnlinePlea");
        when(envelope.payloadAsJsonObject()).thenReturn(docPayload);
        when(envelope.metadata()).thenReturn(getMetadataFrom(randomUUID().toString(), courtCentreId));
        when(fileService.retrieve(any())).thenReturn(java.util.Optional.of(getFileReference()));
        systemDocGeneratorEventProcessor.handleDocumentAvailable(envelope);
    }

    @Test
    public void shouldProcessPrisonCourtRegisterDocumentAvailable() throws FileServiceException {

        final UUID courtCentreId = UUID.randomUUID();

        final UUID fileId = UUID.randomUUID();

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("public.systemdocgenerator.events.document-available"),
                Json.createObjectBuilder()
                        .add("originatingSource", "PRISON_COURT_REGISTER")
                        .add("templateIdentifier", "OEE_Layout5")
                        .add("conversionFormat", "pdf")
                        .add("sourceCorrelationId", courtCentreId.toString())
                        .add("payloadFileServiceId", fileId.toString())
                        .add("documentFileServiceId", UUID.randomUUID().toString())
                        .build());

        systemDocGeneratorEventProcessor.handleDocumentAvailable(jsonEnvelope);

        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonObject> privateEvent = envelopeCaptor.getValue();

        assertThat(privateEvent.metadata().name(),
                equalTo("progression.command.notify-prison-court-register"));

        final JsonObject actualPayload = privateEvent.payload();
        assertThat(actualPayload.getString("courtCentreId"), equalTo(courtCentreId.toString()));

    }

    @Test
    public void shouldFailedPrisonCourtRegister() {

        final UUID courtCentreId = UUID.randomUUID();

        final UUID fileId = UUID.randomUUID();

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("public.systemdocgenerator.events.generation-failed"),
                Json.createObjectBuilder()
                        .add("originatingSource", "PRISON_COURT_REGISTER")
                        .add("templateIdentifier", "OEE_Layout5")
                        .add("conversionFormat", "pdf")
                        .add("sourceCorrelationId", courtCentreId.toString())
                        .add("payloadFileServiceId", fileId.toString())
                        .add("requestedTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                        .add("failedTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                        .add("reason", "Test Reason")
                        .build());

        systemDocGeneratorEventProcessor.handleDocumentGenerationFailed(jsonEnvelope);

        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonObject> privateEvent = envelopeCaptor.getValue();

        assertThat(privateEvent.metadata().name(),
                equalTo("progression.command.record-prison-court-register-failed"));

        final JsonObject actualPayload = privateEvent.payload();
        assertThat(actualPayload.getString("courtCentreId"), equalTo(courtCentreId.toString()));
        assertThat(actualPayload.getString("reason"), equalTo("Test Reason"));

    }

    @Test
    public void shouldProcessNowsDocumentAvailable() throws FileServiceException {

        final UUID materialId = UUID.randomUUID();

        final UUID fileId = UUID.randomUUID();

        final UUID systemDocGeneratorId = UUID.randomUUID();

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("public.systemdocgenerator.events.document-available"),
                Json.createObjectBuilder()
                        .add("originatingSource", "NOWs")
                        .add("templateIdentifier", "OEE_Layout6")
                        .add("conversionFormat", "pdf")
                        .add("sourceCorrelationId", materialId.toString())
                        .add("payloadFileServiceId", fileId.toString())
                        .add("documentFileServiceId", systemDocGeneratorId.toString())
                        .build());

        systemDocGeneratorEventProcessor.handleDocumentAvailable(jsonEnvelope);

        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonObject> privateEvent = envelopeCaptor.getValue();

        assertThat(privateEvent.metadata().name(),
                equalTo("progression.command.record-nows-document-generated"));

        final JsonObject actualPayload = privateEvent.payload();
        assertThat(actualPayload.getString("materialId"), equalTo(materialId.toString()));
        assertThat(actualPayload.getString("payloadFileId"), equalTo(fileId.toString()));
        assertThat(actualPayload.getString("systemDocGeneratorId"), equalTo(systemDocGeneratorId.toString()));

    }


    @Test
    public void shouldProcessWhenNowsFailedToGenerate() {

        final UUID materialId = UUID.randomUUID();

        final UUID fileId = UUID.randomUUID();

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("public.systemdocgenerator.events.generation-failed"),
                Json.createObjectBuilder()
                        .add("originatingSource", "NOWs")
                        .add("templateIdentifier", "OEE_Layout6")
                        .add("conversionFormat", "pdf")
                        .add("sourceCorrelationId", materialId.toString())
                        .add("payloadFileServiceId", fileId.toString())
                        .add("requestedTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                        .add("failedTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                        .add("reason", "Test Reason")
                        .build());

        systemDocGeneratorEventProcessor.handleDocumentGenerationFailed(jsonEnvelope);

        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonObject> privateEvent = envelopeCaptor.getValue();

        assertThat(privateEvent.metadata().name(),
                equalTo("progression.command.record-nows-document-failed"));

        final JsonObject actualPayload = privateEvent.payload();
        assertThat(actualPayload.getString("materialId"), equalTo(materialId.toString()));
        assertThat(actualPayload.getString("reason"), equalTo("Test Reason"));
        assertThat(actualPayload.getString("payloadFileId"), equalTo(fileId.toString()));

    }

    private Metadata getMetadataFrom(final String userId, final UUID courtCentreId) {
        return metadataFrom(Json.createObjectBuilder()
                .add("court_register", courtCentreId.toString())
                .add(ID, randomUUID().toString())
                .add(HeaderConstants.USER_ID, userId)
                .add(NAME, "public.systemdocgenerator.events.document-available")
                .build()).build();
    }

    private JsonObject documentAvailablePayload(final UUID templatePayloadId, final String templateIdentifier, final String reportId, final UUID generatedDocumentId, final String originatingSource) {
        return Json.createObjectBuilder()
                .add("payloadFileServiceId", templatePayloadId.toString())
                .add("templateIdentifier", templateIdentifier)
                .add("conversionFormat", "pdf")
                .add("requestedTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                .add("sourceCorrelationId", reportId)
                .add("originatingSource", originatingSource)
                .add("documentFileServiceId", generatedDocumentId.toString())
                .add("generatedTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                .add("generateVersion", 1)
                .build();
    }

    private FileReference getFileReference() throws IOException {

        PDDocument pdDocument = new PDDocument();
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        pdDocument.save(outStream);
        InputStream inputStream = new ByteArrayInputStream(outStream.toByteArray());

        final String formatDate = DateUtils.formatDate(new Date());
        final JsonObject metaData = createObjectBuilder()
                .add("fileName",
                        "MaterialFile" + "_" + randomUUID() + "_" + formatDate)
                .build();
        return new FileReference(UUID.randomUUID(), metaData, inputStream);
    }
}