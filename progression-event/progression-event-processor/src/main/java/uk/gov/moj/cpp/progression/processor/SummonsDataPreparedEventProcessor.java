package uk.gov.moj.cpp.progression.processor;

import static java.util.Objects.nonNull;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static uk.gov.justice.core.courts.SummonsRequired.SJP_REFERRAL;
import static uk.gov.moj.cpp.progression.helper.SummonsDataHelper.extractCaseReference;
import static uk.gov.moj.cpp.progression.helper.SummonsDataHelper.extractDefendant;
import static uk.gov.moj.cpp.progression.helper.SummonsDataHelper.extractReferralReason;
import static uk.gov.moj.cpp.progression.helper.SummonsDataHelper.populateCourtCentre;
import static uk.gov.moj.cpp.progression.helper.SummonsDataHelper.populateRefferal;

import org.apache.commons.lang3.StringUtils;
import uk.gov.justice.core.courts.ConfirmedProsecutionCaseId;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.DefendantDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.SummonsDataPrepared;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.constant.DateTimeFormats;
import uk.gov.moj.cpp.progression.service.DocumentGeneratorService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.ReferenceDataService;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S1135", "squid:S2789", "squid:S1166"})
@ServiceComponent(Component.EVENT_PROCESSOR)
public class SummonsDataPreparedEventProcessor {

    public static final String SUMMONS = "Summons";
    protected static final String PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT = "progression.command.create-court-document";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DateTimeFormats.STANDARD.getValue());
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(DateTimeFormats.TIME_HHMM.getValue());

    private static final Logger LOGGER = LoggerFactory.getLogger(SummonsDataPreparedEventProcessor.class.getName());
    private static final String SUMMONS_TYPE = "summonsType";
    @Inject
    ProgressionService progressionService;
    @Inject
    private ReferenceDataService referenceDataService;
    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Inject
    private Sender sender;
    @Inject
    private DocumentGeneratorService documentGeneratorService;
    @Inject
    private Enveloper enveloper;

    /*
        TODO data mismatch with the contract:
            address field in courtCentre object and reference data courtCentre endpoint doesn't seem to provide it.
            no parentOrGuardian field in the defendant object
     */
    private static JsonObject createSummonsRequestedPayload(final SummonsDataPrepared summonsDataPrepared, final
    JsonObject prosecutionCaseJson,
                                                            final JsonObject referralReasonsJson, final JsonObject
                                                                    defendantJson, final ListDefendantRequest
                                                                    defendantRequest, final JsonObject courtCentreJson, final JsonObject ljaDetails) {
        LOGGER.info("Summons request payload population starts");
        return createObjectBuilder()
                .add("summonsToCourt", populateCourtCentre(courtCentreJson))
                .add("summonsCourtDate", getCourtDate(summonsDataPrepared.getSummonsData().getHearingDateTime()))
                .add("summonsCourtTime", getCourtTime(summonsDataPrepared.getSummonsData().getHearingDateTime()))
                .add(SUMMONS_TYPE, nonNull(defendantRequest.getSummonsRequired())
                        ? defendantRequest.getSummonsRequired().toString() : SJP_REFERRAL.toString())
                .add("caseReference", extractCaseReference(prosecutionCaseJson.getJsonObject("prosecutionCaseIdentifier")))
                .add("defendant", extractDefendant(defendantJson))
                .add("referral", populateRefferal(extractReferralReason(referralReasonsJson, defendantRequest.getReferralReason().getId().toString())))
                .add("summonsIssueDate", DATE_FORMATTER.format(LocalDate.now()))
                .add("courtCentreName", courtCentreJson.getString("oucodeL3Name", EMPTY))
                .add("ljaCode", ljaDetails.getJsonObject("localJusticeArea").getString("nationalCourtCode", EMPTY))
                .add("ljaName", ljaDetails.getJsonObject("localJusticeArea").getString("name", EMPTY))
                .build();
    }

    private static String getCourtTime(final ZonedDateTime hearingDateTime) {
        try {
            final ZoneId zid = ZoneId.of("Europe/London");
            final ZoneOffset zoneOffset = hearingDateTime.withZoneSameInstant(zid).getOffset();
            final int plusHoursGMT = zoneOffset.getTotalSeconds() / 3600;
            return TIME_FORMATTER.format(hearingDateTime.plusHours(plusHoursGMT));
        }
        catch (DateTimeException dte) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Unable to parse invalid date time {} exception is {} ", hearingDateTime, dte.getMessage());
            }
            return StringUtils.EMPTY;
        }
    }

    private static String getCourtDate(final ZonedDateTime hearingDateTime) {
        return hearingDateTime.toLocalDate().toString();
    }

    @Handles("progression.event.summons-data-prepared")
    public void requestSummons(final JsonEnvelope jsonEnvelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Summons Data Prepared Event received with metadata {} and payload {}", jsonEnvelope.metadata(), jsonEnvelope.toObfuscatedDebugString());
        }
        final SummonsDataPrepared summonsDataPrepared = jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), SummonsDataPrepared.class);
        final List<ConfirmedProsecutionCaseId> confirmedProsecutionCaseIds = summonsDataPrepared.getSummonsData().getConfirmedProsecutionCaseIds();
        final List<ListDefendantRequest> listDefendantRequests = summonsDataPrepared.getSummonsData().getListDefendantRequests();
        final Optional<JsonObject> referralReasonsJsonOptional = referenceDataService.getReferralReasons(jsonEnvelope);
        final JsonObject referralReasonsJson = referralReasonsJsonOptional.orElseThrow(IllegalArgumentException::new);
        final Optional<JsonObject> courtCentreJsonOptional = referenceDataService.getOrganisationUnitById(summonsDataPrepared.getSummonsData().getCourtCentre().getId(), jsonEnvelope);
        final JsonObject courtCentreJson = courtCentreJsonOptional.orElseThrow(IllegalArgumentException::new);

        for (final ConfirmedProsecutionCaseId confirmedProsecutionCaseId : confirmedProsecutionCaseIds) {
            final Optional<JsonObject> prosecutionCaseOptional = progressionService.getProsecutionCaseDetailById(jsonEnvelope, confirmedProsecutionCaseId.getId().toString());
            final JsonObject prosecutionCaseJson = prosecutionCaseOptional.orElseThrow(IllegalArgumentException::new).getJsonObject("prosecutionCase");

            confirmedProsecutionCaseId.getConfirmedDefendantIds().forEach(defendantId -> {
                final JsonObject defendantJson = extractDefendantJson(prosecutionCaseJson, defendantId);
                final ListDefendantRequest defendantRequest = extractDefendantRequestJson(listDefendantRequests, defendantId);

                if (!confirmedProsecutionCaseId.getId().equals(defendantRequest.getProsecutionCaseId())) {
                    throw new IllegalStateException();
                }
                if (defendantRequest.getSummonsRequired() != null && SJP_REFERRAL == defendantRequest.getSummonsRequired()) {
                    final JsonObject ljaDetails = referenceDataService.getEnforcementAreaByLjaCode(jsonEnvelope, courtCentreJson.getString("lja"));

                    final JsonObject summonsRequestedJson = createSummonsRequestedPayload(summonsDataPrepared, prosecutionCaseJson,
                            referralReasonsJson, defendantJson, defendantRequest, courtCentreJson, ljaDetails);
                    LOGGER.debug("Summons requested for case  {}", defendantRequest.getProsecutionCaseId());
                    final UUID materialId = documentGeneratorService.generateSummons(jsonEnvelope, summonsRequestedJson, sender, defendantRequest.getProsecutionCaseId());
                    final CourtDocument courtDocument = courtDocument(defendantRequest.getProsecutionCaseId(), defendantId, materialId);

                    final JsonObject jsonObject = Json.createObjectBuilder()
                            .add("courtDocument", objectToJsonObjectConverter
                                    .convert(courtDocument)).build();
                    sender.send(enveloper.withMetadataFrom(jsonEnvelope, PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT).apply(jsonObject));
                }
            });
        }
    }

    private ListDefendantRequest extractDefendantRequestJson(final List<ListDefendantRequest> listDefendantRequests, final UUID defendantId) {
        return listDefendantRequests.stream()
                .filter(e -> defendantId.equals(e.getReferralReason().getDefendantId()))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    private JsonObject extractDefendantJson(final JsonObject prosecutionCaseJson, final UUID defendantId) {
        return prosecutionCaseJson.getJsonArray("defendants").getValuesAs(JsonObject.class).stream()
                .filter(e -> defendantId.toString().equals(e.getString("id")))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    private final CourtDocument courtDocument(final UUID caseId, final UUID defendantId, final UUID materialId) {
        final List<UUID> defendants = new ArrayList<>();
        defendants.add(defendantId);
        final DefendantDocument defendantDocument = DefendantDocument.defendantDocument()
                .withDefendants(defendants)
                .withProsecutionCaseId(caseId)
                .build();

        final DocumentCategory documentCategory = DocumentCategory.documentCategory()
                .withDefendantDocument(defendantDocument)
                .build();

        return CourtDocument.courtDocument()
                .withCourtDocumentId(UUID.randomUUID())
                //this doesnt seem necessary
                //GPE-6752 this could be hard code to a nows document type
                .withDocumentTypeId(UUID.randomUUID())
                .withDocumentTypeDescription(SUMMONS)
                .withMaterials(Collections.singletonList(Material.material()
                                .withId(materialId)
                                .withGenerationStatus(null)
                                .withUploadDateTime(ZonedDateTime.now())
                                .withName(SUMMONS)
                                .build()
                        )
                )
                .withDocumentCategory(documentCategory)
                .withName(SUMMONS)
                .build();
    }
}
