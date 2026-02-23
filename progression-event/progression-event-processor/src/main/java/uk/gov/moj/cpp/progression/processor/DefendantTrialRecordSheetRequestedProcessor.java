package uk.gov.moj.cpp.progression.processor;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.DefendantTrialRecordSheetRequested;
import uk.gov.justice.core.courts.DefendantTrialRecordSheetRequestedForApplication;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.progression.courts.exract.CourtApplications;
import uk.gov.justice.progression.courts.exract.CourtExtractRequested;
import uk.gov.justice.progression.courts.exract.CourtOrderOffences;
import uk.gov.justice.progression.courts.exract.CourtOrders;
import uk.gov.justice.progression.courts.exract.Defendant;
import uk.gov.justice.progression.courts.exract.Hearings;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

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
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

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
        if(isNull(recordSheetPayload)){
            return;
        }

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
        final CourtApplication application = defendantTrialRecordSheetRequestedForApplication.getCourtApplication();

        final UUID streamId = jsonEnvelope.metadata().streamId().orElse(caseId);
        JsonArray recordSheetPayloads = progressionService.generateTrialRecordSheetPayloadForApplication(jsonEnvelope, caseId, offenceIds);
        recordSheetPayloads.forEach(recordSheetPayload -> {
            final JsonObject payload = recordSheetPayload.asJsonObject().getJsonObject(PAYLOAD);
            final String defendantName = recordSheetPayload.asJsonObject().getString(DEFENDANT_NAME);
            final String fileName = String.format("RecordSheet_%s_%s.pdf", defendantName, utcClock.now().format(TIMESTAMP_FORMATTER));
            final CourtExtractRequested courtExtractRequested = jsonObjectToObjectConverter.convert(payload, CourtExtractRequested.class);
            final UUID fileId = fileService.storePayload(objectToJsonObjectConverter.convert(dedupAllCourtExtractRequested(courtExtractRequested, application)), fileName, RECORD_SHEET_TEMPLATE);
            sendRequestToGenerateDocumentAsync(jsonEnvelope, streamId, fileId, caseId.toString(), defendantName);

        });
    }

    /**
     * Updates the CourtExtractRequested payload with the latest CourtApplication data, as the application may have been updated after the court extract payload was generated.
     *
     * @param courtExtractRequested the court extract payload to be updated
     * @param application           the court application containing the latest court order data
     * @return the updated CourtExtractRequested with deduplicated hearings, or the original CourtExtractRequested if no update is needed
     */
    private CourtExtractRequested dedupAllCourtExtractRequested(final CourtExtractRequested courtExtractRequested, final CourtApplication application) {
        if (isNull(application.getCourtOrder()) || isEmpty(application.getCourtOrder().getCourtOrderOffences())) {
            return courtExtractRequested;
        }
        if (nonNull(courtExtractRequested.getDefendant()) && nonNull(courtExtractRequested.getDefendant().getHearings())) {
            return CourtExtractRequested.courtExtractRequested()
                    .withValuesFrom(courtExtractRequested)
                    .withDefendant(Defendant.defendant()
                            .withValuesFrom(courtExtractRequested.getDefendant())
                            .withHearings(dedupAllHearings(courtExtractRequested.getDefendant().getHearings(), application))
                            .build())
                    .build();
        }

        return courtExtractRequested;
    }

    private List<Hearings> dedupAllHearings(final List<Hearings> hearings, final CourtApplication application) {
        final List<Hearings> updatedHearings = new ArrayList<>();
        hearings.forEach(hearing -> updatedHearings.add(Hearings.hearings()
                .withValuesFrom(hearing)
                .withCourtApplications(dedupAllCourtApplications(hearing.getCourtApplications(), application))
                .build()));
        return updatedHearings;

    }

    private List<CourtApplications> dedupAllCourtApplications(final List<CourtApplications> courtApplications, final CourtApplication application) {
        final List<CourtApplications> updatedCourtApplications = new ArrayList<>();
        Optional.ofNullable(courtApplications).stream().flatMap(Collection::stream).forEach(courtApplication -> {
            if (courtApplication.getId().equals(application.getId())) {
                updatedCourtApplications.add(CourtApplications.courtApplications()
                        .withValuesFrom(courtApplication)
                        .withCourtOrders(dedupAllCourOrder(courtApplication.getCourtOrders(), application))
                        .build());
            } else {
                updatedCourtApplications.add(courtApplication);
            }

        });
        return updatedCourtApplications;

    }

    private CourtOrders dedupAllCourOrder(final CourtOrders courtOrders, final CourtApplication application) {
        return CourtOrders.courtOrders()
                .withValuesFrom(courtOrders)
                .withCourtOrderOffences(dedupAllCourtOrderOffences(courtOrders.getCourtOrderOffences(), application))
                .build();
    }

    private List<CourtOrderOffences> dedupAllCourtOrderOffences(List<CourtOrderOffences> courtOrderOffences, final CourtApplication application) {
        final List<CourtOrderOffences> updatedCourtOrderOffences = new ArrayList<>();
        courtOrderOffences.forEach(courtOrderOffence -> {
            final List<String> resulTexts = new ArrayList<>();
            application.getCourtOrder().getCourtOrderOffences().stream()
                    .map(CourtOrderOffence::getOffence)
                    .filter(offence -> offence.getId().equals(courtOrderOffence.getId()))
                    .map(Offence::getJudicialResults)
                    .filter(CollectionUtils::isNotEmpty)
                    .flatMap(List::stream)
                    .filter(Objects::nonNull)
                    .filter(jr -> isNull(jr.getPublishedForNows()) || Boolean.FALSE.equals(jr.getPublishedForNows()))
                    .map(JudicialResult::getResultText)
                    .filter(StringUtils::isNotEmpty)
                    .forEach(resulTexts::add);
            updatedCourtOrderOffences.add(CourtOrderOffences.courtOrderOffences()
                    .withValuesFrom(courtOrderOffence)
                    .withResultTextList(resulTexts)
                    .build());
        });
        return updatedCourtOrderOffences;
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