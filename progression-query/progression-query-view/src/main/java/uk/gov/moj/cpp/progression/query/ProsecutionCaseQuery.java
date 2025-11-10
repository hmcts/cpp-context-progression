package uk.gov.moj.cpp.progression.query;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.messaging.JsonObjects.getUUID;
import static uk.gov.moj.cpp.progression.domain.helper.JsonHelper.addProperty;
import static uk.gov.moj.cpp.progression.query.utils.CaseHearingsQueryHelper.buildCaseDefendantHearingsResponse;
import static uk.gov.moj.cpp.progression.query.utils.CaseHearingsQueryHelper.buildCaseHearingTypesResponse;
import static uk.gov.moj.cpp.progression.query.utils.CaseHearingsQueryHelper.buildCaseHearingsResponse;
import static uk.gov.moj.cpp.progression.query.utils.SearchQueryUtils.prepareSearch;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CivilFees;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantCase;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.progression.courts.CaagDefendants;
import uk.gov.justice.progression.courts.GetHearingsAtAGlance;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.progression.query.CotrDetail;
import uk.gov.justice.progression.query.TrialHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.progression.domain.pojo.Prosecutor;
import uk.gov.moj.cpp.progression.query.utils.ResultTextFlagBuilder;
import uk.gov.moj.cpp.progression.query.view.CaseAtAGlanceHelper;
import uk.gov.moj.cpp.progression.query.view.service.CotrQueryService;
import uk.gov.moj.cpp.progression.query.view.service.HearingAtAGlanceService;
import uk.gov.moj.cpp.progression.query.view.service.ReferenceDataService;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseCpsProsecutorEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentMaterialEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.MatchDefendantCaseHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.SearchProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.utils.CourtApplicationSummary;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.utils.SearchCaseBuilder;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseCpsProsecutorRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CivilFeeRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentMaterialRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.MatchDefendantCaseHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.RelatedReferenceRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.SearchProsecutionCaseRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.NoResultException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S3655", "squid:S1612", "squid:S1067", "squid:S1166", "squid:S3252"})
@ServiceComponent(Component.QUERY_VIEW)
public class ProsecutionCaseQuery {

    public static final String PROSECUTION_CASE_IDENTIFIER = "prosecutionCaseIdentifier";
    public static final String PROSECUTOR_OBJECT = "prosecutor";
    public static final String PROSECUTOR_ID = "prosecutorId";
    public static final String PROSECUTION_AUTHORITY_ID = "prosecutionAuthorityId";
    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionCaseQuery.class);
    private static final String CASE_ID = "caseId";
    private static final String DEFENDANT_ID = "defendantId";
    private static final String GROUP_ID = "groupId";
    private static final String FIELD_QUERY = "q";
    private static final String FIELD_CASE_URN = "caseUrn";
    private static final String SEARCH_RESULT = "searchResults";
    public static final String OLD_PROSECUTION_AUTHORITY_CODE = "oldProsecutionAuthorityCode";
    public static final String HEARINGS_AT_A_GLANCE = "hearingsAtAGlance";
    public static final String PROSECUTION_CASE = "prosecutionCase";
    public static final String MASTER_CASE = "masterCase";
    public static final String CASE_STATUS = "caseStatus";
    public static final String CASE_STATUS_ACTIVE = "ACTIVE";
    private static final String ORDER_DATE = "orderDate";
    public static final String CASE_IDS_SEARCH_PARAM = "caseIds";
    public static final String NO_CASE_FOUND_YET_FOR_CASE_ID = "# No case found yet for caseId '{}'";
    public static final String ALL_CASE_HEARINGS = "allCaseHearings";
    public static final String LINKED_APPLICATIONS_SUMMARY = "linkedApplicationsSummary";
    public static final String CIVIL_FEES = "civilFees";
    public static final String PROSECUTION_CASES = "prosecutionCases";
    public static final String OFFENCE_IDS = "offenceIds";
    public static final String APPEALS_LODGED_INFO = "appealsLodgedInfo";
    public static final String APPEALS_LODGED = "appealsLodged";
    public static final String APPEALS_LODGED_FOR = "appealsLodgedFor";

    @Inject
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Inject
    private SearchProsecutionCaseRepository searchCaseRepository;

    @Inject
    private CourtDocumentRepository courtDocumentRepository;

    @Inject
    private CourtDocumentMaterialRepository courtDocumentMaterialRepository;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private ListToJsonArrayConverter<CaagDefendants> listToJsonArrayConverter;

    @Inject
    private ListToJsonArrayConverter<Hearings> hearingListToJsonArrayConverter;

    @Inject
    private HearingAtAGlanceService hearingAtAGlanceService;

    @Inject
    private CourtApplicationCaseRepository courtApplicationCaseRepository;

    @Inject
    private HearingApplicationRepository hearingApplicationRepository;

    @Inject
    private MatchDefendantCaseHearingRepository matchDefendantCaseHearingRepository;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private CaseCpsProsecutorRepository caseCpsProsecutorRepository;

    @Inject
    private CotrQueryService cotrQueryService;

    @Inject
    private ResultTextFlagBuilder resultTextFlagBuilder;

    @Inject
    private CivilFeeRepository civilFeeRepository;

    @Inject
    private RelatedReferenceRepository relatedReferenceRepository;

    @Handles("progression.query.prosecutioncase-details")
    public JsonEnvelope getProsecutionCaseDetails(final JsonEnvelope envelope) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        final Optional<UUID> caseId = getUUID(envelope.payloadAsJsonObject(), CASE_ID);
        try {
            final ProsecutionCaseEntity prosecutionCaseEntity = prosecutionCaseRepository.findByCaseId(caseId.get());
            final JsonObject prosecutionCase = stringToJsonObjectConverter.convert(prosecutionCaseEntity.getPayload());
            jsonObjectBuilder.add(PROSECUTION_CASE, prosecutionCase);
        } catch (final NoResultException e) {
            LOGGER.info("No case found  for caseId '{}'", caseId.get());
        }

        return JsonEnvelope.envelopeFrom(
                envelope.metadata(),
                jsonObjectBuilder.build());
    }

    @Handles("progression.query.mastercase-details")
    public JsonEnvelope getProsecutionMasterCaseDetails(final JsonEnvelope envelope) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        final Optional<UUID> groupId = getUUID(envelope.payloadAsJsonObject(), GROUP_ID);
        try {
            final List<ProsecutionCaseEntity> prosecutionCaseEntities = prosecutionCaseRepository.findByGroupId(groupId.get());
            final Optional<JsonObject> masterProsecutionCase = getMasterProsecutionCase(prosecutionCaseEntities);
            if (masterProsecutionCase.isPresent()) {
                jsonObjectBuilder.add(MASTER_CASE, masterProsecutionCase.get());
            } else {
                LOGGER.info("No group case found  for groupId '{}'", groupId.get());
            }
        } catch (final NoResultException e) {
            LOGGER.info("No case found  for groupId '{}'", groupId.get());
        }

        return JsonEnvelope.envelopeFrom(
                envelope.metadata(),
                jsonObjectBuilder.build());
    }

    @Handles("progression.query.group-member-cases")
    public JsonEnvelope getGroupMemberCases(final JsonEnvelope envelope) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        final Optional<UUID> groupId = getUUID(envelope.payloadAsJsonObject(), GROUP_ID);
        try {
            final List<ProsecutionCaseEntity> prosecutionCaseEntities = prosecutionCaseRepository.findByGroupId(groupId.get());
            final JsonArrayBuilder prosecutionCasesArray = getMemberProsecutionCases(prosecutionCaseEntities);
            jsonObjectBuilder.add(PROSECUTION_CASES, prosecutionCasesArray);
        } catch (final NoResultException e) {
            LOGGER.info("No member case found  for groupId '{}'", groupId.get());
        }

        return JsonEnvelope.envelopeFrom(
                envelope.metadata(),
                jsonObjectBuilder.build());
    }

    private JsonArrayBuilder getMemberProsecutionCases(final List<ProsecutionCaseEntity> prosecutionCaseEntities) {
        final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        final List<JsonObject> jsonObjectList = prosecutionCaseEntities.stream()
                .map(o -> jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(o.getPayload()), ProsecutionCase.class))
                .filter(p -> (p.getIsGroupMember() && !p.getIsGroupMaster()))
                .map(prosecutionCase -> objectToJsonObjectConverter.convert(prosecutionCase))
                .collect(Collectors.toList());
        jsonObjectList.forEach(jsonArrayBuilder::add);
        return jsonArrayBuilder;
    }

    private Optional<JsonObject> getMasterProsecutionCase(final List<ProsecutionCaseEntity> prosecutionCaseEntities) {
        return prosecutionCaseEntities.stream()
                .map(o -> jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(o.getPayload()), ProsecutionCase.class))
                .filter(p -> (p.getIsGroupMaster()))
                .findFirst()
                .map(prosecutionCase -> objectToJsonObjectConverter.convert(prosecutionCase));
    }

    @Handles("progression.query.prosecutioncase")
    public JsonEnvelope getProsecutionCase(final JsonEnvelope envelope) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        final Optional<UUID> caseId = getUUID(envelope.payloadAsJsonObject(), CASE_ID);
        try {
            final ProsecutionCaseEntity prosecutionCaseEntity = prosecutionCaseRepository.findByCaseId(caseId.get());
            JsonObject prosecutionCase = stringToJsonObjectConverter.convert(prosecutionCaseEntity.getPayload());
            if (!prosecutionCase.containsKey(CASE_STATUS)) {
                prosecutionCase = addProperty(prosecutionCase, CASE_STATUS, CASE_STATUS_ACTIVE);
            }

            final List<Hearings> hearingsList = hearingAtAGlanceService.getCaseHearings(caseId.get());
            final ProsecutionCase prosecutionCase1 = jsonObjectToObjectConverter.convert(prosecutionCase, ProsecutionCase.class);
            final CaseAtAGlanceHelper caseAtAGlanceHelper = new CaseAtAGlanceHelper(prosecutionCase1, hearingsList, referenceDataService, civilFeeRepository, relatedReferenceRepository);
            final List<CivilFees> civilFeesList = caseAtAGlanceHelper.getCivilFeeEntity(prosecutionCase1);

            JsonArray civilFeesArray = null;
            if (!civilFeesList.isEmpty()) {
                final JsonArrayBuilder jsonProsecutionBuilder = Json.createArrayBuilder();
                civilFeesList.forEach(civilFee -> buildCivilFeesList(civilFee, jsonProsecutionBuilder));
                civilFeesArray = jsonProsecutionBuilder.build();
            }

            final JsonObjectBuilder newJsonObjectBuilder = createObjectBuilder();
            final JsonArray finalCivilFeesArray = civilFeesArray;
            prosecutionCase.forEach((k, v) -> {
                if (k.equals(CIVIL_FEES) && nonNull(finalCivilFeesArray)) {
                    newJsonObjectBuilder.add(CIVIL_FEES, finalCivilFeesArray);
                } else {
                    newJsonObjectBuilder.add(k, v);
                }
            });

            final JsonObject prosecutionCaseWithFees = newJsonObjectBuilder.build();
            jsonObjectBuilder.add(PROSECUTION_CASE, prosecutionCaseWithFees);

            final JsonObject getCaseAtAGlanceJson = objectToJsonObjectConverter.convert(getHearingsAtAGlance(jsonObjectBuilder, caseId));
            jsonObjectBuilder.add(HEARINGS_AT_A_GLANCE, getCaseAtAGlanceJson);

            final CaseCpsProsecutorEntity caseCpsProsecutorEntity = caseCpsProsecutorRepository.findBy(caseId.get());
            if (nonNull(caseCpsProsecutorEntity) && StringUtils.isNotEmpty(caseCpsProsecutorEntity.getOldCpsProsecutor())) {
                final String oldCpsProsecutor = caseCpsProsecutorEntity.getOldCpsProsecutor();
                JsonObject prosecutionCaseIdentifier = addProperty(prosecutionCaseWithFees.getJsonObject(PROSECUTION_CASE_IDENTIFIER), OLD_PROSECUTION_AUTHORITY_CODE, oldCpsProsecutor);
                jsonObjectBuilder.add(PROSECUTION_CASE, addProperty(prosecutionCaseWithFees, PROSECUTION_CASE_IDENTIFIER, prosecutionCaseIdentifier));

                prosecutionCaseIdentifier = addProperty(getCaseAtAGlanceJson.getJsonObject(PROSECUTION_CASE_IDENTIFIER), OLD_PROSECUTION_AUTHORITY_CODE, oldCpsProsecutor);
                jsonObjectBuilder.add(HEARINGS_AT_A_GLANCE, addProperty(getCaseAtAGlanceJson, PROSECUTION_CASE_IDENTIFIER, prosecutionCaseIdentifier));
            }

            final List<UUID> masterDefendantIds = retrieveMasterDefendantIdList(caseId.get());

            final List<MatchDefendantCaseHearingEntity> matchedCases = matchDefendantCaseHearingRepository.findByMasterDefendantId(masterDefendantIds);

            final Map<UUID, List<MatchDefendantCaseHearingEntity>> matchedCasesGroupedByMasterDefendantId = matchedCases.stream()
                    .filter(matchDefendantCaseHearingEntity -> masterDefendantIds.contains(matchDefendantCaseHearingEntity.getMasterDefendantId()))
                    .filter(matchDefendantCaseHearingEntity -> !matchDefendantCaseHearingEntity.getProsecutionCaseId().equals(caseId.get()))
                    .collect(Collectors.groupingBy(MatchDefendantCaseHearingEntity::getMasterDefendantId));

            final String statusOfPrimaryCase = prosecutionCaseWithFees.getString(CASE_STATUS);
            final JsonArrayBuilder relatedCasesArrayBuilder = Json.createArrayBuilder();
            matchedCasesGroupedByMasterDefendantId.forEach((masterDefendantId, cases) -> buildRelatedCasesForDefendant(masterDefendantId, cases, relatedCasesArrayBuilder, statusOfPrimaryCase));
            final JsonArray relatedCases = relatedCasesArrayBuilder.build();
            if (isNotEmpty(relatedCases)) {
                jsonObjectBuilder.add("relatedCases", relatedCases);
            }

            populateAppealLodgedInfo(caseId.get(), jsonObjectBuilder);

        } catch (final NoResultException e) {
            LOGGER.info(NO_CASE_FOUND_YET_FOR_CASE_ID, caseId.get());
        }

        return JsonEnvelope.envelopeFrom(
                envelope.metadata(),
                jsonObjectBuilder.build());
    }

    private static JsonArray buildAppealLodgedInfo(final HashMap<UUID, HashSet<UUID>> appealLodgedInfo) {
        final JsonArrayBuilder appealsLodged = createArrayBuilder();
        appealLodgedInfo.keySet().forEach(e -> {
            final JsonObjectBuilder appealLodged = createObjectBuilder();
            final JsonArrayBuilder jsonArrayBuilder =
                    appealLodgedInfo
                            .get(e)
                            .stream()
                            .map(UUID::toString)
                            .reduce(createArrayBuilder(), JsonArrayBuilder::add, JsonArrayBuilder::add);

            appealLodged.add(DEFENDANT_ID, e.toString());
            appealLodged.add(OFFENCE_IDS, jsonArrayBuilder.build());
            appealsLodged.add(appealLodged);
        });
        return appealsLodged.build();
    }

    private void populateAppealLodgedInfo(final UUID caseId, final JsonObjectBuilder jsonObjectBuilder) {
        final List<CourtApplicationCaseEntity> courtApplicationCaseEntities = courtApplicationCaseRepository.findByCaseId(caseId);
        final HashMap<UUID, HashSet<UUID>> defendantOffencesMap = new HashMap<>();
        boolean appealLodged = false;
        if (isNotEmpty(courtApplicationCaseEntities)) {
            courtApplicationCaseEntities
                    .stream()
                    .map(e -> jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(e.getCourtApplication().getPayload()), CourtApplication.class))
                    .filter(e -> Boolean.TRUE.equals(e.getType().getAppealFlag()))
                    .filter(e -> nonNull(e.getCourtApplicationCases()))
                    .forEach(e -> {
                        final HashSet<UUID> offenceIds = new HashSet<>();
                        e.getCourtApplicationCases()
                                .stream()
                                .filter(courtApplicationCase -> courtApplicationCase.getProsecutionCaseId().equals(caseId))
                                .filter(courtApplicationCase -> nonNull(courtApplicationCase.getOffences()))
                                .flatMap(courtApplicationCase -> courtApplicationCase.getOffences().stream())
                                .forEach(o -> offenceIds.add(o.getId()));

                        if(!offenceIds.isEmpty() && e.getSubject().getMasterDefendant() != null) {
                            final DefendantCase matchingDefendantCase = e.getSubject().getMasterDefendant().getDefendantCase()
                                    .stream()
                                    .filter(defendantCase -> caseId.equals(defendantCase.getCaseId()))
                                    .findFirst()
                                    .orElse(null);
                            if(matchingDefendantCase != null) {
                                defendantOffencesMap.computeIfAbsent(matchingDefendantCase.getDefendantId(), k -> new HashSet<>());
                                defendantOffencesMap.get(matchingDefendantCase.getDefendantId()).addAll(offenceIds);
                            }
                        }
                    });

                appealLodged = courtApplicationCaseEntities
                    .stream()
                    .map(e -> jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(e.getCourtApplication().getPayload()), CourtApplication.class))
                    .anyMatch(e -> Boolean.TRUE.equals(e.getType().getAppealFlag()));
        }

        final JsonObjectBuilder appealsInfoBuilder = createObjectBuilder();
        appealsInfoBuilder.add(APPEALS_LODGED, appealLodged);

        if (!defendantOffencesMap.isEmpty()) {
            final JsonArray appealsLodged = buildAppealLodgedInfo(defendantOffencesMap);
            appealsInfoBuilder.add(APPEALS_LODGED_FOR, appealsLodged);
        }

        jsonObjectBuilder.add(APPEALS_LODGED_INFO, appealsInfoBuilder);

    }

    private void buildCivilFeesList(final CivilFees civilFee, final JsonArrayBuilder jsonProsecutionBuilder) {
        jsonProsecutionBuilder.add(objectToJsonObjectConverter.convert(CivilFees.civilFees()
                .withFeeId(civilFee.getFeeId())
                .withFeeStatus(civilFee.getFeeStatus())
                .withFeeType(civilFee.getFeeType())
                .withPaymentReference(civilFee.getPaymentReference())
                .build()));
    }

    @Handles("progression.query.prosecutioncase.caag")
    public JsonEnvelope getProsecutionCaseForCaseAtAGlance(final JsonEnvelope envelope) {
        final Optional<UUID> caseId = getUUID(envelope.payloadAsJsonObject(), CASE_ID);
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();

        try {
            final ProsecutionCaseEntity prosecutionCaseEntity = prosecutionCaseRepository.findByCaseId(caseId.get());
            final JsonObject prosecutionCasePayload = stringToJsonObjectConverter.convert(prosecutionCaseEntity.getPayload());
            final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(prosecutionCasePayload, ProsecutionCase.class);
            final List<Hearings> hearingsList = hearingAtAGlanceService.getCaseHearings(caseId.get());
            final CaseAtAGlanceHelper caseAtAGlanceHelper = new CaseAtAGlanceHelper(prosecutionCase, hearingsList, referenceDataService, civilFeeRepository, relatedReferenceRepository);
            final JsonObject caseDetailsJson = objectToJsonObjectConverter.convert(caseAtAGlanceHelper.getCaseDetails());
            final JsonObject prosecutorDetailsJson = objectToJsonObjectConverter.convert(caseAtAGlanceHelper.getProsecutorDetails());
            final List<CourtApplicationCaseEntity> courtApplicationCaseEntities = courtApplicationCaseRepository.findByCaseId(caseId.get());
            final JsonArray caseDefendantsJsonArray = listToJsonArrayConverter.convert(
                    caseAtAGlanceHelper.getCaagDefendantsList(getDefendantIdAndUpdatedOnMap(courtApplicationCaseEntities)));

            jsonObjectBuilder.add(CASE_ID, caseId.get().toString());
            jsonObjectBuilder.add("caseDetails", caseDetailsJson);
            jsonObjectBuilder.add("prosecutorDetails", prosecutorDetailsJson);
            jsonObjectBuilder.add("defendants", resultTextFlagBuilder.rebuildWithResultTextFlag(caseDefendantsJsonArray));

            if (!courtApplicationCaseEntities.isEmpty()) {
                final JsonArrayBuilder jsonApplicationBuilder = Json.createArrayBuilder();
                courtApplicationCaseEntities
                        .stream().filter(courtApplicationCaseEntity -> courtApplicationCaseEntity.getCourtApplication().getParentApplicationId() == null)
                        .forEach(courtApplicationCaseEntity -> buildApplicationSummary(courtApplicationCaseEntity.getCourtApplication().getPayload(), jsonApplicationBuilder));
                jsonObjectBuilder.add("linkedApplications", jsonApplicationBuilder.build());
            }

            final CaseCpsProsecutorEntity caseCpsProsecutorEntity = caseCpsProsecutorRepository.findBy(caseId.get());
            if (nonNull(caseCpsProsecutorEntity) && StringUtils.isNotEmpty(caseCpsProsecutorEntity.getOldCpsProsecutor())) {
                jsonObjectBuilder.add("prosecutorDetails", addProperty(prosecutorDetailsJson, OLD_PROSECUTION_AUTHORITY_CODE, caseCpsProsecutorEntity.getOldCpsProsecutor()));
            }

            populateAppealLodgedInfo(caseId.get(), jsonObjectBuilder);

        } catch (final NoResultException e) {
            LOGGER.warn(NO_CASE_FOUND_YET_FOR_CASE_ID, caseId.get());
        }

        return JsonEnvelope.envelopeFrom(envelope.metadata(), jsonObjectBuilder.build());
    }

    private Map<UUID, LocalDate> getDefendantIdAndUpdatedOnMap(final List<CourtApplicationCaseEntity> courtApplicationCaseEntities) {
        final Map<UUID,LocalDate> defendantUpdatedOn = new HashMap<>();
        ofNullable(courtApplicationCaseEntities).ifPresent(list-> list.forEach(courtApplicationCaseEntity -> {
            final CourtApplication courtApplication = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert
                    (courtApplicationCaseEntity.getCourtApplication().getPayload()), CourtApplication.class);
            if(nonNull(courtApplication.getApplicant()) && nonNull(courtApplication.getApplicant().getUpdatedOn())
                    && nonNull(courtApplication.getApplicant().getMasterDefendant())
                    && !defendantUpdatedOn.containsKey(courtApplication.getApplicant().getMasterDefendant().getMasterDefendantId()) ){
                defendantUpdatedOn.put(courtApplication.getApplicant().getMasterDefendant().getMasterDefendantId(),courtApplication.getApplicant().getUpdatedOn());
            }
            Optional.ofNullable(courtApplication.getRespondents())
                    .ifPresent(respondents -> respondents.stream()
                            .filter(respondent -> nonNull(respondent.getUpdatedOn()) && nonNull(respondent.getMasterDefendant()))
                            .forEach(respondent -> defendantUpdatedOn.computeIfAbsent(respondent.getMasterDefendant().getMasterDefendantId() , key-> respondent.getUpdatedOn())));
        }));
        return defendantUpdatedOn;
    }

    @Handles("progression.query.case")
    public JsonEnvelope getCase(final JsonEnvelope envelope) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        final Optional<UUID> caseId = getUUID(envelope.payloadAsJsonObject(), CASE_ID);
        try {
            final ProsecutionCaseEntity prosecutionCaseEntity = prosecutionCaseRepository.findByCaseId(caseId.get());
            JsonObject prosecutionCase = stringToJsonObjectConverter.convert(prosecutionCaseEntity.getPayload());
            if (!prosecutionCase.containsKey(CASE_STATUS)) {
                prosecutionCase = addProperty(prosecutionCase, CASE_STATUS, CASE_STATUS_ACTIVE);
            }
            jsonObjectBuilder.add(PROSECUTION_CASE, prosecutionCase);
        } catch (final NoResultException e) {
            LOGGER.info(NO_CASE_FOUND_YET_FOR_CASE_ID, caseId.get());
        }

        return JsonEnvelope.envelopeFrom(
                envelope.metadata(),
                jsonObjectBuilder.build());
    }

    @Handles("progression.query.casehearings")
    public JsonEnvelope getCaseHearings(final JsonEnvelope envelope) {
        final Optional<UUID> caseId = JsonObjects.getUUID(envelope.payloadAsJsonObject(), CASE_ID);
        final List<Hearings> hearings = hearingAtAGlanceService.getCaseHearings(caseId.get());
        final JsonObject responsePayload = buildCaseHearingsResponse(hearings);
        return JsonEnvelope.envelopeFrom(envelope.metadata(), responsePayload);
    }

    @Handles("progression.query.case-defendant-hearings")
    public JsonEnvelope getCaseDefendantHearings(final JsonEnvelope envelope) {
        final Optional<UUID> caseId = JsonObjects.getUUID(envelope.payloadAsJsonObject(), CASE_ID);
        final Optional<UUID> defendantId = JsonObjects.getUUID(envelope.payloadAsJsonObject(), DEFENDANT_ID);
        final List<Hearings> hearings = hearingAtAGlanceService.getCaseDefendantHearings(caseId.get(), defendantId.get());
        final JsonObject responsePayload = buildCaseDefendantHearingsResponse(hearings, caseId.get(), defendantId.get());
        return JsonEnvelope.envelopeFrom(envelope.metadata(), responsePayload);
    }

    @Handles("progression.query.case.hearingtypes")
    public JsonEnvelope getCaseHearingTypes(final JsonEnvelope envelope) {
        final JsonObject payloadAsJsonObject = envelope.payloadAsJsonObject();
        final UUID caseId = JsonObjects.getUUID(envelope.payloadAsJsonObject(), CASE_ID)
                .orElseThrow(() -> new IllegalArgumentException("caseId parameter cannot be empty!"));

        final LocalDate orderDate = payloadAsJsonObject.containsKey(ORDER_DATE) ? LocalDate.parse(payloadAsJsonObject.getString(ORDER_DATE)) : LocalDate.now();

        final List<HearingEntity> hearings = hearingAtAGlanceService.getCaseHearingEntities(caseId);
        final JsonObject responsePayload = buildCaseHearingTypesResponse(getHearingTypes(hearings, orderDate));
        return JsonEnvelope.envelopeFrom(envelope.metadata(), responsePayload);
    }

    @Handles("progression.query.cotr-trial-hearings")
    public JsonEnvelope getTrialHearings(final JsonEnvelope envelope) {
        final UUID prosecutionCaseId = getProsecutionCaseId(envelope);
        final List<TrialHearing> trialHearings = hearingAtAGlanceService.getTrialHearings(prosecutionCaseId);

        final JsonArrayBuilder trialHearingsBuilder = createArrayBuilder();
        trialHearings.forEach(trialHearing -> {
            final JsonObject trialHearingJsonObject = objectToJsonObjectConverter.convert(trialHearing);
            trialHearingsBuilder.add(trialHearingJsonObject);
        });

        final JsonObject responsePayload = createObjectBuilder().add("trialHearings", trialHearingsBuilder).build();
        return JsonEnvelope.envelopeFrom(envelope.metadata(), responsePayload);
    }

    @Handles("progression.query.cotr-details")
    public JsonEnvelope getCotrDetails(final JsonEnvelope envelope) {
        final UUID prosecutionCaseId = getProsecutionCaseId(envelope);
        final List<CotrDetail> cotrDetails = cotrQueryService.getCotrDetailsForAProsecutionCase(prosecutionCaseId);

        final JsonArrayBuilder cotrDetailsBuilder = createArrayBuilder();
        cotrDetails.forEach(cotrDetail -> {
            final JsonObject cotrDetailsJsonObject = objectToJsonObjectConverter.convert(cotrDetail);
            cotrDetailsBuilder.add(cotrDetailsJsonObject);
        });

        final JsonObject responsePayload = createObjectBuilder().add("cotrDetails", cotrDetailsBuilder).build();
        return JsonEnvelope.envelopeFrom(envelope.metadata(), responsePayload);
    }

    @Handles("progression.query.cotr-form")
    public JsonEnvelope getCotrForm(final JsonEnvelope envelope) {
        final UUID prosecutionCaseId = getProsecutionCaseId(envelope);
        final UUID cotrId = getCotrId(envelope);
        final JsonObject cotrForm = cotrQueryService.getCotrFormForAProsecutionCaseAndCotr(prosecutionCaseId, cotrId);

        final JsonObject responsePayload = createObjectBuilder().add("cotrForm", cotrForm).build();
        return JsonEnvelope.envelopeFrom(envelope.metadata(), responsePayload);
    }

    private UUID getProsecutionCaseId(final JsonEnvelope envelope) {
        return JsonObjects.getUUID(envelope.payloadAsJsonObject(), "prosecutionCaseId").get();
    }

    private UUID getCotrId(final JsonEnvelope envelope) {
        return JsonObjects.getUUID(envelope.payloadAsJsonObject(), "cotrId").get();
    }

    @Handles("progression.query.case.allhearingtypes")
    public JsonEnvelope getCaseAllHearingTypes(final JsonEnvelope envelope) {
        final UUID caseId = JsonObjects.getUUID(envelope.payloadAsJsonObject(), CASE_ID)
                .orElseThrow(() -> new IllegalArgumentException("caseId parameter cannot be empty!"));

        final List<HearingEntity> hearings = hearingAtAGlanceService.getCaseHearingEntities(caseId);
        final JsonObject responsePayload = buildCaseHearingTypesResponse(getAllHearingTypes(hearings));
        return JsonEnvelope.envelopeFrom(envelope.metadata(), responsePayload);
    }


    private void addCourtApplication(final GetHearingsAtAGlance getHearingAtAGlance, final List<CourtApplicationCaseEntity> courtApplicationEntities) {
        getHearingAtAGlance.getCourtApplications()
                .addAll(courtApplicationEntities.stream()
                        .map(o -> jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(o.getCourtApplication().getPayload()), CourtApplication.class))
                        .collect(Collectors.toList()));
    }


    @Handles("progression.query.usergroups-by-material-id")
    public JsonEnvelope searchByMaterialId(final JsonEnvelope envelope) {

        LOGGER.debug("Searching for allowed user groups with materialId='{}'", FIELD_QUERY);
        final JsonObjectBuilder json = createObjectBuilder();
        final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        final CourtDocumentMaterialEntity courtDocumentMaterialEntity = courtDocumentMaterialRepository.findBy(UUID
                .fromString(envelope.payloadAsJsonObject().getString(FIELD_QUERY)));
        if (courtDocumentMaterialEntity != null) {
            courtDocumentMaterialEntity.getUserGroups().forEach(jsonArrayBuilder::add);
        } else {
            LOGGER.info("No user groups found with materialId='{}'", FIELD_QUERY);
        }
        json.add("allowedUserGroups", jsonArrayBuilder.build());
        return JsonEnvelope.envelopeFrom(
                envelope.metadata(),
                json.build());
    }

    @Handles("progression.query.search-cases")
    public JsonEnvelope searchCase(final JsonEnvelope envelope) {
        final String searchCriteria = envelope.payloadAsJsonObject().getString(FIELD_QUERY);
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        if (StringUtils.isNotBlank(searchCriteria)) {
            final List<SearchProsecutionCaseEntity> cases = searchCaseRepository.findBySearchCriteria(prepareSearch
                    (searchCriteria.toLowerCase()));
            final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
            cases.forEach(caseEntity ->
                    jsonArrayBuilder.add(stringToJsonObjectConverter.convert(SearchCaseBuilder.searchCaseBuilder()
                            .withSearchCaseEntity(caseEntity)
                            .withDefendantFullName()
                            .withResultPayload()
                            .build().getResultPayload())));
            jsonObjectBuilder.add(SEARCH_RESULT, jsonArrayBuilder.build());
        }

        return JsonEnvelope.envelopeFrom(
                envelope.metadata(),
                jsonObjectBuilder.build());
    }

    @Handles("progression.query.search-cases-by-caseurn")
    public JsonEnvelope searchCaseByCaseUrn(final JsonEnvelope envelope) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        if (envelope.payloadAsJsonObject().containsKey(FIELD_CASE_URN) && StringUtils.isNotBlank(envelope.payloadAsJsonObject().getString(FIELD_CASE_URN))) {
            final List<SearchProsecutionCaseEntity> cases = searchCaseRepository.findByCaseUrn(envelope.payloadAsJsonObject().getString(FIELD_CASE_URN).toUpperCase());
            final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
            cases.forEach(caseEntity ->
                    jsonArrayBuilder.add(stringToJsonObjectConverter.convert(SearchCaseBuilder.searchCaseBuilder()
                            .withSearchCaseEntity(caseEntity)
                            .withDefendantFullName()
                            .withResultPayload()
                            .build().getResultPayload())));
            jsonObjectBuilder.add(SEARCH_RESULT, jsonArrayBuilder.build());
        }

        return JsonEnvelope.envelopeFrom(
                envelope.metadata(),
                jsonObjectBuilder.build());
    }

    @Handles("progression.query.case-exist-by-caseurn")
    public JsonEnvelope caseExistsByCaseUrn(final JsonEnvelope envelope) {
        final String caseUrn = envelope.payloadAsJsonObject().getString(FIELD_CASE_URN);
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();

        if (StringUtils.isNotBlank(caseUrn)) {
            final List<SearchProsecutionCaseEntity> searchResult = searchCaseRepository.findByCaseUrn(caseUrn.toUpperCase());
            if (CollectionUtils.isNotEmpty(searchResult)) {
                jsonObjectBuilder.add(CASE_ID, searchResult.get(0).getCaseId());
            }
        }

        return JsonEnvelope.envelopeFrom(
                envelope.metadata(),
                jsonObjectBuilder.build());
    }


    @Handles("progression.query.prosecutionauthorityid-by-case-id")
    public JsonEnvelope searchProsecutionAuthorityId(final JsonEnvelope envelope) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        final JsonArrayBuilder jsonArrayBuilder = createArrayBuilder();
        final String strCaseIds =
                JsonObjects.getString(envelope.payloadAsJsonObject(), CASE_IDS_SEARCH_PARAM)
                        .orElse(null);
        if (StringUtils.isNotEmpty(strCaseIds)) {
            final List<UUID> caseIdList = commaSeparatedUuidParam2UUIDs(strCaseIds);
            final List<ProsecutionCaseEntity> prosecutionCaseEntities = prosecutionCaseRepository.findByProsecutionCaseIds(caseIdList);
            prosecutionCaseEntities.forEach(prosecutionCaseEntity -> {
                final JsonObject payloadEntity = stringToJsonObjectConverter.convert(prosecutionCaseEntity.getPayload());
                final JsonObject prosecutionCaseIdentifier = payloadEntity.getJsonObject(PROSECUTION_CASE_IDENTIFIER);
                final Prosecutor prosecutor = new Prosecutor();
                prosecutor.setCaseId(prosecutionCaseEntity.getCaseId());
                prosecutor.setProsecutionAuthorityId(UUID.fromString(prosecutionCaseIdentifier.getString(PROSECUTION_AUTHORITY_ID)));
                final JsonObjectBuilder prosecutorObject = createObjectBuilder();
                prosecutorObject.add(PROSECUTOR_OBJECT, objectToJsonObjectConverter.convert(prosecutor));
                jsonArrayBuilder.add(prosecutorObject);
            });
        }
        jsonObjectBuilder.add("prosecutors", jsonArrayBuilder);
        return JsonEnvelope.envelopeFrom(
                envelope.metadata(),
                jsonObjectBuilder.build());
    }

    @Handles("progression.query.prosecutorid-prosecutionauthorityid-by-case-id")
    public JsonEnvelope searchProsecutorIdProsecutionAuthorityId(final JsonEnvelope envelope) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        final JsonArrayBuilder jsonArrayBuilder = createArrayBuilder();
        final String strCaseIds =
                JsonObjects.getString(envelope.payloadAsJsonObject(), CASE_IDS_SEARCH_PARAM)
                        .orElse(null);
        if (StringUtils.isNotEmpty(strCaseIds)) {
            final List<UUID> caseIdList = commaSeparatedUuidParam2UUIDs(strCaseIds);
            final List<ProsecutionCaseEntity> prosecutionCaseEntities = prosecutionCaseRepository.findByProsecutionCaseIds(caseIdList);
            prosecutionCaseEntities.forEach(prosecutionCaseEntity -> {
                final JsonObject payloadEntity = stringToJsonObjectConverter.convert(prosecutionCaseEntity.getPayload());
                final JsonObject prosecutorObjectInEntity = payloadEntity.getJsonObject(PROSECUTOR_OBJECT);
                final UUID prosecutorId  = (null != prosecutorObjectInEntity && prosecutorObjectInEntity.containsKey(PROSECUTOR_ID))
                        ? UUID.fromString(prosecutorObjectInEntity.getString(PROSECUTOR_ID)) : null;
                final JsonObject prosecutionCaseIdentifier = payloadEntity.getJsonObject(PROSECUTION_CASE_IDENTIFIER);
                final Prosecutor prosecutor = new Prosecutor();
                prosecutor.setCaseId(prosecutionCaseEntity.getCaseId());
                prosecutor.setProsecutionAuthorityId(UUID.fromString(prosecutionCaseIdentifier.getString(PROSECUTION_AUTHORITY_ID)));
                prosecutor.setProsecutorId(prosecutorId);
                final JsonObjectBuilder prosecutorObject = createObjectBuilder();
                prosecutorObject.add(PROSECUTOR_OBJECT, objectToJsonObjectConverter.convert(prosecutor));
                jsonArrayBuilder.add(prosecutorObject);
            });
        }
        jsonObjectBuilder.add("prosecutors", jsonArrayBuilder);
        return JsonEnvelope.envelopeFrom(
                envelope.metadata(),
                jsonObjectBuilder.build());
    }

    @Handles("progression.query.cotr.details.prosecutioncase")
    public JsonEnvelope getCotrDetailsByCaseId(final JsonEnvelope envelope) {
        final UUID prosecutionCaseId = getProsecutionCaseId(envelope);
        final List<CotrDetail> cotrDetails = cotrQueryService.getCotrDetailsForAProsecutionCaseByLatestHearingDate(prosecutionCaseId);

        final JsonArrayBuilder cotrDetailsBuilder = createArrayBuilder();
        cotrDetails.forEach(cotrDetail -> {
            final JsonObject cotrDetailsJsonObject = objectToJsonObjectConverter.convert(cotrDetail);
            cotrDetailsBuilder.add(cotrDetailsJsonObject);
        });

        final JsonObject responsePayload = createObjectBuilder().add("cotrDetails", cotrDetailsBuilder).build();
        return JsonEnvelope.envelopeFrom(envelope.metadata(), responsePayload);
    }

    public JsonEnvelope getAllCaseHearings(final JsonEnvelope envelope) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        final Optional<UUID> caseId = getUUID(envelope.payloadAsJsonObject(), CASE_ID);
        if(caseId.isPresent()) {
            try {
                jsonObjectBuilder.add(ALL_CASE_HEARINGS, objectToJsonObjectConverter.convert(getHearingsAtAGlance(jsonObjectBuilder, caseId)));
            } catch (final NoResultException e) {
                LOGGER.info(NO_CASE_FOUND_YET_FOR_CASE_ID, caseId.get());
            }
        }
        return JsonEnvelope.envelopeFrom(
                envelope.metadata(),
                jsonObjectBuilder.build());
    }

    private GetHearingsAtAGlance getHearingsAtAGlance(final JsonObjectBuilder jsonObjectBuilder, final Optional<UUID> caseId) {
        final GetHearingsAtAGlance hearingsAtAGlance = hearingAtAGlanceService.getHearingAtAGlance(caseId.get());
        final List<CourtApplicationCaseEntity> courtApplicationCaseEntities = courtApplicationCaseRepository.findByCaseId(caseId.get());
        if (isNotEmpty(courtApplicationCaseEntities)) {
            final JsonArrayBuilder jsonApplicationBuilder = Json.createArrayBuilder();
            courtApplicationCaseEntities.forEach(courtApplicationCaseEntity -> buildApplicationSummary(courtApplicationCaseEntity.getCourtApplication().getPayload(), jsonApplicationBuilder));
            jsonObjectBuilder.add(LINKED_APPLICATIONS_SUMMARY, jsonApplicationBuilder.build());
            addCourtApplication(hearingsAtAGlance, courtApplicationCaseEntities);
        }
        return hearingsAtAGlance;
    }

    private void buildApplicationSummary(final String applicationPayload, final JsonArrayBuilder jsonApplicationBuilder) {
        final CourtApplication courtApplication = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert
                (applicationPayload), CourtApplication.class);

        jsonApplicationBuilder.add(objectToJsonObjectConverter.convert(CourtApplicationSummary.applicationSummary()
                .withApplicationId(courtApplication.getId().toString())
                .withApplicationReference(courtApplication.getApplicationReference())
                .withApplicationStatus(courtApplication.getApplicationStatus())
                .withApplicationTitle(courtApplication.getType())
                .withApplicantDisplayName(courtApplication.getApplicant())
                .withApplicantId(getApplicantId(courtApplication))
                .withRespondentDisplayNames(courtApplication.getRespondents())
                .withRespondentIds(courtApplication.getRespondents())
                .withIsAppeal(isAppealApplication(courtApplication))
                .withRemovalReason(courtApplication.getRemovalReason())
                .withSubjectId(getSubjectId(courtApplication.getSubject()))
                .build()));
    }

    public JsonEnvelope getActiveApplicationsOnCase(final JsonEnvelope envelope) {
        final UUID prosecutionCaseId = getProsecutionCaseId(envelope);
        final JsonArrayBuilder jsonApplicationBuilder = Json.createArrayBuilder();
        final List<CourtApplicationCaseEntity> courtApplicationCaseEntities = courtApplicationCaseRepository.findByCaseId(prosecutionCaseId);

        if (!courtApplicationCaseEntities.isEmpty()) {
            courtApplicationCaseEntities
                .forEach(courtApplicationCaseEntity -> {
                    final CourtApplication courtApplication = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert
                            (courtApplicationCaseEntity.getCourtApplication().getPayload()), CourtApplication.class);
                    if(courtApplication.getApplicationStatus()!=null && !courtApplication.getApplicationStatus().equals(ApplicationStatus.FINALISED)){
                        final List<HearingApplicationEntity> applicationHearingEntities = hearingApplicationRepository.findByApplicationId(courtApplication.getId());
                        final JsonArrayBuilder hearingArrayBuilder = createArrayBuilder();
                        if(isNotEmpty(applicationHearingEntities)) {
                            applicationHearingEntities.stream()
                                    .filter(entity -> nonNull(entity.getHearing()) && !HearingListingStatus.HEARING_RESULTED.equals(entity.getHearing().getListingStatus()))
                                    .forEach(entity -> hearingArrayBuilder.add(entity.getHearing().getHearingId().toString()));
                        }
                        jsonApplicationBuilder.add(Json.createObjectBuilder()
                                .add("applicationId", courtApplication.getId().toString())
                                .add("hearingIds", hearingArrayBuilder.build()).build());
                    }
                });
        }
        return JsonEnvelope.envelopeFrom(envelope.metadata(), createObjectBuilder().add("linkedApplications", jsonApplicationBuilder.build()).build());
    }

    private UUID getSubjectId(final CourtApplicationParty subject) {
        return isNull(subject) || isNull(subject.getMasterDefendant()) ? null : subject.getMasterDefendant().getMasterDefendantId();
    }

    private UUID getApplicantId(final CourtApplication courtApplication) {
        return isNull(courtApplication.getApplicant()) || isNull(courtApplication.getApplicant().getMasterDefendant()) ? null : courtApplication.getApplicant().getMasterDefendant().getMasterDefendantId();
    }

    private void buildRelatedCasesForDefendant(final UUID masterDefendantId, final List<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntityList, final JsonArrayBuilder relatedCasesArrayBuilder, final String statusOfPrimaryCase) {
        final JsonObjectBuilder relatedCaseObjectBuilder = Json.createObjectBuilder();
        final JsonArrayBuilder casesArrayBuilder = Json.createArrayBuilder();
        final List<MatchDefendantCaseHearingEntity> uniqueMatchDefendantCaseHearingEntityList = matchDefendantCaseHearingEntityList.stream()
                .collect(collectingAndThen(toCollection(() -> new TreeSet<>(Comparator.comparing(MatchDefendantCaseHearingEntity::getProsecutionCaseId))), ArrayList::new));

        uniqueMatchDefendantCaseHearingEntityList.forEach(matchDefendantCaseHearingEntity -> buildCases(matchDefendantCaseHearingEntity, casesArrayBuilder, statusOfPrimaryCase));

        final JsonArray cases = casesArrayBuilder.build();
        if (isNotEmpty(cases)) {
            relatedCaseObjectBuilder.add("masterDefendantId", masterDefendantId.toString());
            relatedCaseObjectBuilder.add("cases", cases);
        }

        relatedCasesArrayBuilder.add(relatedCaseObjectBuilder.build());
    }

    private void buildCases(final MatchDefendantCaseHearingEntity matchDefendantCaseHearingEntity, final JsonArrayBuilder casesArrayBuilder, final String statusOfPrimaryCase) {
        final JsonObject prosecutionCaseJson = stringToJsonObjectConverter.convert(matchDefendantCaseHearingEntity.getProsecutionCase().getPayload());
        final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);
        final String prosecutionCaseStatus = Optional.ofNullable(prosecutionCase.getCaseStatus()).orElse(CASE_STATUS_ACTIVE);

        if ((statusOfPrimaryCase.equals(CASE_STATUS_ACTIVE) && !statusOfPrimaryCase.equals(prosecutionCaseStatus)) ||
                (!statusOfPrimaryCase.equals(CASE_STATUS_ACTIVE) && prosecutionCaseStatus.equals(CASE_STATUS_ACTIVE))) {
            return;
        }

        final JsonArrayBuilder offencesArrayBuilder = Json.createArrayBuilder();
        prosecutionCase.getDefendants().stream()
                .filter(defendant -> defendant.getMasterDefendantId().equals(matchDefendantCaseHearingEntity.getMasterDefendantId()))
                .flatMap(defendant -> defendant.getOffences().stream())
                .collect(Collectors.toList()).stream()
                .map(offence -> objectToJsonObjectConverter.convert(offence))
                .collect(Collectors.toList())
                .forEach(offencesArrayBuilder::add);

        final JsonArray offences = offencesArrayBuilder.build();
        if (isNotEmpty(offences)) {
            final JsonObjectBuilder caseObjectBuilder = Json.createObjectBuilder();
            caseObjectBuilder.add(CASE_ID, prosecutionCase.getId().toString());
            caseObjectBuilder.add(CASE_STATUS, prosecutionCaseStatus);
            if (nonNull(prosecutionCase.getProsecutionCaseIdentifier())) {
                final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCase.getProsecutionCaseIdentifier();
                final JsonObjectBuilder pciJsonBuilder = Json.createObjectBuilder();

                pciJsonBuilder.add(PROSECUTION_AUTHORITY_ID, prosecutionCaseIdentifier.getProsecutionAuthorityId().toString());
                pciJsonBuilder.add("prosecutionAuthorityCode", prosecutionCaseIdentifier.getProsecutionAuthorityCode());

                if (nonNull(prosecutionCaseIdentifier.getProsecutionAuthorityReference())) {
                    pciJsonBuilder.add("prosecutionAuthorityReference", prosecutionCaseIdentifier.getProsecutionAuthorityReference());
                }
                if (nonNull(prosecutionCaseIdentifier.getCaseURN())) {
                    pciJsonBuilder.add("caseURN", prosecutionCaseIdentifier.getCaseURN());
                }
                caseObjectBuilder.add(PROSECUTION_CASE_IDENTIFIER, pciJsonBuilder.build());
            }
            caseObjectBuilder.add("offences", offences);
            casesArrayBuilder.add(caseObjectBuilder.build());
        }
    }

    private Boolean isAppealApplication(final CourtApplication courtApplication) {
        return nonNull(courtApplication.getType()) && nonNull(courtApplication.getType().getAppealFlag())
                && (courtApplication.getType().getAppealFlag() &&
                !(ApplicationStatus.FINALISED == courtApplication.getApplicationStatus() || ApplicationStatus.EJECTED == courtApplication.getApplicationStatus()));
    }

    private List<UUID> retrieveMasterDefendantIdList(final UUID caseId) {
        final ProsecutionCaseEntity prosecutionCaseEntity = prosecutionCaseRepository.findByCaseId(caseId);
        final JsonObject prosecutionCasePayload = stringToJsonObjectConverter.convert(prosecutionCaseEntity.getPayload());
        final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(prosecutionCasePayload, ProsecutionCase.class);
        return prosecutionCase.getDefendants().stream()
                .map(Defendant::getMasterDefendantId)
                .distinct()
                .collect(Collectors.toList());
    }

    public Map<UUID, String> getHearingTypes(final List<HearingEntity> allHearings, final LocalDate orderDate) {
        return allHearings.stream()
                .filter(hearing -> hasHearingOnDate(hearing, orderDate))
                .collect(Collectors.toMap(HearingEntity::getHearingId, this::getHearingType));
    }

    private Map<UUID, String> getAllHearingTypes(final List<HearingEntity> allHearings) {
        return allHearings.stream()
                .collect(Collectors.toMap(HearingEntity::getHearingId, this::getHearingType));
    }

    private String getHearingType(final HearingEntity hearingEntity) {
        final JsonObject hearingJson = stringToJsonObjectConverter.convert(hearingEntity.getPayload());
        final uk.gov.justice.core.courts.Hearing hearing = jsonObjectToObjectConverter.convert(hearingJson, uk.gov.justice.core.courts.Hearing.class);
        return hearing.getType().getDescription();
    }

    private static boolean hasHearingOnDate(final HearingEntity hearing, final LocalDate orderDate) {
        return Objects.nonNull(hearing.getConfirmedDate()) && hearing.getConfirmedDate().isEqual(orderDate);
    }

    private List<UUID> commaSeparatedUuidParam2UUIDs(final String strUuids) {
        return Stream.of(strUuids.split(","))
                .map(UUID::fromString)
                .collect(Collectors.toList());
    }
}
