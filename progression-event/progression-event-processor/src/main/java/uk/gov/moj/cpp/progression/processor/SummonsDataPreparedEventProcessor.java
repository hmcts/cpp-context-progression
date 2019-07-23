package uk.gov.moj.cpp.progression.processor;

import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static uk.gov.justice.core.courts.SummonsRequired.FIRST_HEARING;
import static uk.gov.justice.core.courts.SummonsRequired.SJP_REFERRAL;
import static uk.gov.justice.core.courts.SummonsRequired.YOUTH;
import static uk.gov.moj.cpp.progression.helper.SummonsDataHelper.extractAddresse;
import static uk.gov.moj.cpp.progression.helper.SummonsDataHelper.extractCaseReference;
import static uk.gov.moj.cpp.progression.helper.SummonsDataHelper.extractDefendant;
import static uk.gov.moj.cpp.progression.helper.SummonsDataHelper.extractGuardianAddresse;
import static uk.gov.moj.cpp.progression.helper.SummonsDataHelper.extractOffences;
import static uk.gov.moj.cpp.progression.helper.SummonsDataHelper.extractReferralReason;
import static uk.gov.moj.cpp.progression.helper.SummonsDataHelper.extractYouth;
import static uk.gov.moj.cpp.progression.helper.SummonsDataHelper.getCourtTime;
import static uk.gov.moj.cpp.progression.helper.SummonsDataHelper.populateCourtCentreAddress;
import static uk.gov.moj.cpp.progression.helper.SummonsDataHelper.populateReferral;

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

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S1135", "squid:S2789", "squid:S1166", "squid:S1188", "squid:S3776"})
@ServiceComponent(Component.EVENT_PROCESSOR)
public class SummonsDataPreparedEventProcessor {

    private static final String SUMMONS = "Summons";
    private static final String PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT = "progression.command.create-court-document";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DateTimeFormats.STANDARD.getValue());

    private static final Logger LOGGER = LoggerFactory.getLogger(SummonsDataPreparedEventProcessor.class.getName());

    private static final String SUB_TEMPLATE_NAME = "subTemplateName";
    private static final String SUMMONS_TYPE = "type";
    private static final String ISSUE_DATE = "issueDate";
    private static final String LJA_CODE = "ljaCode";
    private static final String LJA_NAME = "ljaName";
    private static final String COURT_CENTRE_NAME = "courtCentreName";
    private static final String DEFENDANT = "defendant";
    private static final String OFFENCES = "offences";
    private static final String HEARING_COURT_DETAILS = "hearingCourtDetails";
    private static final String COURT_NAME = "courtName";
    private static final String HEARING_DATE = "hearingDate";
    private static final String HEARING_TIME = "hearingTime";
    private static final String COURT_ADDRESS = "courtAddress";
    private static final String CASE_REFERENCE = "caseReference";
    private static final String REFERRAL_CONTENT = "referralContent";
    private static final String ADDRESSE = "addressee";
    private static final String YOUTH_CONTENT = "youthContent";
    private static final String LOCAL_JUSTICE_AREA = "localJusticeArea";
    private static final String NATIONAL_COURT_CODE = "nationalCourtCode";
    private static final String L3_NAME = "oucodeL3Name";
    private static final String PROSECUTION_CASE_IDENTIFIER = "prosecutionCaseIdentifier";
    private static final String NAME = "name";

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

    private static JsonObject createSummonsPayloadForSjpReferral(final SummonsDataPrepared summonsDataPrepared, final JsonObject prosecutionCaseJson,
                                                            final JsonObject referralReasonsJson, final JsonObject defendantJson,
                                                                 final ListDefendantRequest defendantRequest, final JsonObject courtCentreJson, final JsonObject ljaDetails) {
        final JsonObject localJusticeArea = ljaDetails.getJsonObject(LOCAL_JUSTICE_AREA);
        return createObjectBuilder()
                .add(SUB_TEMPLATE_NAME, SJP_REFERRAL.toString())
                .add(SUMMONS_TYPE, SJP_REFERRAL.toString())
                .add(ISSUE_DATE, DATE_FORMATTER.format(LocalDate.now()))
                .add(LJA_CODE, localJusticeArea.getString(NATIONAL_COURT_CODE, EMPTY))
                .add(LJA_NAME, localJusticeArea.getString(NAME, EMPTY))
                .add(COURT_CENTRE_NAME, courtCentreJson.getString(L3_NAME, EMPTY))
                .add(DEFENDANT, extractDefendant(defendantJson))
                .add(OFFENCES, extractOffences(defendantJson))
                .add(ADDRESSE, extractAddresse(defendantJson))
                .add(HEARING_COURT_DETAILS, createObjectBuilder()
                        .add(COURT_NAME, courtCentreJson.getString(L3_NAME))
                        .add(HEARING_DATE, getCourtDate(summonsDataPrepared.getSummonsData().getHearingDateTime()))
                        .add(HEARING_TIME, getCourtTime(summonsDataPrepared.getSummonsData().getHearingDateTime()))
                        .add(COURT_ADDRESS, populateCourtCentreAddress(courtCentreJson))
                        .build())
                .add(CASE_REFERENCE, extractCaseReference(prosecutionCaseJson.getJsonObject(PROSECUTION_CASE_IDENTIFIER)))
                .add(REFERRAL_CONTENT, populateReferral(extractReferralReason(referralReasonsJson, defendantRequest.getReferralReason().getId().toString())))
                .build();
    }

    private static JsonObject createSummonsPayloadForFirstHearing(final SummonsDataPrepared summonsDataPrepared, final JsonObject prosecutionCaseJson,
                                                                 final JsonObject defendantJson, final JsonObject courtCentreJson, final JsonObject ljaDetails) {
        final JsonObject localJusticeArea = ljaDetails.getJsonObject(LOCAL_JUSTICE_AREA);
        return createObjectBuilder()
                .add(SUB_TEMPLATE_NAME, FIRST_HEARING.toString())
                .add(SUMMONS_TYPE, FIRST_HEARING.toString())
                .add(ISSUE_DATE, DATE_FORMATTER.format(LocalDate.now()))
                .add(LJA_CODE, localJusticeArea.getString(NATIONAL_COURT_CODE, EMPTY))
                .add(LJA_NAME, localJusticeArea.getString(NAME, EMPTY))
                .add(COURT_CENTRE_NAME, courtCentreJson.getString(L3_NAME, EMPTY))
                .add(DEFENDANT, extractDefendant(defendantJson))
                .add(OFFENCES, extractOffences(defendantJson))
                .add(ADDRESSE, extractAddresse(defendantJson))
                .add(HEARING_COURT_DETAILS, createObjectBuilder()
                        .add(COURT_NAME, courtCentreJson.getString(L3_NAME))
                        .add(HEARING_DATE, getCourtDate(summonsDataPrepared.getSummonsData().getHearingDateTime()))
                        .add(HEARING_TIME, getCourtTime(summonsDataPrepared.getSummonsData().getHearingDateTime()))
                        .add(COURT_ADDRESS, populateCourtCentreAddress(courtCentreJson))
                        .build())
                .add(CASE_REFERENCE, extractCaseReference(prosecutionCaseJson.getJsonObject(PROSECUTION_CASE_IDENTIFIER)))
                .build();
    }

    private static JsonObject createSummonsPayloadForYouth(final SummonsDataPrepared summonsDataPrepared, final JsonObject prosecutionCaseJson,
                                                                  final JsonObject defendantJson, final JsonObject courtCentreJson, final JsonObject ljaDetails) {
        final JsonObject localJusticeArea = ljaDetails.getJsonObject(LOCAL_JUSTICE_AREA);
        return createObjectBuilder()
                .add(SUB_TEMPLATE_NAME, FIRST_HEARING.toString())
                .add(SUMMONS_TYPE, FIRST_HEARING.toString())
                .add(ISSUE_DATE, DATE_FORMATTER.format(LocalDate.now()))
                .add(LJA_CODE, localJusticeArea.getString(NATIONAL_COURT_CODE, EMPTY))
                .add(LJA_NAME, localJusticeArea.getString(NAME, EMPTY))
                .add(COURT_CENTRE_NAME, courtCentreJson.getString(L3_NAME, EMPTY))
                .add(YOUTH_CONTENT, extractYouth(defendantJson, false))
                .add(DEFENDANT, extractDefendant(defendantJson))
                .add(OFFENCES, extractOffences(defendantJson))
                .add(ADDRESSE, extractAddresse(defendantJson))
                .add(HEARING_COURT_DETAILS, createObjectBuilder()
                        .add(COURT_NAME, courtCentreJson.getString(L3_NAME))
                        .add(HEARING_DATE, getCourtDate(summonsDataPrepared.getSummonsData().getHearingDateTime()))
                        .add(HEARING_TIME, getCourtTime(summonsDataPrepared.getSummonsData().getHearingDateTime()))
                        .add(COURT_ADDRESS, populateCourtCentreAddress(courtCentreJson))
                        .build())
                .add(CASE_REFERENCE, extractCaseReference(prosecutionCaseJson.getJsonObject(PROSECUTION_CASE_IDENTIFIER)))
                .build();
    }

    private static JsonObject createSummonsPayloadForYouthGuardian(final SummonsDataPrepared summonsDataPrepared, final JsonObject prosecutionCaseJson,
                                                           final JsonObject defendantJson, final JsonObject courtCentreJson, final JsonObject ljaDetails) {
        final JsonObject localJusticeArea = ljaDetails.getJsonObject(LOCAL_JUSTICE_AREA);
        return createObjectBuilder()
                .add(SUB_TEMPLATE_NAME, YOUTH.toString())
                .add(SUMMONS_TYPE, YOUTH.toString())
                .add(ISSUE_DATE, DATE_FORMATTER.format(LocalDate.now()))
                .add(LJA_CODE, localJusticeArea.getString(NATIONAL_COURT_CODE, EMPTY))
                .add(LJA_NAME, localJusticeArea.getString(NAME, EMPTY))
                .add(COURT_CENTRE_NAME, courtCentreJson.getString(L3_NAME, EMPTY))
                .add(YOUTH_CONTENT, extractYouth(defendantJson, true))
                .add(DEFENDANT, extractDefendant(defendantJson))
                .add(OFFENCES, extractOffences(defendantJson))
                .add(ADDRESSE, extractGuardianAddresse(defendantJson))
                .add(HEARING_COURT_DETAILS, createObjectBuilder()
                        .add(COURT_NAME, courtCentreJson.getString(L3_NAME))
                        .add(HEARING_DATE, getCourtDate(summonsDataPrepared.getSummonsData().getHearingDateTime()))
                        .add(HEARING_TIME, getCourtTime(summonsDataPrepared.getSummonsData().getHearingDateTime()))
                        .add(COURT_ADDRESS, populateCourtCentreAddress(courtCentreJson))
                        .build())
                .add(CASE_REFERENCE, extractCaseReference(prosecutionCaseJson.getJsonObject(PROSECUTION_CASE_IDENTIFIER)))
                .build();
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
                final JsonObject ljaDetails = referenceDataService.getEnforcementAreaByLjaCode(jsonEnvelope, courtCentreJson.getString("lja"));

                LOGGER.debug("Summons requested for case  {}", defendantRequest.getProsecutionCaseId());
                JsonObject summonsRequestedJson = null;
                if (defendantRequest.getSummonsRequired() != null && SJP_REFERRAL == defendantRequest.getSummonsRequired()) {
                    summonsRequestedJson = createSummonsPayloadForSjpReferral(summonsDataPrepared, prosecutionCaseJson, referralReasonsJson, defendantJson, defendantRequest, courtCentreJson, ljaDetails);
                    LOGGER.info("SJP_REFERRAL_PAYLOAD  {}", Objects.nonNull(summonsRequestedJson) ? summonsRequestedJson.toString() : null);
                    generateCourtDocument(jsonEnvelope, defendantId, defendantRequest, summonsRequestedJson);
                }
                if (defendantRequest.getSummonsRequired() != null && FIRST_HEARING == defendantRequest.getSummonsRequired()) {
                    summonsRequestedJson = createSummonsPayloadForFirstHearing(summonsDataPrepared, prosecutionCaseJson, defendantJson, courtCentreJson, ljaDetails);
                    LOGGER.info("FIRST_HEARING_PAYLOAD  {}", Objects.nonNull(summonsRequestedJson) ? summonsRequestedJson.toString() : null);
                    generateCourtDocument(jsonEnvelope, defendantId, defendantRequest, summonsRequestedJson);
                }
                if (defendantRequest.getSummonsRequired() != null && YOUTH == defendantRequest.getSummonsRequired()) {
                    summonsRequestedJson = createSummonsPayloadForYouth(summonsDataPrepared, prosecutionCaseJson, defendantJson, courtCentreJson, ljaDetails);
                    LOGGER.info("YOUTH_PAYLOAD  {}", Objects.nonNull(summonsRequestedJson) ? summonsRequestedJson.toString() : null);
                    generateCourtDocument(jsonEnvelope, defendantId, defendantRequest, summonsRequestedJson);
                    summonsRequestedJson = createSummonsPayloadForYouthGuardian(summonsDataPrepared, prosecutionCaseJson, defendantJson, courtCentreJson, ljaDetails);
                    LOGGER.info("GUARDIAN_PAYLOAD  {}", Objects.nonNull(summonsRequestedJson) ? summonsRequestedJson.toString() : null);
                    generateCourtDocument(jsonEnvelope, defendantId, defendantRequest, summonsRequestedJson);
                }
            });
        }
    }

    private void generateCourtDocument(final JsonEnvelope jsonEnvelope, final UUID defendantId, final ListDefendantRequest defendantRequest, final JsonObject summonsRequestedJson) {
        final UUID materialId = documentGeneratorService.generateDocument(jsonEnvelope, summonsRequestedJson, SUMMONS, sender, defendantRequest.getProsecutionCaseId(), null);
        final CourtDocument courtDocument = courtDocument(defendantRequest.getProsecutionCaseId(), defendantId, materialId);

        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("courtDocument", objectToJsonObjectConverter
                        .convert(courtDocument)).build();
        sender.send(enveloper.withMetadataFrom(jsonEnvelope, PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT).apply(jsonObject));
    }

    private ListDefendantRequest extractDefendantRequestJson(final List<ListDefendantRequest> listDefendantRequests, final UUID defendantId) {
        return listDefendantRequests.stream()
                .filter(e -> defendantId.equals(Objects.nonNull(e.getReferralReason()) ? e.getReferralReason().getDefendantId() : e.getDefendantId()))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    private JsonObject extractDefendantJson(final JsonObject prosecutionCaseJson, final UUID defendantId) {
        return prosecutionCaseJson.getJsonArray("defendants").getValuesAs(JsonObject.class).stream()
                .filter(e -> defendantId.toString().equals(e.getString("id")))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    private CourtDocument courtDocument(final UUID caseId, final UUID defendantId, final UUID materialId) {
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
