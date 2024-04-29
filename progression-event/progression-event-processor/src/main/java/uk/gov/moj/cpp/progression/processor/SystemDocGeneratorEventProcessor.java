package uk.gov.moj.cpp.progression.processor;

import static java.util.UUID.fromString;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.progression.helper.OnlinePleaProcessorHelper.isForPleaDocument;
import static uk.gov.moj.cpp.progression.helper.OnlinePleaProcessorHelper.isForPleaFinancialDocument;

import uk.gov.justice.progression.courts.NotifyCourtRegister;
import uk.gov.justice.progression.courts.NotifyPrisonCourtRegister;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.client.FileService;
import uk.gov.justice.services.fileservice.domain.FileReference;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.handler.HandleOnlinePleaDocumentCreation;
import uk.gov.moj.cpp.progression.plea.json.schemas.PleaNotificationType;
import uk.gov.moj.cpp.progression.plea.json.schemas.PleadOnline;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class SystemDocGeneratorEventProcessor {

    private static final String PUBLIC_DOCUMENT_AVAILABLE_EVENT_NAME = "public.systemdocgenerator.events.document-available";
    private static final String DOCUMENT_GENERATION_FAILED_EVENT_NAME = "public.systemdocgenerator.events.generation-failed";
    private static final String COURT_REGISTER = "CourtRegister";
    private static final String PRISON_COURT_REGISTER = "PRISON_COURT_REGISTER";
    private static final String NOWS = "NOWs";
    private static final Logger LOGGER = LoggerFactory.getLogger(SystemDocGeneratorEventProcessor.class);
    private static final String CASE_ID = "caseId";
    private static final String DEFENDANT_ID = "defendantId";
    private static final String DOCUMENT_FILE_SERVICE_ID = "documentFileServiceId";
    private static final String SOURCE_CORRELATION_ID = "sourceCorrelationId";
    private static final String PAYLOAD_FILE_SERVICE_ID = "payloadFileServiceId";
    private static final String COURT_CENTRE_ID = "courtCentreId";
    private static final String ORIGINATING_SOURCE = "originatingSource";
    private static final String PAYLOAD_FILE_ID = "payloadFileId";
    private static final String TEMPLATE_IDENTIFIER = "templateIdentifier";
    private static final String CONVERSION_FORMAT = "conversionFormat";
    private static final String REQUESTED_TIME = "requestedTime";
    private static final String FAILED_TIME = "failedTime";
    private static final String REASON = "reason";
    @Inject
    private Sender sender;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private FileService fileService;

    @Handles(PUBLIC_DOCUMENT_AVAILABLE_EVENT_NAME)
    public void handleDocumentAvailable(final JsonEnvelope documentAvailableEvent) throws FileServiceException {
        LOGGER.info(PUBLIC_DOCUMENT_AVAILABLE_EVENT_NAME + " {}", documentAvailableEvent.payload());

        final JsonObject documentAvailablePayload = documentAvailableEvent.payloadAsJsonObject();
        final String originatingSource = documentAvailablePayload.getString(ORIGINATING_SOURCE, "");
        if (COURT_REGISTER.equalsIgnoreCase(originatingSource)) {
            final String fileId = documentAvailablePayload.getString(DOCUMENT_FILE_SERVICE_ID);
            final String courtCenterId = documentAvailablePayload.getString(SOURCE_CORRELATION_ID);
            final NotifyCourtRegister notifyCourtRegister = new NotifyCourtRegister.Builder()
                    .withCourtCentreId(fromString(courtCenterId))
                    .withSystemDocGeneratorId(fromString(fileId))
                    .build();
            this.sender.send(Envelope.envelopeFrom(metadataFrom(documentAvailableEvent.metadata()).withName("progression.command.notify-court-register").build(),
                    this.objectToJsonObjectConverter.convert(notifyCourtRegister)));
        } else if (isForPleaDocument(originatingSource)) {
            final String fileId = documentAvailablePayload.getString(DOCUMENT_FILE_SERVICE_ID);
            final UUID payloadFileId = fromString(documentAvailablePayload.getString(PAYLOAD_FILE_SERVICE_ID));
            final FileReference payloadFileReference = fileService.retrieve(payloadFileId).orElseThrow(() -> new BadRequestException("Failed to retrieve file"));
            LOGGER.info("Retrieved file reference '{}' successfully", payloadFileReference);

            try (JsonReader reader = Json.createReader(payloadFileReference.getContentStream())) {
                final JsonObject rawPayload = reader.readObject();
                LOGGER.info("Read payload '{}'", rawPayload);
                this.sender.send(Envelope.envelopeFrom(metadataFrom(documentAvailableEvent.metadata()).withName("progression.command.handle-online-plea-document-creation").build(),
                        this.objectToJsonObjectConverter.convert(buildHandleOnlinePleaDocumentCreation(originatingSource, fileId, rawPayload))));
            }
        } else if (PRISON_COURT_REGISTER.equalsIgnoreCase(originatingSource)) {
            processPrisonCourtRegisterDocumentAvailable(documentAvailableEvent);
        } else if (NOWS.equalsIgnoreCase(originatingSource)) {
            processNowsDocumentAvailable(documentAvailableEvent);
        }
    }

    @Handles(DOCUMENT_GENERATION_FAILED_EVENT_NAME)
    public void handleDocumentGenerationFailed(final JsonEnvelope envelope) {
        LOGGER.info(DOCUMENT_GENERATION_FAILED_EVENT_NAME + " {}", envelope.payload());

        final JsonObject documentAvailablePayload = envelope.payloadAsJsonObject();

        final String originatingSource = documentAvailablePayload.getString(ORIGINATING_SOURCE, "");

        final String sourceCorrelationId = documentAvailablePayload.getString(SOURCE_CORRELATION_ID);

        final UUID payloadFileId = fromString(documentAvailablePayload.getString(PAYLOAD_FILE_SERVICE_ID));

        final String reason = documentAvailablePayload.getString(REASON);

        if (PRISON_COURT_REGISTER.equalsIgnoreCase(originatingSource)) {

            LOGGER.error("Asynchronous document generation failed. Failed to generate Prison Court Register for courtCentreId: '{}', payloadFileId: '{}', reason: '{}'",
                    sourceCorrelationId, payloadFileId, reason);

            final JsonObjectBuilder payloadBuilder = createObjectBuilder()
                    .add(COURT_CENTRE_ID, sourceCorrelationId)
                    .add(PAYLOAD_FILE_ID, payloadFileId.toString())
                    .add(TEMPLATE_IDENTIFIER, documentAvailablePayload.getString(TEMPLATE_IDENTIFIER))
                    .add(CONVERSION_FORMAT, documentAvailablePayload.getString(CONVERSION_FORMAT))
                    .add(REQUESTED_TIME, documentAvailablePayload.getString(REQUESTED_TIME))
                    .add(FAILED_TIME, documentAvailablePayload.getString(FAILED_TIME))
                    .add(ORIGINATING_SOURCE, documentAvailablePayload.getString(ORIGINATING_SOURCE))
                    .add(REASON, reason);

            this.sender.send(Envelope.envelopeFrom(metadataFrom(envelope.metadata())
                            .withName("progression.command.record-prison-court-register-failed")
                            .build(),
                    this.objectToJsonObjectConverter.convert(payloadBuilder.build())));
        } else if (NOWS.equalsIgnoreCase(originatingSource)) {
            LOGGER.error("Asynchronous document generation failed. Failed to generate NOWs for materialId: '{}', payloadFileId: '{}', reason: '{}'",
                    sourceCorrelationId, payloadFileId, reason);

            final JsonObjectBuilder payloadBuilder = createObjectBuilder()
                    .add("materialId", sourceCorrelationId)
                    .add(PAYLOAD_FILE_ID, payloadFileId.toString())
                    .add(TEMPLATE_IDENTIFIER, documentAvailablePayload.getString(TEMPLATE_IDENTIFIER))
                    .add(CONVERSION_FORMAT, documentAvailablePayload.getString(CONVERSION_FORMAT))
                    .add(REQUESTED_TIME, documentAvailablePayload.getString(REQUESTED_TIME))
                    .add(FAILED_TIME, documentAvailablePayload.getString(FAILED_TIME))
                    .add(ORIGINATING_SOURCE, documentAvailablePayload.getString(ORIGINATING_SOURCE))
                    .add(REASON, reason);

            this.sender.send(Envelope.envelopeFrom(metadataFrom(envelope.metadata())
                    .withName("progression.command.record-nows-document-failed")
                    .build(), this.objectToJsonObjectConverter.convert(payloadBuilder.build())));
        }
    }

    private HandleOnlinePleaDocumentCreation buildHandleOnlinePleaDocumentCreation(final String originatingSource, final String fileId, final JsonObject rawPayload) {
        if (isForPleaFinancialDocument(originatingSource)) {
            final UUID caseId = fromString(rawPayload.getString(CASE_ID));
            final UUID defendantId = fromString(rawPayload.getString(DEFENDANT_ID));
            return getHandleOnlinePleaDocumentCreation(caseId, fileId, originatingSource, defendantId);
        } else {
            final PleadOnline pleadOnline = jsonObjectToObjectConverter.convert(rawPayload, PleadOnline.class);
            return getHandleOnlinePleaDocumentCreation(pleadOnline.getCaseId(), fileId, originatingSource, pleadOnline.getDefendantId());
        }
    }

    private HandleOnlinePleaDocumentCreation getHandleOnlinePleaDocumentCreation(final UUID caseId, final String fileId, final String originatingSource, final UUID defendantId) {
        return new HandleOnlinePleaDocumentCreation.Builder()
                .withCaseId(caseId)
                .withSystemDocGeneratorId(fromString(fileId))
                .withPleaNotificationType(PleaNotificationType.valueFor(originatingSource).orElse(null))
                .withDefendantId(defendantId)
                .build();
    }

    private void processPrisonCourtRegisterDocumentAvailable(final JsonEnvelope documentAvailableEvent) {

        final JsonObject documentAvailablePayload = documentAvailableEvent.payloadAsJsonObject();

        final String documentFileServiceId = documentAvailablePayload.getString(DOCUMENT_FILE_SERVICE_ID);

        final String courtCenterId = documentAvailablePayload.getString(SOURCE_CORRELATION_ID);

        final UUID payloadFileId = fromString(documentAvailablePayload.getString(PAYLOAD_FILE_SERVICE_ID));

        final NotifyPrisonCourtRegister notifyPrisonCourtRegister = new NotifyPrisonCourtRegister.Builder()
                .withCourtCentreId(fromString(courtCenterId))
                .withPayloadFileId(payloadFileId)
                .withSystemDocGeneratorId(fromString(documentFileServiceId))
                .build();

        LOGGER.info("progression.command.notify-prison-court-register - {}", notifyPrisonCourtRegister);

        this.sender.send(Envelope.envelopeFrom(metadataFrom(documentAvailableEvent.metadata())
                        .withName("progression.command.notify-prison-court-register")
                        .build(),
                this.objectToJsonObjectConverter.convert(notifyPrisonCourtRegister)));
    }

    private void processNowsDocumentAvailable(final JsonEnvelope documentAvailableEvent) {

        final JsonObject documentAvailablePayload = documentAvailableEvent.payloadAsJsonObject();

        final String documentFileServiceId = documentAvailablePayload.getString(DOCUMENT_FILE_SERVICE_ID);

        final String materialId = documentAvailablePayload.getString(SOURCE_CORRELATION_ID);

        final UUID payloadFileId = fromString(documentAvailablePayload.getString(PAYLOAD_FILE_SERVICE_ID));

        final JsonObject jsonObject = createObjectBuilder()
                .add("materialId", materialId)
                .add(PAYLOAD_FILE_ID, payloadFileId.toString())
                .add("systemDocGeneratorId", documentFileServiceId)
                .build();

        LOGGER.info("progression.command.record-nows-document-generated - {}", jsonObject);

        this.sender.send(Envelope.envelopeFrom(metadataFrom(documentAvailableEvent.metadata())
                        .withName("progression.command.record-nows-document-generated")
                        .build(),
                this.objectToJsonObjectConverter.convert(jsonObject)));
    }
}