package uk.gov.moj.cpp.progression.service;

import static java.util.UUID.fromString;
import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.core.courts.CreateNowsRequest;
import uk.gov.justice.core.courts.NotificationDocumentState;
import uk.gov.justice.core.courts.NowType;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.activiti.common.JsonHelper;
import uk.gov.moj.cpp.progression.event.nows.order.NowsDocumentOrder;
import uk.gov.moj.cpp.progression.processor.NowsNotificationDocumentState;
import uk.gov.moj.cpp.progression.processor.NowsTemplateNameNotFoundException;
import uk.gov.moj.cpp.progression.processor.SummonGenerationException;
import uk.gov.moj.cpp.progression.service.exception.FileUploadException;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClient;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClientProducer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
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
    private static final String SUMMONS = "Summons";
    private static final String FAILED = "failed";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

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

    private NotificationDocumentState convert(final NowsNotificationDocumentState nowsNotificationDocumentState) {
        return NotificationDocumentState.notificationDocumentState()
                .withCaseUrns(nowsNotificationDocumentState.getCaseUrns())
                .withCourtCentreName(nowsNotificationDocumentState.getCourtCentreName())
                .withCourtClerkName(nowsNotificationDocumentState.getCourtClerkName())
                .withDefendantName(nowsNotificationDocumentState.getDefendantName())
                .withJurisdiction(nowsNotificationDocumentState.getJurisdiction())
                .withMaterialId(nowsNotificationDocumentState.getMaterialId())
                .withNowsTypeId(nowsNotificationDocumentState.getNowsTypeId())
                .withOrderName(nowsNotificationDocumentState.getOrderName())
                .withOriginatingCourtCentreId(nowsNotificationDocumentState.getOriginatingCourtCentreId())
                .withPriority(nowsNotificationDocumentState.getPriority())
                .withUsergroups(nowsNotificationDocumentState.getUsergroups())
                .build();
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void generateNow(final Sender sender, final JsonEnvelope originatingEnvelope, final UUID userId, final CreateNowsRequest nowsRequested,
                            final String hearingId, final Map<NowsDocumentOrder, NowsNotificationDocumentState> nowsDocumentOrderToNotificationState,
                            final NowsDocumentOrder nowsDocumentOrder) {
        try {
            final NowsNotificationDocumentState nowsNotificationDocumentState = nowsDocumentOrderToNotificationState.get(nowsDocumentOrder);
            final String templateName = getTemplateName(nowsRequested, nowsNotificationDocumentState);
            final DocumentGeneratorClient documentGeneratorClient = documentGeneratorClientProducer.documentGeneratorClient();
            final JsonObject nowsDocumentOrderJson = objectToJsonObjectConverter.convert(nowsDocumentOrder);
            final byte[] resultOrderAsByteArray = documentGeneratorClient.generatePdfDocument(nowsDocumentOrderJson, templateName, getSystemUserUuid());
            final UUID caseId = nowsRequested.getHearing().getProsecutionCases().get(0).getId();
            addDocumentToMaterial(sender, originatingEnvelope, getTimeStampAmendedFileName(nowsNotificationDocumentState.getOrderName()), new ByteArrayInputStream(resultOrderAsByteArray),
                    userId, hearingId, fromString(nowsNotificationDocumentState.getMaterialId().toString()), convert(nowsNotificationDocumentState), caseId, nowsNotificationDocumentState.getIsRemotePrintingRequired());
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Error while uploading document generation or upload ", e);
            updateStatus(sender, hearingId, nowsDocumentOrder.getMaterialId().toString(), userId.toString(), FAILED, PROGRESSION_UPDATE_NOWS_MATERIAL_STATUS);
            updateStatus(sender, hearingId, nowsDocumentOrder.getMaterialId().toString(), userId.toString(), FAILED, RESULTS_UPDATE_NOWS_MATERIAL_STATUS);
        }
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public UUID generateSummons(final JsonEnvelope envelope, final JsonObject documentPayload, final Sender sender, final UUID prosecutionCaseId) {
        final UUID materialId = UUID.randomUUID();
        try {
            final UUID userId = fromString(envelope.metadata().userId().orElseThrow(() -> new RuntimeException("UserId missing from event.")));

            final byte[] resultOrderAsByteArray = documentGeneratorClientProducer.documentGeneratorClient().generatePdfDocument(documentPayload, SUMMONS, getSystemUserUuid());
            addDocumentToMaterial(sender, envelope, getTimeStampAmendedFileName(SUMMONS), new ByteArrayInputStream(resultOrderAsByteArray), userId, UUID.randomUUID().toString(), materialId, null, prosecutionCaseId, true);

        } catch (IOException | RuntimeException e) {
            LOGGER.error("Error while uploading document generation or upload ", e);
            throw new SummonGenerationException("Error while generating simmons");
        }
        return materialId;
    }

    private String getTemplateName(CreateNowsRequest nowsRequested, NowsNotificationDocumentState nowsNotificationDocumentState) {
        final UUID nowsTypeId = nowsNotificationDocumentState.getNowsTypeId();
        return nowsRequested.getNowTypes().stream()
                .filter(nt -> nowsTypeId.equals(nt.getId()))
                .findFirst()
                .map(NowType::getTemplateName)
                .orElseThrow(() -> new NowsTemplateNameNotFoundException(String.format("Could not find templateName for nowsTypeId: %s", nowsTypeId)));
    }

    private void addDocumentToMaterial(Sender sender, JsonEnvelope originatingEnvelope, final String filename, final InputStream fileContent,
                                       final UUID userId, final String hearingId,
                                       final UUID materialId, final NotificationDocumentState nowsDocumentOrder, final UUID caseId, final boolean isRemotePrintingRequired) {

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
                    .setNowsNotificationDocumentState(nowsDocumentOrder)
                    .setCaseId(caseId).setIsRemotePrintingRequired(isRemotePrintingRequired).createUploadMaterialContext());

        } catch (final FileServiceException e) {
            LOGGER.error("Error while uploading file {}", filename);
            throw new FileUploadException(e);
        }
    }

    private UUID storeFile(final InputStream fileContent, final String fileName) throws FileServiceException {
        final JsonObject metadata = createObjectBuilder().add("fileName", fileName).build();
        return fileStorer.store(metadata, fileContent);
    }

    private void updateStatus(Sender sender, String hearingId, String materialId, String userId, String status, String commandName) {
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
