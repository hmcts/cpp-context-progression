package uk.gov.moj.cpp.progression.service;

import static com.google.common.collect.ImmutableList.of;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.transaction.Transactional.TxType.REQUIRES_NEW;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.moj.cpp.progression.service.DocumentTemplateType.getDocumentTemplateNameByType;

import uk.gov.justice.core.courts.FormType;
import uk.gov.justice.core.courts.Personalisation;
import uk.gov.justice.core.courts.UpdateNowsMaterialStatus;
import uk.gov.justice.core.courts.nces.NcesNotificationRequested;
import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.core.courts.nowdocument.EmailRenderingVocabulary;
import uk.gov.justice.core.courts.nowdocument.NowDistribution;
import uk.gov.justice.core.courts.nowdocument.NowDocumentContent;
import uk.gov.justice.core.courts.nowdocument.NowDocumentRequest;
import uk.gov.justice.core.courts.nowdocument.OrderAddressee;
import uk.gov.justice.core.courts.nowdocument.ProsecutionCase;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.url.MaterialUrlGenerator;
import uk.gov.moj.cpp.progression.formatters.AccountingDivisionCodeFormatter;
import uk.gov.moj.cpp.progression.processor.exceptions.InvalidHearingTimeException;
import uk.gov.moj.cpp.progression.processor.exceptions.NowsTemplateNameNotFoundException;
import uk.gov.moj.cpp.progression.service.exception.DocumentGenerationException;
import uk.gov.moj.cpp.progression.service.exception.FileUploadException;
import uk.gov.moj.cpp.progression.service.utils.NowDocumentValidator;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClient;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClientProducer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.transaction.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S00107", "squid:S2139", "squid:S00112"})
public class DocumentGeneratorService {

    public static final String PROGRESSION_COMMAND_UPDATE_NOWS_MATERIAL_STATUS = "progression.command.update-nows-material-status";
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentGeneratorService.class);
    private static final String FAILED = "failed";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String ERROR_MESSAGE = "Error while uploading document generation or upload ";
    public static final String NCES_DOCUMENT_TEMPLATE_NAME = "NCESNotification";
    public static final String NCES_DOCUMENT_ORDER = "NCESDocumentOrder";
    public static final String ACCOUNTING_DIVISION_CODE = "accountingDivisionCode";
    public static final String FINANCIAL_ORDER_DETAILS = "financialOrderDetails";
    public static final String PET_DOCUMENT_TEMPLATE_NAME = "PetNotification";
    public static final String PET_DOCUMENT_ORDER = "PetDocumentOrder";
    public static final String STORED_MATERIAL_IN_FILE_STORE = "Stored material {} in file store {}";
    public static final String ERROR_WHILE_UPLOADING_FILE = "Error while uploading file {}";
    public static final String FORM_DOCUMENT_PDF_NAME = "name";
    public static final String FORM_DOCUMENT_FILE_EXTENSION_AS_PDF = ".pdf";
    public static final String IS_WELSH = "isWelsh";
    public static final String STRING_CREATED = " created ";
    public static final String STRING_CREATED_WITH_WELSH = " created (welsh) ";

    private final DocumentGeneratorClientProducer documentGeneratorClientProducer;

    private final ObjectToJsonObjectConverter objectToJsonObjectConverter;

    private final FileStorer fileStorer;

    private final UploadMaterialService uploadMaterialService;

    private final SystemUserProvider systemUserProvider;

    private final MaterialUrlGenerator materialUrlGenerator;

    private final ApplicationParameters applicationParameters;

    private final NowDocumentValidator nowDocumentValidator;

    private final ObjectMapper mapper;

    private final MaterialService materialService;

    @Inject
    public DocumentGeneratorService(final SystemUserProvider systemUserProvider,
                                    final DocumentGeneratorClientProducer documentGeneratorClientProducer,
                                    final ObjectToJsonObjectConverter objectToJsonObjectConverter,
                                    final FileStorer fileStorer,
                                    final UploadMaterialService uploadMaterialService,
                                    final MaterialUrlGenerator materialUrlGenerator,
                                    final ApplicationParameters applicationParameters,
                                    final NowDocumentValidator nowDocumentValidator,
                                    final ObjectMapper mapper,
                                    final MaterialService materialService) {
        this.systemUserProvider = systemUserProvider;
        this.documentGeneratorClientProducer = documentGeneratorClientProducer;
        this.objectToJsonObjectConverter = objectToJsonObjectConverter;
        this.fileStorer = fileStorer;
        this.uploadMaterialService = uploadMaterialService;
        this.materialUrlGenerator = materialUrlGenerator;
        this.applicationParameters = applicationParameters;
        this.nowDocumentValidator = nowDocumentValidator;
        this.mapper = mapper;
        this.materialService = materialService;
    }

    @Transactional(REQUIRES_NEW)
    public void generateNow(final Sender sender, final JsonEnvelope originatingEnvelope,
                            final UUID userId, final NowDocumentRequest nowDocumentRequest) {
        try {
            final String orderName = nowDocumentRequest.getNowContent().getOrderName();
            final DocumentGeneratorClient documentGeneratorClient = documentGeneratorClientProducer.documentGeneratorClient();
            final JsonObject nowDocumentContentJson = objectToJsonObjectConverter.convert(nowDocumentRequest.getNowContent());
            final JsonObject updatedNowContent = updateNowContentWithAccountDivisionCode(nowDocumentContentJson);

            final byte[] resultOrderAsByteArray = documentGeneratorClient.generatePdfDocument(updatedNowContent, getTemplateName(nowDocumentRequest), getSystemUserUuid());
            addDocumentToMaterial(sender, originatingEnvelope, getTimeStampAmendedFileName(orderName),
                    new ByteArrayInputStream(resultOrderAsByteArray), userId, nowDocumentRequest.getHearingId().toString(), nowDocumentRequest.getMaterialId(),
                    nowDocumentRequest.getNowDistribution(), nowDocumentRequest.getNowContent().getOrderAddressee(),
                    isCpsProsecutionCase(nowDocumentRequest.getNowContent()));
        } catch (IOException | RuntimeException e) {
            LOGGER.error(ERROR_MESSAGE, e);
            updateMaterialStatusAsFailed(sender, originatingEnvelope, nowDocumentRequest.getMaterialId());
            throw new RuntimeException("Progression : exception while generating NOWs document ", e);
        }
    }

    @Transactional(REQUIRES_NEW)
    public void generateNcesDocument(final Sender sender, final JsonEnvelope originatingEnvelope,
                                     final UUID userId, final NcesNotificationRequested ncesNotificationRequested) {
        try {
            final DocumentGeneratorClient documentGeneratorClient = documentGeneratorClientProducer.documentGeneratorClient();
            final JsonObject nowsDocumentOrderJson = objectToJsonObjectConverter.convert(ncesNotificationRequested.getDocumentContent());
            final byte[] resultOrderAsByteArray = documentGeneratorClient.generatePdfDocument(nowsDocumentOrderJson, NCES_DOCUMENT_TEMPLATE_NAME, getSystemUserUuid());
            addDocumentToMaterial(sender, originatingEnvelope, getTimeStampAmendedFileName(NCES_DOCUMENT_ORDER),
                    new ByteArrayInputStream(resultOrderAsByteArray), userId, ncesNotificationRequested.getHearingId().toString(), ncesNotificationRequested.getMaterialId(),
                    ncesNotificationRequested.getCaseId(),
                    null,
                    false);

        } catch (IOException | RuntimeException e) {
            LOGGER.error(ERROR_MESSAGE, e);
        }
    }

    @Transactional(REQUIRES_NEW)
    public String generatePetDocument(final JsonEnvelope originatingEnvelope, final JsonObject petForm, final UUID materialId) {
        try {
            final DocumentGeneratorClient documentGeneratorClient = documentGeneratorClientProducer.documentGeneratorClient();
            final byte[] resultOrderAsByteArray = documentGeneratorClient.generatePdfDocument(petForm, PET_DOCUMENT_TEMPLATE_NAME, getSystemUserUuid());
            final String filename = getTimeStampAmendedFileName(PET_DOCUMENT_ORDER);
            addDocumentToMaterial(originatingEnvelope, filename, new ByteArrayInputStream(resultOrderAsByteArray), materialId);
            return filename;
        } catch (IOException | RuntimeException e) {
            throw new DocumentGenerationException(e);
        }
    }

    @Transactional(REQUIRES_NEW)
    public String generateFormDocument(final JsonEnvelope originatingEnvelope, final FormType formType, final JsonObject formData, final UUID materialId) {
        try {
            final DocumentGeneratorClient documentGeneratorClient = documentGeneratorClientProducer.documentGeneratorClient();

            final boolean isWelsh = formData.getBoolean(IS_WELSH, false);
            final String templateName = getDocumentTemplateNameByType(formType, isWelsh);
            final byte[] resultOrderAsByteArray = documentGeneratorClient.generatePdfDocument(formData, templateName, getSystemUserUuid());
            String filename = formData.getString(FORM_DOCUMENT_PDF_NAME).concat(FORM_DOCUMENT_FILE_EXTENSION_AS_PDF);

            if (isWelsh) {
                filename = filename.replaceAll(STRING_CREATED, STRING_CREATED_WITH_WELSH);
            }
            addDocumentToMaterial(originatingEnvelope, filename, new ByteArrayInputStream(resultOrderAsByteArray), materialId);
            return filename;
        } catch (IOException | RuntimeException e) {
            throw new DocumentGenerationException(e);
        }
    }

        @Transactional(REQUIRES_NEW)
    public UUID generateDocument(final JsonEnvelope envelope, final JsonObject documentPayload, String templateName,
                                 final Sender sender, final UUID prosecutionCaseId, final UUID applicationId, final boolean isRemotePrintingRequired) {

        final UUID materialId = randomUUID();

        try {
            final UUID userId = fromString(envelope.metadata().userId().orElseThrow(() -> new RuntimeException("UserId missing from event.")));

            final byte[] resultOrderAsByteArray = documentGeneratorClientProducer.documentGeneratorClient().generatePdfDocument(documentPayload, templateName, getSystemUserUuid());

            addDocumentToMaterial(
                    sender,
                    envelope,
                    getTimeStampAmendedFileName(templateName),
                    new ByteArrayInputStream(resultOrderAsByteArray),
                    userId,
                    randomUUID().toString(),
                    materialId,
                    prosecutionCaseId,
                    applicationId,
                    isRemotePrintingRequired);

        } catch (IOException | RuntimeException e) {
            LOGGER.error(ERROR_MESSAGE, e);
            throw new InvalidHearingTimeException("Error while generating document");
        }
        return materialId;
    }

    @Transactional(REQUIRES_NEW)
    public void generateSummonsDocument(final JsonEnvelope envelope, final JsonObject documentPayload, final String templateName,
                                        final Sender sender, final UUID prosecutionCaseId, final UUID applicationId,
                                        final boolean sendForRemotePrinting, final EmailChannel emailChannel, final UUID materialId) {
        try {
            final UUID userId = fromString(envelope.metadata().userId().orElseThrow(() -> new RuntimeException(format("UserId missing from event %s", envelope.toObfuscatedDebugString()))));

            final byte[] resultOrderAsByteArray = documentGeneratorClientProducer.documentGeneratorClient().generatePdfDocument(documentPayload, templateName, getSystemUserUuid());

            addDocumentToMaterial(
                    sender,
                    envelope,
                    getTimeStampAmendedFileName(templateName),
                    new ByteArrayInputStream(resultOrderAsByteArray),
                    userId,
                    randomUUID().toString(),
                    materialId,
                    prosecutionCaseId,
                    applicationId,
                    sendForRemotePrinting,
                    emailChannel);

        } catch (IOException | RuntimeException e) {
            LOGGER.error(ERROR_MESSAGE, e);
            throw new InvalidHearingTimeException("Error while generating document");
        }
    }

    @Transactional(REQUIRES_NEW)
    public String generateCotrDocument(final JsonEnvelope envelope, final JsonObject documentPayload, String templateName, final UUID materialId, final String filleNameOfPdf) {

        final String fileName = filleNameOfPdf + ".pdf";
        try {
            final byte[] resultOrderAsByteArray = documentGeneratorClientProducer
                    .documentGeneratorClient()
                    .generatePdfDocument(documentPayload, templateName, getSystemUserUuid());

            addDocumentToMaterial(
                    envelope,
                    fileName,
                    new ByteArrayInputStream(resultOrderAsByteArray),
                    materialId);

        } catch (IOException e) {
            LOGGER.error(ERROR_MESSAGE, e);
            throw new InvalidHearingTimeException("Error while generating cotr document", e);
        }
        return fileName;
    }

    @Transactional(REQUIRES_NEW)
    public void generateNonNowDocument(final JsonEnvelope envelope, final JsonObject documentPayload, String templateName, final UUID materialId, final String fileNameWithoutPdfExtension) {

        final String fileName = fileNameWithoutPdfExtension+".pdf";
        try {
            final byte[] resultOrderAsByteArray = documentGeneratorClientProducer
                    .documentGeneratorClient()
                    .generatePdfDocument(documentPayload, templateName, getSystemUserUuid());

            addDocumentToMaterial(
                    envelope,
                    fileName,
                    new ByteArrayInputStream(resultOrderAsByteArray),
                    materialId);

        } catch (IOException e) {
            LOGGER.error(ERROR_MESSAGE, e);
            throw new InvalidHearingTimeException("Error while generating non now document", e);
        }
    }

    private void addDocumentToMaterial(Sender sender, JsonEnvelope originatingEnvelope, final String filename, final InputStream fileContent,
                                       final UUID userId, final String hearingId,
                                       final UUID materialId,
                                       final UUID caseId,
                                       final UUID applicationId,
                                       final boolean isRemotePrintingRequired) {

        addDocumentToMaterial(sender, originatingEnvelope, filename, fileContent, userId, hearingId, materialId, caseId, applicationId, isRemotePrintingRequired, null);
    }

    private void addDocumentToMaterial(final JsonEnvelope originatingEnvelope, final String filename, final InputStream fileContent, final UUID materialId) {

        try {
            final UUID fileId = storeFile(fileContent, filename);
            LOGGER.info(STORED_MATERIAL_IN_FILE_STORE, materialId, fileId);
            materialService.uploadMaterial(fileId, materialId, originatingEnvelope);
        } catch (final FileServiceException e) {
            LOGGER.error(ERROR_WHILE_UPLOADING_FILE, filename);
            throw new FileUploadException(e);
        }
    }

    private void addDocumentToMaterial(Sender sender, JsonEnvelope originatingEnvelope, final String filename, final InputStream fileContent,
                                       final UUID userId, final String hearingId,
                                       final UUID materialId,
                                       final UUID caseId,
                                       final UUID applicationId,
                                       final boolean isRemotePrintingRequired,
                                       final EmailChannel emailChannel) {

        try {
            final UUID fileId = storeFile(fileContent, filename);
            LOGGER.info(STORED_MATERIAL_IN_FILE_STORE, materialId, fileId);
            final UploadMaterialContextBuilder uploadMaterialContextBuilder = new UploadMaterialContextBuilder();
            if (nonNull(emailChannel)) {
                uploadMaterialContextBuilder.setEmailNotifications(of(emailChannel));
            }
            uploadMaterialService.uploadFile(uploadMaterialContextBuilder
                    .setSender(sender)
                    .setOriginatingEnvelope(originatingEnvelope)
                    .setUserId(userId)
                    .setHearingId(fromString(hearingId))
                    .setMaterialId(materialId)
                    .setFileId(fileId)
                    .setCaseId(caseId)
                    .setApplicationId(applicationId)
                    .setFirstClassLetter(false)
                    .setSecondClassLetter(isRemotePrintingRequired)
                    .build());

        } catch (final FileServiceException e) {
            LOGGER.error(ERROR_WHILE_UPLOADING_FILE, filename);
            throw new FileUploadException(e);
        }
    }

    private void addDocumentToMaterial(Sender sender, JsonEnvelope originatingEnvelope, final String filename, final InputStream fileContent,
                                       final UUID userId, final String hearingId,
                                       final UUID materialId,
                                       final NowDistribution nowDistribution,
                                       final OrderAddressee orderAddressee,
                                       final boolean isCps) {
        try {

            final UUID fileId = storeFile(fileContent, filename);

            LOGGER.info(STORED_MATERIAL_IN_FILE_STORE, materialId, fileId);

            final boolean isPostable = nowDocumentValidator.isPostable(orderAddressee);
            final boolean firstClassLetter = isFirstClassLetter(nowDistribution) && isPostable;
            final boolean secondClassLetter = isSecondClassLetter(nowDistribution) && isPostable;
            final boolean isNotificationApi = isNotificationApi(nowDistribution);

            final List<EmailChannel> emailNotifications = buildEmailChannel(materialId, nowDistribution, orderAddressee);

            final UploadMaterialContextBuilder uploadMaterialContextBuilder = new UploadMaterialContextBuilder()
                    .setSender(sender)
                    .setOriginatingEnvelope(originatingEnvelope)
                    .setUserId(userId)
                    .setHearingId(fromString(hearingId))
                    .setMaterialId(materialId)
                    .setFileId(fileId)
                    .setCaseId(null)
                    .setApplicationId(null)
                    .setFirstClassLetter(firstClassLetter)
                    .setSecondClassLetter(secondClassLetter)
                    .setIsNotificationApi(isNotificationApi)
                    .setIsCps(isCps);

            if (!emailNotifications.isEmpty()) {
                uploadMaterialContextBuilder.setEmailNotifications(emailNotifications);
            }

            uploadMaterialService.uploadFile(uploadMaterialContextBuilder.build());

        } catch (final FileServiceException e) {
            LOGGER.error(ERROR_WHILE_UPLOADING_FILE, filename);
            throw new FileUploadException(e);
        }
    }

    private List<EmailChannel> buildEmailChannel(final UUID materialId, final NowDistribution nowDistribution, final OrderAddressee orderAddressee) {

        if (notValidNowDistribution(nowDistribution) || notValidOrderAddressee(orderAddressee)) {
            return Collections.emptyList();
        }

        final List<String> emailAddresses = Stream.of(orderAddressee.getAddress().getEmailAddress1(),
                        orderAddressee.getAddress().getEmailAddress2())
                .filter(StringUtils::isNoneBlank)
                .collect(Collectors.toList());

        return emailAddresses.stream()
                .map(emailAddress -> buildEmailNotification(materialId, nowDistribution, emailAddress))
                .collect(Collectors.toList());
    }

    private boolean notValidNowDistribution(final NowDistribution nowDistribution) {
        return isNull(nowDistribution) || isNull(nowDistribution.getEmail()) || !nowDistribution.getEmail();
    }

    private boolean notValidOrderAddressee(final OrderAddressee orderAddressee) {
        return isNull(orderAddressee) || isNull(orderAddressee.getAddress());
    }

    @SuppressWarnings("squid:S1172")
    private UUID getTemplateId(String emailTemplateName) {
        return fromString(applicationParameters.getEmailTemplateId(emailTemplateName));
    }

    private EmailChannel buildEmailNotification(final UUID materialId, final NowDistribution nowDistribution, final String emailAddress) {
        final String materialUrl = materialUrlGenerator.pdfFileStreamUrlFor(materialId);
        final UUID templateId = getTemplateId(nowDistribution.getEmailTemplateName());
        return EmailChannel.emailChannel()
                .withTemplateId(templateId)
                .withSendToAddress(emailAddress)
                .withMaterialUrl(materialUrl)
                .withPersonalisation(buildPersonalisation(nowDistribution.getEmailContent()))
                .build();
    }

    private String getTemplateName(NowDocumentRequest nowDocumentRequest) {

        final Boolean isWelshCourtCentre = nowDocumentRequest.getNowContent().getOrderingCourt().getWelshCourtCentre();
        if (nonNull(isWelshCourtCentre) && isWelshCourtCentre
                && nonNull(nowDocumentRequest.getBilingualTemplateName())
                && !nowDocumentRequest.getBilingualTemplateName().isEmpty()) {
            return nowDocumentRequest.getBilingualTemplateName();
        }
        return nowDocumentRequest.getTemplateName();
    }

    private Personalisation buildPersonalisation(List<EmailRenderingVocabulary> emailContents) {

        if (isNull(emailContents)) {
            return null;
        }

        final Personalisation.Builder builder = Personalisation.personalisation();
        emailContents.forEach(emailRenderingVocabulary -> builder.withAdditionalProperty(emailRenderingVocabulary.getLabel(), emailRenderingVocabulary.getValue()));
        return builder.build();
    }

    private boolean isSecondClassLetter(NowDistribution nowDistribution) {
        return nonNull(nowDistribution) && nonNull(nowDistribution.getSecondClassLetter()) && nowDistribution.getSecondClassLetter();
    }

    private boolean isFirstClassLetter(NowDistribution nowDistribution) {
        return nonNull(nowDistribution) && nonNull(nowDistribution.getFirstClassLetter()) && nowDistribution.getFirstClassLetter();
    }

    private boolean isNotificationApi(NowDistribution nowDistribution) {
        return nonNull(nowDistribution) && nonNull(nowDistribution.getIsNotificationApi()) && nowDistribution.getIsNotificationApi();
    }

    private UUID storeFile(final InputStream fileContent, final String fileName) throws FileServiceException {
        final JsonObject metadata = createObjectBuilder().add("fileName", fileName).build();
        return fileStorer.store(metadata, fileContent);
    }

    private void updateMaterialStatusAsFailed(Sender sender, final JsonEnvelope originatingEnvelope, UUID materialId) {

        final UpdateNowsMaterialStatus updateNowsMaterialStatusCommand = UpdateNowsMaterialStatus.updateNowsMaterialStatus()
                .withStatus(FAILED)
                .withMaterialId(materialId)
                .build();

        final JsonObject payload = this.objectToJsonObjectConverter.convert(updateNowsMaterialStatusCommand);

        sender.send(envelop(payload).withName(PROGRESSION_COMMAND_UPDATE_NOWS_MATERIAL_STATUS).withMetadataFrom(originatingEnvelope));
    }

    private String getTimeStampAmendedFileName(final String fileName) {
        return format("%s_%s.pdf", fileName, ZonedDateTime.now().format(TIMESTAMP_FORMATTER));
    }

    private UUID getSystemUserUuid() {
        return systemUserProvider.getContextSystemUserId().orElseThrow(() -> new NowsTemplateNameNotFoundException("Could not find systemId "));
    }

    private JsonObject updateNowContentWithAccountDivisionCode(final JsonObject jsonObject) throws JsonProcessingException {
        final JsonNode jsonNode = mapper.valueToTree(jsonObject);

        if (Objects.nonNull(jsonNode.path(FINANCIAL_ORDER_DETAILS)) && !(jsonNode.path(FINANCIAL_ORDER_DETAILS).isMissingNode())) {
            final ObjectNode financialOrderDetailsNode = (ObjectNode) jsonNode.path(FINANCIAL_ORDER_DETAILS);
            final String divisionCode = convertJsonNodeToString(financialOrderDetailsNode.path(ACCOUNTING_DIVISION_CODE));

            financialOrderDetailsNode.put(ACCOUNTING_DIVISION_CODE, AccountingDivisionCodeFormatter
                    .formatAccountingDivisionCode(divisionCode));
            return jsonFromString(mapper.writeValueAsString(jsonNode));
        }

        return jsonObject;
    }

    private String convertJsonNodeToString(JsonNode jsonNode) {
        if (jsonNode.isMissingNode()) {
            return StringUtils.EMPTY;
        }
        return jsonNode.asText();
    }

    private static JsonObject jsonFromString(final String jsonObjectStr) {
        final JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();
        return object;
    }

    private boolean isCpsProsecutionCase(final NowDocumentContent nowContent) {
        return nowContent.getCases().stream()
                .filter(pc -> nonNull(pc.getIsCps()))
                .filter(ProsecutionCase::getIsCps)
                .findFirst()
                .map(ProsecutionCase::getIsCps)
                .orElse(false);
    }

    @Transactional(REQUIRES_NEW)
    public UUID generatePdfDocument(final JsonEnvelope originatingEnvelope, String filename, final byte[] referralDisqualifyWarningContent) {
        try {
            final UUID materialId = randomUUID();
            addDocumentToMaterial(originatingEnvelope, filename, new ByteArrayInputStream(referralDisqualifyWarningContent), materialId);
            return materialId;

        } catch (RuntimeException e) {
            LOGGER.error("DocumentGenerationException Exception happened during Referral Disqualify Warning generation {}", e.getMessage());
            throw new DocumentGenerationException(e);
        }
    }
}
