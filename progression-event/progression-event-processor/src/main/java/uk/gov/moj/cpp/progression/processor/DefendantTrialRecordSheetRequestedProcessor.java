package uk.gov.moj.cpp.progression.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.DefendantTrialRecordSheetRequested;
import uk.gov.justice.core.courts.DefendantTrialRecordSheetRequestedForApplication;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ConversionFormat;
import uk.gov.moj.cpp.progression.service.DocumentGenerationRequest;
import uk.gov.moj.cpp.progression.service.FileService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.SystemDocGeneratorService;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

import com.google.common.collect.ImmutableMap;

@ServiceComponent(EVENT_PROCESSOR)
public class DefendantTrialRecordSheetRequestedProcessor {

    public static final String DEFENDANT = "defendant";
    public static final String PAYLOAD = "payload";
    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private ProgressionService progressionService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private SystemDocGeneratorService systemDocGeneratorService;

    @Inject
    private FileService fileService;

    @Inject
    private UtcClock utcClock;

    private static final String RECORD_SHEET_TEMPLATE = "RecordSheet";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String RECORD_SHEET_ORIG_SOURCE = "RECORD_SHEET";
    private static final String RECORD_SHEET_CASE_ID = "caseId";
    public static final String DEFENDANT_NAME = "defendantName";

    @Handles("progression.event.defendant-trial-record-sheet-requested")
    public void process(final JsonEnvelope jsonEnvelope) {
        final DefendantTrialRecordSheetRequested defendantTrialRecordSheetRequested = jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), DefendantTrialRecordSheetRequested.class);
        String[] defendantName = new String[1];

        UUID streamId = jsonEnvelope.metadata().streamId().orElse(defendantTrialRecordSheetRequested.getCaseId());
        JsonObject recordSheetPayload = progressionService.generateTrialRecordSheetPayload(jsonEnvelope, defendantTrialRecordSheetRequested.getCaseId(), defendantTrialRecordSheetRequested.getDefendantId());
        recordSheetPayload = recordSheetPayload.containsKey(PAYLOAD) ? recordSheetPayload.getJsonObject(PAYLOAD) : null;

        Optional.ofNullable(recordSheetPayload)
                .filter(r -> r.containsKey(DEFENDANT))
                .map(r -> r.getJsonObject(DEFENDANT)).ifPresent(d -> {
                    defendantName[0] = d.getString("name");
                });

        final String fileName = String.format("RecordSheet_%s_%s.pdf", defendantName[0], utcClock.now().format(TIMESTAMP_FORMATTER));
        final UUID fileId = fileService.storePayload(recordSheetPayload, fileName, RECORD_SHEET_TEMPLATE);

        sendRequestToGenerateDocumentAsync(jsonEnvelope, streamId, fileId, defendantTrialRecordSheetRequested.getCaseId().toString(), defendantName[0]);
    }

    @Handles("progression.event.defendant-trial-record-sheet-requested-for-application")
    public void processForApplication(final JsonEnvelope jsonEnvelope) {
        final DefendantTrialRecordSheetRequestedForApplication defendantTrialRecordSheetRequestedForApplication = jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), DefendantTrialRecordSheetRequestedForApplication.class);
        final UUID caseId = defendantTrialRecordSheetRequestedForApplication.getCaseId();
        final List<UUID> offenceIds = defendantTrialRecordSheetRequestedForApplication.getOffenceIds();

        final UUID streamId = jsonEnvelope.metadata().streamId().orElse(caseId);
        JsonArray recordSheetPayloads = progressionService.generateTrialRecordSheetPayloadForApplication(jsonEnvelope, caseId, offenceIds);
        recordSheetPayloads.forEach(recordSheetPayload -> {
            final JsonObject payload = recordSheetPayload.asJsonObject().getJsonObject(PAYLOAD);
            final String defendantName = recordSheetPayload.asJsonObject().getString(DEFENDANT_NAME);
            final String fileName = String.format("RecordSheet_%s_%s.pdf", defendantName, utcClock.now().format(TIMESTAMP_FORMATTER));
            final UUID fileId = fileService.storePayload(payload, fileName, RECORD_SHEET_TEMPLATE);
            sendRequestToGenerateDocumentAsync(jsonEnvelope, streamId, fileId, caseId.toString(), defendantName);

        });

    }


    private void sendRequestToGenerateDocumentAsync(final JsonEnvelope envelope, final UUID streamId, final UUID fileId, final String caseId, final String defendantName) {

        Map<String, String> additionalInformation = ImmutableMap.of(RECORD_SHEET_CASE_ID, caseId, DEFENDANT_NAME, defendantName);

        final DocumentGenerationRequest documentGenerationRequest = new DocumentGenerationRequest(
                RECORD_SHEET_ORIG_SOURCE,
                RECORD_SHEET_TEMPLATE,
                ConversionFormat.PDF,
                streamId.toString(),
                fileId,
                additionalInformation);

        systemDocGeneratorService.generateDocument(documentGenerationRequest, envelope);
    }
}