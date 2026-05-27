package uk.gov.moj.cpp.progression.processor;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.progression.domain.constant.OnlinePleaNotificationType.COMPANY_FINANCE_DATA;
import static uk.gov.moj.cpp.progression.domain.constant.OnlinePleaNotificationType.COMPANY_ONLINE_PLEA;
import static uk.gov.moj.cpp.progression.domain.constant.OnlinePleaNotificationType.INDIVIDUAL_FINANCE_DATA;
import static uk.gov.moj.cpp.progression.domain.constant.OnlinePleaNotificationType.INDIVIDUAL_ONLINE_PLEA;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.sender.Sender;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import uk.gov.moj.cpp.progression.blobstore.AzureBlobConfiguration;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.progression.events.FinanceDocumentForOnlinePleaSubmitted;
import uk.gov.moj.cpp.progression.events.PleaDocumentForOnlinePleaSubmitted;
import uk.gov.moj.cpp.progression.plea.json.schemas.PleadOnline;
import uk.gov.moj.cpp.progression.service.MaterialService;

import java.nio.charset.Charset;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OnlinePleaEventProcessorTest {

    public static final String FINANCE_DOCUMENT_FOR_ONLINE_PLEA_SUBMITTED = "progression.event.finance-document-for-online-plea-submitted";
    public static final String PLEA_DOCUMENT_FOR_ONLINE_PLEA_SUBMITTED = "progression.event.plea-document-for-online-plea-submitted";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm a");
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new JsonObjectConvertersFactory().objectToJsonObjectConverter();

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private BlobClient blobClient;

    @Mock
    private BlobProperties blobProperties;

    @Mock
    private AzureBlobConfiguration configuration;

    @Mock
    private SystemUserProvider systemUserProvider;

    @Mock
    private Sender sender;

    @Mock
    private MaterialService materialService;

    @Mock
    private SystemUserProvider userProvider;

    @InjectMocks
    private OnlinePleaEventProcessor onlinePleaEventProcessor;

    private PleadOnline personDefendantPleadOnline;
    private PleadOnline legalEntityDefendantPleadOnline;
    private final UUID caseId = randomUUID();
    private final UUID systemUserId = randomUUID();
    private final UUID fileId = randomUUID();

    public static JsonObject getPayload(final String path) {
        String response = null;
        try {
            response = Resources.toString(Resources.getResource(path), Charset.defaultCharset()
            );
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return new StringToJsonObjectConverter().convert(response);
    }

    @BeforeEach
    public void setup() {
        personDefendantPleadOnline = new JsonObjectToObjectConverter(objectMapper).convert(getPayload("person-defendant-online-plea-payload.json"), PleadOnline.class);
        legalEntityDefendantPleadOnline = new JsonObjectToObjectConverter(objectMapper).convert(getPayload("legal-entity-defendant-online-plea-payload.json"), PleadOnline.class);
    }

    @Test
    public void shouldGenerateIndividualOnlinePleaDocument() {

        final PleaDocumentForOnlinePleaSubmitted pleaDocumentForOnlinePleaSubmitted = PleaDocumentForOnlinePleaSubmitted.pleaDocumentForOnlinePleaSubmitted()
                .withCaseId(caseId)
                .withPleadOnline(personDefendantPleadOnline)
                .withDateOfPlea(DATE_FORMATTER.format(LocalDate.now()))
                .withTimeOfPlea(TIME_FORMATTER.format(LocalTime.now()))
                .build();

        final JsonObject jsonObject = objectToJsonObjectConverter.convert(pleaDocumentForOnlinePleaSubmitted);

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID(PLEA_DOCUMENT_FOR_ONLINE_PLEA_SUBMITTED),
                jsonObject);

        final ArgumentCaptor<String> blobNameCaptor = ArgumentCaptor.forClass(String.class);
        when(blobContainerClient.getBlobClient(blobNameCaptor.capture())).thenReturn(blobClient);
        when(configuration.getTransferTimeout()).thenReturn(Duration.ofSeconds(300));

        onlinePleaEventProcessor.generateOnlinePleaDocument(requestMessage);

        verify(blobClient).uploadWithResponse(any(BlobParallelUploadOptions.class), any(), any());
        final UUID capturedFileId = UUID.fromString(blobNameCaptor.getValue());
        ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).sendAsAdmin(captor.capture());
        assertCommonProps(captor, INDIVIDUAL_ONLINE_PLEA.getDescription(), INDIVIDUAL_ONLINE_PLEA.getDescription(), caseId, capturedFileId);
    }

    private void assertCommonProps(ArgumentCaptor<Envelope> captor, String individualOriginatingSource, String individualOnlinePleaTemplate, UUID caseId, UUID fileId) {
        assertThat(captor.getValue().metadata().name(), is("systemdocgenerator.generate-document"));
        assertThat(objectToJsonObjectConverter.convert(captor.getValue().payload()).getString("originatingSource"), is(individualOriginatingSource));
        assertThat(objectToJsonObjectConverter.convert(captor.getValue().payload()).getString("templateIdentifier"), is(individualOnlinePleaTemplate));
        assertThat(objectToJsonObjectConverter.convert(captor.getValue().payload()).getString("conversionFormat"), is("pdf"));
        assertThat(objectToJsonObjectConverter.convert(captor.getValue().payload()).getString("sourceCorrelationId"), is(caseId.toString()));
        assertThat(objectToJsonObjectConverter.convert(captor.getValue().payload()).getString("payloadFileServiceId"), is(fileId.toString()));
    }

    @Test
    public void shouldGenerateCompanyOnlinePleaDocument() {

        final PleaDocumentForOnlinePleaSubmitted pleaDocumentForOnlinePleaSubmitted = PleaDocumentForOnlinePleaSubmitted.pleaDocumentForOnlinePleaSubmitted()
                .withCaseId(caseId)
                .withPleadOnline(legalEntityDefendantPleadOnline)
                .withDateOfPlea(DATE_FORMATTER.format(LocalDate.now()))
                .withTimeOfPlea(TIME_FORMATTER.format(LocalTime.now()))
                .build();

        final JsonObject jsonObject = objectToJsonObjectConverter.convert(pleaDocumentForOnlinePleaSubmitted);

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID(PLEA_DOCUMENT_FOR_ONLINE_PLEA_SUBMITTED),
                jsonObject);

        final ArgumentCaptor<String> blobNameCaptor = ArgumentCaptor.forClass(String.class);
        when(blobContainerClient.getBlobClient(blobNameCaptor.capture())).thenReturn(blobClient);
        when(configuration.getTransferTimeout()).thenReturn(Duration.ofSeconds(300));

        onlinePleaEventProcessor.generateOnlinePleaDocument(requestMessage);

        verify(blobClient).uploadWithResponse(any(BlobParallelUploadOptions.class), any(), any());
        final UUID capturedFileId = UUID.fromString(blobNameCaptor.getValue());
        ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).sendAsAdmin(captor.capture());
        assertCommonProps(captor, COMPANY_ONLINE_PLEA.getDescription(), COMPANY_ONLINE_PLEA.getDescription(), caseId, capturedFileId);
    }

    @Test
    public void shouldGenerateCompanyFinanceDocument() {

        final FinanceDocumentForOnlinePleaSubmitted pleaDocumentForOnlinePleaSubmitted = FinanceDocumentForOnlinePleaSubmitted.financeDocumentForOnlinePleaSubmitted()
                .withCaseId(caseId)
                .withPleadOnline(legalEntityDefendantPleadOnline)
                .withDateOfPlea(DATE_FORMATTER.format(LocalDate.now()))
                .withTimeOfPlea(TIME_FORMATTER.format(LocalTime.now()))
                .build();

        final JsonObject jsonObject = objectToJsonObjectConverter.convert(pleaDocumentForOnlinePleaSubmitted);

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID(FINANCE_DOCUMENT_FOR_ONLINE_PLEA_SUBMITTED),
                jsonObject);

        final ArgumentCaptor<String> blobNameCaptor = ArgumentCaptor.forClass(String.class);
        when(blobContainerClient.getBlobClient(blobNameCaptor.capture())).thenReturn(blobClient);
        when(configuration.getTransferTimeout()).thenReturn(Duration.ofSeconds(300));

        onlinePleaEventProcessor.generateFinanceOnlinePleaDocument(requestMessage);

        verify(blobClient).uploadWithResponse(any(BlobParallelUploadOptions.class), any(), any());
        final UUID capturedFileId = UUID.fromString(blobNameCaptor.getValue());
        ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).sendAsAdmin(captor.capture());
        assertCommonProps(captor, COMPANY_FINANCE_DATA.getDescription(), COMPANY_FINANCE_DATA.getDescription(), caseId, capturedFileId);
    }

    @Test
    public void shouldGenerateIndividualFinanceDocument() {

        final FinanceDocumentForOnlinePleaSubmitted pleaDocumentForOnlinePleaSubmitted = FinanceDocumentForOnlinePleaSubmitted.financeDocumentForOnlinePleaSubmitted()
                .withCaseId(caseId)
                .withPleadOnline(personDefendantPleadOnline)
                .withDateOfPlea(DATE_FORMATTER.format(LocalDate.now()))
                .withTimeOfPlea(TIME_FORMATTER.format(LocalTime.now()))
                .build();

        final JsonObject jsonObject = objectToJsonObjectConverter.convert(pleaDocumentForOnlinePleaSubmitted);

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID(FINANCE_DOCUMENT_FOR_ONLINE_PLEA_SUBMITTED),
                jsonObject);

        final ArgumentCaptor<String> blobNameCaptor = ArgumentCaptor.forClass(String.class);
        when(blobContainerClient.getBlobClient(blobNameCaptor.capture())).thenReturn(blobClient);
        when(configuration.getTransferTimeout()).thenReturn(Duration.ofSeconds(300));

        onlinePleaEventProcessor.generateFinanceOnlinePleaDocument(requestMessage);

        verify(blobClient).uploadWithResponse(any(BlobParallelUploadOptions.class), any(), any());
        final UUID capturedFileId = UUID.fromString(blobNameCaptor.getValue());
        ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).sendAsAdmin(captor.capture());
        assertCommonProps(captor, INDIVIDUAL_FINANCE_DATA.getDescription(), INDIVIDUAL_FINANCE_DATA.getDescription(), caseId, capturedFileId);
    }

    @Test
    public void shouldProcessOnlinePleaDocumentUpload() {
        final UUID fileId = randomUUID();
        final UUID materialId = randomUUID();
        final UUID systemUserId = randomUUID();
        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.events.online-plea-document-uploaded-as-case-material"),
                createObjectBuilder()
                        .add("fileId", fileId.toString())
                        .add("materialId", materialId.toString())
                        .add("caseId", randomUUID().toString())
                        .add("pleaNotificationType", "IndividualFinanceData")
                        .build());

        when(userProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserId));
        when(blobContainerClient.getBlobClient(fileId.toString())).thenReturn(blobClient);
        when(blobClient.getProperties()).thenReturn(blobProperties);
        when(blobProperties.getMetadata()).thenReturn(Map.of("fileName", "documentFileName"));

        onlinePleaEventProcessor.processOnlinePleaMaterialUploadRequest(event);

        verify(materialService).uploadMaterial(fileId, materialId, systemUserId);
    }

    @Test
    public void shouldGenerateCompanyFinanceDocumentWhenLegalEntityFinancialMeansIsNotPresent() {

        final PleadOnline legalEntityDefendantPleadOnline1 = new JsonObjectToObjectConverter(objectMapper).convert(getPayload("legal-entity-defendant-online-plea-payload1.json"), PleadOnline.class);
        final FinanceDocumentForOnlinePleaSubmitted pleaDocumentForOnlinePleaSubmitted = FinanceDocumentForOnlinePleaSubmitted.financeDocumentForOnlinePleaSubmitted()
                .withCaseId(caseId)
                .withPleadOnline(legalEntityDefendantPleadOnline1)
                .withDateOfPlea(DATE_FORMATTER.format(LocalDate.now()))
                .withTimeOfPlea(TIME_FORMATTER.format(LocalTime.now()))
                .build();

        final JsonObject jsonObject = objectToJsonObjectConverter.convert(pleaDocumentForOnlinePleaSubmitted);

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID(FINANCE_DOCUMENT_FOR_ONLINE_PLEA_SUBMITTED),
                jsonObject);

        final ArgumentCaptor<String> blobNameCaptor = ArgumentCaptor.forClass(String.class);
        when(blobContainerClient.getBlobClient(blobNameCaptor.capture())).thenReturn(blobClient);
        when(configuration.getTransferTimeout()).thenReturn(Duration.ofSeconds(300));

        onlinePleaEventProcessor.generateFinanceOnlinePleaDocument(requestMessage);

        verify(blobClient).uploadWithResponse(any(BlobParallelUploadOptions.class), any(), any());
        final UUID capturedFileId = UUID.fromString(blobNameCaptor.getValue());
        ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).sendAsAdmin(captor.capture());
        assertCommonProps(captor, COMPANY_FINANCE_DATA.getDescription(), COMPANY_FINANCE_DATA.getDescription(), caseId, capturedFileId);
    }
}