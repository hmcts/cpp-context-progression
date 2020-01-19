package uk.gov.moj.cpp.progression.service;

import static java.util.UUID.fromString;
import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.core.courts.nces.NcesNotificationRequested;
import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.core.courts.nowdocument.NowDocumentRequest;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.activiti.common.JsonHelper;
import uk.gov.moj.cpp.progression.processor.NowsTemplateNameNotFoundException;
import uk.gov.moj.cpp.progression.processor.InvalidHearingTimeException;
import uk.gov.moj.cpp.progression.service.exception.FileUploadException;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClient;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClientProducer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S00107"})
public class DocumentGeneratorService {

    public static final String PROGRESSION_UPDATE_NOWS_MATERIAL_STATUS = "progression.command.update-nows-material-status";
    public static final String RESULTS_UPDATE_NOWS_MATERIAL_STATUS = "results.update-nows-material-status";
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentGeneratorService.class);
    private static final String FAILED = "failed";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String ERROR_MESSAGE = "Error while uploading document generation or upload ";
    public static final String NCES_DOCUMENT_TEMPLATE_NAME = "NCESNotification";
    public static final String NCES_DOCUMENT_ORDER = "NCESDocumentOrder";

    private DocumentGeneratorClientProducer documentGeneratorClientProducer;

    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    private FileStorer fileStorer;

    private UploadMaterialService uploadMaterialService;

    private SystemUserProvider systemUserProvider;

    @Inject
    public DocumentGeneratorService(final SystemUserProvider systemUserProvider,
                                    final DocumentGeneratorClientProducer documentGeneratorClientProducer,
                                    final ObjectToJsonObjectConverter objectToJsonObjectConverter,
                                    final FileStorer fileStorer,
                                    final UploadMaterialService uploadMaterialService
    ) {
        this.systemUserProvider = systemUserProvider;
        this.documentGeneratorClientProducer = documentGeneratorClientProducer;
        this.objectToJsonObjectConverter = objectToJsonObjectConverter;
        this.fileStorer = fileStorer;
        this.uploadMaterialService = uploadMaterialService;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void generateNow(final Sender sender, final JsonEnvelope originatingEnvelope,
                            final UUID userId, final NowDocumentRequest nowDocumentRequest) {
        try {
            final String orderName = nowDocumentRequest.getNowContent().getOrderName();
            final DocumentGeneratorClient documentGeneratorClient = documentGeneratorClientProducer.documentGeneratorClient();
            final JsonObject nowsDocumentOrderJson = objectToJsonObjectConverter.convert(nowDocumentRequest.getNowContent());
            final byte[] resultOrderAsByteArray = documentGeneratorClient.generatePdfDocument(nowsDocumentOrderJson, nowDocumentRequest.getTemplateName(), getSystemUserUuid());
            addDocumentToMaterial(sender, originatingEnvelope, getTimeStampAmendedFileName(orderName),
                    new ByteArrayInputStream(resultOrderAsByteArray), userId, nowDocumentRequest.getHearingId().toString(), nowDocumentRequest.getMaterialId(),
                    nowDocumentRequest.getCaseId(),
                    null,
                    nowDocumentRequest.getIsRemotePrintingRequired(),
                    nowDocumentRequest.getEmailNotifications());
        } catch (IOException | RuntimeException e) {
            LOGGER.error(ERROR_MESSAGE, e);
            updateStatus(sender, nowDocumentRequest.getHearingId().toString(), nowDocumentRequest.getMaterialId().toString(), userId.toString(), FAILED, PROGRESSION_UPDATE_NOWS_MATERIAL_STATUS);
            updateStatus(sender, nowDocumentRequest.getHearingId().toString(), nowDocumentRequest.getMaterialId().toString(), userId.toString(), FAILED, RESULTS_UPDATE_NOWS_MATERIAL_STATUS);
        }
    }


    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void generateNcesDocument(final Sender sender, final JsonEnvelope originatingEnvelope,
                            final UUID userId, final NcesNotificationRequested ncesNotificationRequested) {
        try {
            final String orderName = NCES_DOCUMENT_ORDER;
            final DocumentGeneratorClient documentGeneratorClient = documentGeneratorClientProducer.documentGeneratorClient();
            final JsonObject nowsDocumentOrderJson = objectToJsonObjectConverter.convert(ncesNotificationRequested.getDocumentContent());
            final byte[] resultOrderAsByteArray = documentGeneratorClient.generatePdfDocument(nowsDocumentOrderJson, NCES_DOCUMENT_TEMPLATE_NAME, getSystemUserUuid());
            addDocumentToMaterial(sender, originatingEnvelope, getTimeStampAmendedFileName(orderName),
                    new ByteArrayInputStream(resultOrderAsByteArray), userId, ncesNotificationRequested.getHearingId().toString(), ncesNotificationRequested.getMaterialId(),
                    ncesNotificationRequested.getCaseId(),
                    null,
                    false,
                    null);

        } catch (IOException | RuntimeException e) {
            LOGGER.error(ERROR_MESSAGE, e);
        }
    }
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public UUID generateDocument(final JsonEnvelope envelope, final JsonObject documentPayload, String templateName, final Sender sender, final UUID prosecutionCaseId, final UUID applicationId) {

        final UUID materialId = UUID.randomUUID();

        try {
            final UUID userId = fromString(envelope.metadata().userId().orElseThrow(() -> new RuntimeException("UserId missing from event.")));

            final byte[] resultOrderAsByteArray = documentGeneratorClientProducer.documentGeneratorClient().generatePdfDocument(documentPayload, templateName, getSystemUserUuid());

            addDocumentToMaterial(
                    sender,
                    envelope,
                    getTimeStampAmendedFileName(templateName),
                    new ByteArrayInputStream(resultOrderAsByteArray),
                    userId,
                    UUID.randomUUID().toString(),
                    materialId,
                    prosecutionCaseId,
                    applicationId,
                    true,
                    null);

        } catch (IOException | RuntimeException e) {
            LOGGER.error(ERROR_MESSAGE, e);
            throw new InvalidHearingTimeException("Error while generating document");
        }
        return materialId;
    }

    private void addDocumentToMaterial(Sender sender, JsonEnvelope originatingEnvelope, final String filename, final InputStream fileContent,
                                       final UUID userId, final String hearingId,
                                       final UUID materialId,
                                       final UUID caseId,
                                       final UUID applicationId,
                                       final boolean isRemotePrintingRequired,
                                       final List<EmailChannel> emailNotifications) {

        try {
            final UUID fileId = storeFile(fileContent, filename);
            LOGGER.info("Stored material {} in file store {}", materialId, fileId);
            uploadMaterialService.uploadFile(new UploadMaterialContextBuilder()
                    .setSender(sender)
                    .setOriginatingEnvelope(originatingEnvelope)
                    .setUserId(userId)
                    .setHearingId(fromString(hearingId))
                    .setMaterialId(materialId)
                    .setFileId(fileId)
                    .setCaseId(caseId)
                    .setApplicationId(applicationId)
                    .setIsRemotePrintingRequired(isRemotePrintingRequired)
                    .setEmailNotifications(emailNotifications)
                    .build());

        } catch (final FileServiceException e) {
            LOGGER.error("Error while uploading file {}", filename);
            throw new FileUploadException(e);
        }
    }

    private UUID storeFile(final InputStream fileContent, final String fileName) throws FileServiceException {
        final JsonObject metadata = createObjectBuilder().add("fileName", fileName).build();
        return fileStorer.store(metadata, fileContent);
    }

    private void updateStatus(Sender sender, String hearingId, String materialId,
                              String userId, String status, String commandName) {
        final JsonObject payload = Json.createObjectBuilder()
                .add("hearing_id", hearingId)
                .add("material_id", materialId)
                .add("status", status).build();

        final JsonEnvelope postRequestEnvelope = JsonHelper.assembleEnvelopeWithPayloadAndMetaDetails(payload,
                commandName, materialId, userId);

        sender.send(postRequestEnvelope);
    }

    private String getTimeStampAmendedFileName(final String fileName) {
        return String.format("%s_%s.pdf", fileName, ZonedDateTime.now().format(TIMESTAMP_FORMATTER));
    }

    private UUID getSystemUserUuid() {
        return systemUserProvider.getContextSystemUserId().orElseThrow(() -> new NowsTemplateNameNotFoundException("Could not find systemId "));
    }
}
