package uk.gov.moj.cpp.progression.processor;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.progression.helper.OnlinePleaProcessorHelper.isForPleaDocument;
import static uk.gov.moj.cpp.progression.helper.OnlinePleaProcessorHelper.isForPleaFinancialDocument;

import uk.gov.justice.progression.courts.NotifyCourtRegister;
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
import javax.json.JsonReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class SystemDocGeneratorEventProcessor {


    private static final String DOCUMENT_AVAILABLE_EVENT_NAME = "public.systemdocgenerator.events.document-available";
    private static final String COURT_REGISTER = "CourtRegister";
    private static final Logger LOGGER = LoggerFactory.getLogger(SystemDocGeneratorEventProcessor.class);
    public static final String CASE_ID = "caseId";
    public static final String DEFENDANT_ID = "defendantId";

    @Inject
    private Sender sender;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private FileService fileService;

    @Handles(DOCUMENT_AVAILABLE_EVENT_NAME)
    public void handleDocumentAvailable(final JsonEnvelope documentAvailableEvent) throws FileServiceException {
        final JsonObject documentAvailablePayload = documentAvailableEvent.payloadAsJsonObject();
        final String originatingSource = documentAvailablePayload.getString("originatingSource", "");
        if (COURT_REGISTER.equalsIgnoreCase(originatingSource)) {
            final String fileId = documentAvailablePayload.getString("documentFileServiceId");

            final String courtCenterId = documentAvailablePayload.getString("sourceCorrelationId");
            final NotifyCourtRegister notifyCourtRegister = new NotifyCourtRegister.Builder()
                    .withCourtCentreId(fromString(courtCenterId))
                    .withSystemDocGeneratorId(fromString(fileId))
                    .build();
            this.sender.send(Envelope.envelopeFrom(metadataFrom(documentAvailableEvent.metadata()).withName("progression.command.notify-court-register").build(),
                    this.objectToJsonObjectConverter.convert(notifyCourtRegister)));
        } else if (isForPleaDocument(originatingSource)) {
            final String fileId = documentAvailablePayload.getString("documentFileServiceId");
            final UUID payloadFileId = fromString(documentAvailablePayload.getString("payloadFileServiceId"));
            final FileReference payloadFileReference = fileService.retrieve(payloadFileId).orElseThrow(() -> new BadRequestException("Failed to retrieve file"));
            LOGGER.info("Retrieved file reference '{}' successfully", payloadFileReference);

            try (JsonReader reader = Json.createReader(payloadFileReference.getContentStream())) {
                final JsonObject rawPayload = reader.readObject();
                LOGGER.info("Read payload '{}'", rawPayload);
                this.sender.send(Envelope.envelopeFrom(metadataFrom(documentAvailableEvent.metadata()).withName("progression.command.handle-online-plea-document-creation").build(),
                        this.objectToJsonObjectConverter.convert(buildHandleOnlinePleaDocumentCreation(originatingSource, fileId, rawPayload))));
            }
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

}
