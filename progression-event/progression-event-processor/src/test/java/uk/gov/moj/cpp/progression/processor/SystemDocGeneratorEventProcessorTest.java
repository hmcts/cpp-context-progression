package uk.gov.moj.cpp.progression.processor;

import static java.lang.Boolean.FALSE;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonMetadata.NAME;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.client.FileService;
import uk.gov.justice.services.fileservice.domain.FileReference;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonMetadata;
import uk.gov.justice.services.messaging.Metadata;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.stream.JsonParsingException;

import org.apache.http.client.utils.DateUtils;
import org.apache.pdfbox.pdmodel.PDDocument;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.helper.HearingNotificationHelper;
import uk.gov.moj.cpp.progression.service.MaterialService;

@ExtendWith(MockitoExtension.class)
public class SystemDocGeneratorEventProcessorTest {

    public static final String PRISON_COURT_REGISTER_STREAM_ID = "prisonCourtRegisterStreamId";
    public static final String COURT_CENTRE_ID = "courtCentreId";
    public static final String ID = "id";
    public static final String ORIGINATING_SOURCE = "originatingSource";
    public static final String TEMPLATE_IDENTIFIER = "templateIdentifier";
    public static final String CONVERSION_FORMAT = "conversionFormat";
    public static final String SOURCE_CORRELATION_ID = "sourceCorrelationId";
    public static final String PAYLOAD_FILE_SERVICE_ID = "payloadFileServiceId";
    public static final String DOCUMENT_FILE_SERVICE_ID = "documentFileServiceId";
    public static final String ADDITIONAL_INFORMATION = "additionalInformation";
    public static final String PROPERTY_NAME = "propertyName";
    public static final String PROPERTY_VALUE = "propertyValue";


    @InjectMocks
    private SystemDocGeneratorEventProcessor systemDocGeneratorEventProcessor;

    @Mock
    private JsonEnvelope envelope;
    @Mock
    private MaterialService materialService;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private Sender sender;

    @Mock
    private FileService fileService;

    @Mock
    private SystemUserProvider userProvider;

    @Spy
    private HearingNotificationHelper hearingNotificationHelper;

    @Spy
    private UtcClock utcClock;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @BeforeEach
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

    @Test
    public void shouldThrowExceptionOnHandleDocument() throws FileServiceException, IOException {
        final UUID courtCentreId = UUID.randomUUID();
        final JsonObject docPayload = documentAvailablePayload(UUID.randomUUID(), "OEE_Layout5", courtCentreId.toString(), UUID.randomUUID(), "IndividualOnlinePlea");
        when(envelope.payloadAsJsonObject()).thenReturn(docPayload);
        when(fileService.retrieve(any())).thenReturn(java.util.Optional.of(getFileReference()));
        assertThrows(JsonParsingException.class, () -> systemDocGeneratorEventProcessor.handleDocumentAvailable(envelope));
    }

    @Test
    public void shouldProcessPrisonCourtRegisterDocumentAvailable() throws FileServiceException {

        final UUID prisonCourtRegisterStreamId = UUID.randomUUID();
        final UUID fileId = UUID.randomUUID();
        final UUID id = UUID.randomUUID();
        final UUID systemUserId = UUID.randomUUID();

        final JsonArray additionalInfo = createArrayBuilder()
                .add(createObjectBuilder().add(PROPERTY_NAME, "prisonCourtRegisterId").add(PROPERTY_VALUE, id.toString()))
                .add(createObjectBuilder().add(PROPERTY_NAME, "defendantName").add(PROPERTY_VALUE, "Test User"))
                .add(createObjectBuilder().add(PROPERTY_NAME, "caseId").add(PROPERTY_VALUE, randomUUID().toString()))
                .build();

        final JsonObject payload = Json.createObjectBuilder()
                .add(ORIGINATING_SOURCE, "PRISON_COURT_REGISTER")
                .add(TEMPLATE_IDENTIFIER, "OEE_Layout5")
                .add(CONVERSION_FORMAT, "pdf")
                .add(SOURCE_CORRELATION_ID, prisonCourtRegisterStreamId.toString())
                .add(PAYLOAD_FILE_SERVICE_ID, fileId.toString())
                .add(DOCUMENT_FILE_SERVICE_ID, UUID.randomUUID().toString())
                .add(ADDITIONAL_INFORMATION, additionalInfo)
                .build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("public.systemdocgenerator.events.document-available"),
                payload);

        when(userProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserId));

        systemDocGeneratorEventProcessor.handleDocumentAvailable(jsonEnvelope);

        verify(sender, times(2)).send(envelopeCaptor.capture());
        final Envelope<JsonObject> addCourtDocumentCommand = envelopeCaptor.getAllValues().get(0);
        assertThat(addCourtDocumentCommand.metadata().name(),
                equalTo("progression.command.add-court-document"));

        final Envelope<JsonObject> notifyPcrCommand = envelopeCaptor.getAllValues().get(1);
        assertThat(notifyPcrCommand.metadata().name(),
                equalTo("progression.command.notify-prison-court-register"));

        final JsonObject notifyPcrCommandPayload = notifyPcrCommand.payload();
        assertThat(notifyPcrCommandPayload.getString(PRISON_COURT_REGISTER_STREAM_ID), equalTo(prisonCourtRegisterStreamId.toString()));
        assertThat(notifyPcrCommandPayload.containsKey(COURT_CENTRE_ID), is(FALSE));
        assertThat(notifyPcrCommandPayload.getString(ID), equalTo(id.toString()));

    }


    @Test
    public void shouldFailedPrisonCourtRegister() {

        final UUID prisonCourtRegisterStreamId = UUID.randomUUID();

        final UUID fileId = UUID.randomUUID();

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("public.systemdocgenerator.events.generation-failed"),
                Json.createObjectBuilder()
                        .add(ORIGINATING_SOURCE, "PRISON_COURT_REGISTER")
                        .add(TEMPLATE_IDENTIFIER, "OEE_Layout5")
                        .add(CONVERSION_FORMAT, "pdf")
                        .add(SOURCE_CORRELATION_ID, prisonCourtRegisterStreamId.toString())
                        .add(PAYLOAD_FILE_SERVICE_ID, fileId.toString())
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
        assertThat(actualPayload.getString(PRISON_COURT_REGISTER_STREAM_ID), equalTo(prisonCourtRegisterStreamId.toString()));
        assertThat(actualPayload.containsKey(COURT_CENTRE_ID), is(FALSE));
        assertThat(actualPayload.getString("reason"), equalTo("Test Reason"));

    }

    @Test
    public void shouldProcessNowsDocumentAvailableWhenOriginatingSourceIsNows() throws FileServiceException {

        final UUID materialId = UUID.randomUUID();

        final UUID fileId = UUID.randomUUID();

        final UUID systemDocGeneratorId = UUID.randomUUID();

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("public.systemdocgenerator.events.document-available"),
                Json.createObjectBuilder()
                        .add(ORIGINATING_SOURCE, "NOWs")
                        .add(TEMPLATE_IDENTIFIER, "OEE_Layout6")
                        .add(CONVERSION_FORMAT, "pdf")
                        .add(SOURCE_CORRELATION_ID, materialId.toString())
                        .add(PAYLOAD_FILE_SERVICE_ID, fileId.toString())
                        .add(DOCUMENT_FILE_SERVICE_ID, systemDocGeneratorId.toString())
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
    public void shouldProcessNowsFailedToGenerateWhenOriginatingSourceIsNows() {

        final UUID materialId = UUID.randomUUID();

        final UUID fileId = UUID.randomUUID();

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("public.systemdocgenerator.events.generation-failed"),
                Json.createObjectBuilder()
                        .add(ORIGINATING_SOURCE, "NOWs")
                        .add(TEMPLATE_IDENTIFIER, "OEE_Layout6")
                        .add(CONVERSION_FORMAT, "pdf")
                        .add(SOURCE_CORRELATION_ID, materialId.toString())
                        .add(PAYLOAD_FILE_SERVICE_ID, fileId.toString())
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

    @Test
    public void shouldNotProcessNowsDocumentAvailableWhenOriginatingSourceIsNotNows() throws FileServiceException {
        final UUID materialId = UUID.randomUUID();
        final UUID fileId = UUID.randomUUID();
        final UUID systemDocGeneratorId = UUID.randomUUID();
        final JsonEnvelope jsonEnvelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("public.systemdocgenerator.events.document-available"),
                Json.createObjectBuilder()
                        .add(ORIGINATING_SOURCE, "NOWS_DOCUMENTS")
                        .add(TEMPLATE_IDENTIFIER, "OEE_Layout6")
                        .add(CONVERSION_FORMAT, "pdf")
                        .add(SOURCE_CORRELATION_ID, materialId.toString())
                        .add(PAYLOAD_FILE_SERVICE_ID, fileId.toString())
                        .add(DOCUMENT_FILE_SERVICE_ID, systemDocGeneratorId.toString())
                        .build());

        systemDocGeneratorEventProcessor.handleDocumentAvailable(jsonEnvelope);

        verifyNoInteractions(sender);
    }

    @Test
    public void shouldNotProcessNowsFailedToGenerateWhenOriginatingSourceIsNotNows() {
        final UUID materialId = UUID.randomUUID();
        final UUID fileId = UUID.randomUUID();
        final JsonEnvelope jsonEnvelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("public.systemdocgenerator.events.generation-failed"),
                Json.createObjectBuilder()
                        .add(ORIGINATING_SOURCE, "NOWS_DOCUMENTS")
                        .add(TEMPLATE_IDENTIFIER, "OEE_Layout6")
                        .add(CONVERSION_FORMAT, "pdf")
                        .add(SOURCE_CORRELATION_ID, materialId.toString())
                        .add(PAYLOAD_FILE_SERVICE_ID, fileId.toString())
                        .add("requestedTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                        .add("failedTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                        .add("reason", "Test Reason")
                        .build());

        systemDocGeneratorEventProcessor.handleDocumentGenerationFailed(jsonEnvelope);

        verifyNoInteractions(sender);
    }

    private Metadata getMetadataFrom(final String userId, final UUID courtCentreId) {
        return metadataFrom(Json.createObjectBuilder()
                .add("court_register", courtCentreId.toString())
                .add(JsonMetadata.ID, randomUUID().toString())
                .add(HeaderConstants.USER_ID, userId)
                .add(NAME, "public.systemdocgenerator.events.document-available")
                .build()).build();
    }

    private JsonObject documentAvailablePayload(final UUID templatePayloadId, final String templateIdentifier, final String reportId, final UUID generatedDocumentId, final String originatingSource) {
        return Json.createObjectBuilder()
                .add(PAYLOAD_FILE_SERVICE_ID, templatePayloadId.toString())
                .add(TEMPLATE_IDENTIFIER, templateIdentifier)
                .add(CONVERSION_FORMAT, "pdf")
                .add("requestedTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                .add(SOURCE_CORRELATION_ID, reportId)
                .add(ORIGINATING_SOURCE, originatingSource)
                .add(DOCUMENT_FILE_SERVICE_ID, generatedDocumentId.toString())
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