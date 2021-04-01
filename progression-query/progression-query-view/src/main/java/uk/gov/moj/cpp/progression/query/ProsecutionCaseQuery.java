package uk.gov.moj.cpp.progression.query;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.messaging.JsonObjects.getUUID;
import static uk.gov.moj.cpp.progression.domain.helper.JsonHelper.addProperty;
import static uk.gov.moj.cpp.progression.query.utils.CaseHearingsQueryHelper.buildCaseHearingsResponse;
import static uk.gov.moj.cpp.progression.query.utils.SearchQueryUtils.prepareSearch;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.progression.courts.CaagDefendants;
import uk.gov.justice.progression.courts.GetHearingsAtAGlance;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.progression.query.view.CaseAtAGlanceHelper;
import uk.gov.moj.cpp.progression.query.view.service.HearingAtAGlanceService;
import uk.gov.moj.cpp.progression.query.view.service.ReferenceDataService;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseCpsProsecutorEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentMaterialEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.MatchDefendantCaseHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.SearchProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.utils.CourtApplicationSummary;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.utils.SearchCaseBuilder;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseCpsProsecutorRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentMaterialRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.MatchDefendantCaseHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.SearchProsecutionCaseRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.NoResultException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S3655", "squid:S1612", "squid:S1067", "squid:S1166", "squid:S3252"})
@ServiceComponent(Component.QUERY_VIEW)
public class ProsecutionCaseQuery {

    public static final String PROSECUTION_CASE_IDENTIFIER = "prosecutionCaseIdentifier";
    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionCaseQuery.class);
    private static final String CASE_ID = "caseId";
    private static final String FIELD_QUERY = "q";
    private static final String SEARCH_RESULT = "searchResults";
    public static final String OLD_PROSECUTION_AUTHORITY_CODE = "oldProsecutionAuthorityCode";
    public static final String HEARINGS_AT_A_GLANCE = "hearingsAtAGlance";
    public static final String PROSECUTION_CASE = "prosecutionCase";
    public static final String CASE_STATUS = "caseStatus";
    public static final String CASE_STATUS_ACTIVE = "ACTIVE";

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
    private MatchDefendantCaseHearingRepository matchDefendantCaseHearingRepository;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private CaseCpsProsecutorRepository caseCpsProsecutorRepository;

    @Handles("progression.query.prosecutioncase")
    public JsonEnvelope getProsecutionCase(final JsonEnvelope envelope) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        final Optional<UUID> caseId = getUUID(envelope.payloadAsJsonObject(), CASE_ID);
        try {
            final ProsecutionCaseEntity prosecutionCaseEntity = prosecutionCaseRepository.findByCaseId(caseId.get());
            JsonObject prosecutionCase = stringToJsonObjectConverter.convert(prosecutionCaseEntity.getPayload());
            if(!prosecutionCase.containsKey(CASE_STATUS)){
                prosecutionCase = addProperty(prosecutionCase, CASE_STATUS, CASE_STATUS_ACTIVE);
            }
            jsonObjectBuilder.add(PROSECUTION_CASE, prosecutionCase);

            final GetHearingsAtAGlance hearingsAtAGlance = hearingAtAGlanceService.getHearingAtAGlance(caseId.get());
            final List<CourtApplicationCaseEntity> courtApplicationCaseEntities = courtApplicationCaseRepository.findByCaseId(caseId.get());

            if (!courtApplicationCaseEntities.isEmpty()) {
                final JsonArrayBuilder jsonApplicationBuilder = Json.createArrayBuilder();
                courtApplicationCaseEntities.forEach(courtApplicationCaseEntity -> buildApplicationSummary(courtApplicationCaseEntity.getCourtApplication().getPayload(), jsonApplicationBuilder));
                jsonObjectBuilder.add("linkedApplicationsSummary", jsonApplicationBuilder.build());
                addCourtApplication(hearingsAtAGlance, courtApplicationCaseEntities);
            }

            final JsonObject getCaseAtAGlanceJson = objectToJsonObjectConverter.convert(hearingsAtAGlance);
            jsonObjectBuilder.add(HEARINGS_AT_A_GLANCE, getCaseAtAGlanceJson);

            final CaseCpsProsecutorEntity caseCpsProsecutorEntity = caseCpsProsecutorRepository.findBy(caseId.get());
            if (nonNull(caseCpsProsecutorEntity) && StringUtils.isNotEmpty(caseCpsProsecutorEntity.getOldCpsProsecutor())) {
                final String oldCpsProsecutor = caseCpsProsecutorEntity.getOldCpsProsecutor();
                JsonObject prosecutionCaseIdentifier = addProperty(prosecutionCase.getJsonObject(PROSECUTION_CASE_IDENTIFIER), OLD_PROSECUTION_AUTHORITY_CODE, oldCpsProsecutor);
                jsonObjectBuilder.add(PROSECUTION_CASE, addProperty(prosecutionCase, PROSECUTION_CASE_IDENTIFIER, prosecutionCaseIdentifier));

                prosecutionCaseIdentifier = addProperty(getCaseAtAGlanceJson.getJsonObject(PROSECUTION_CASE_IDENTIFIER), OLD_PROSECUTION_AUTHORITY_CODE, oldCpsProsecutor);
                jsonObjectBuilder.add(HEARINGS_AT_A_GLANCE, addProperty(getCaseAtAGlanceJson, PROSECUTION_CASE_IDENTIFIER, prosecutionCaseIdentifier));
            }

            final List<UUID> masterDefendantIds = retrieveMasterDefendantIdList(caseId.get());

            final List<MatchDefendantCaseHearingEntity> matchedCases = matchDefendantCaseHearingRepository.findByMasterDefendantId(masterDefendantIds);

            final Map<UUID, List<MatchDefendantCaseHearingEntity>> matchedCasesGroupedByMasterDefendantId = matchedCases.stream()
                    .filter(matchDefendantCaseHearingEntity -> masterDefendantIds.contains(matchDefendantCaseHearingEntity.getMasterDefendantId()))
                    .filter(matchDefendantCaseHearingEntity -> !matchDefendantCaseHearingEntity.getProsecutionCaseId().equals(caseId.get()))
                    .collect(Collectors.groupingBy(MatchDefendantCaseHearingEntity::getMasterDefendantId));

            final String statusOfPrimaryCase = prosecutionCase.getString(CASE_STATUS);
            final JsonArrayBuilder relatedCasesArrayBuilder = Json.createArrayBuilder();
            matchedCasesGroupedByMasterDefendantId.forEach((masterDefendantId, cases) -> buildRelatedCasesForDefendant(masterDefendantId, cases, relatedCasesArrayBuilder, statusOfPrimaryCase));
            final JsonArray relatedCases = relatedCasesArrayBuilder.build();
            if (isNotEmpty(relatedCases)) {
                jsonObjectBuilder.add("relatedCases", relatedCases);
            }

        } catch (final NoResultException e) {
            LOGGER.info("# No case found yet for caseId '{}'", caseId.get());
        }

        return JsonEnvelope.envelopeFrom(
                envelope.metadata(),
                jsonObjectBuilder.build());

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
            final CaseAtAGlanceHelper caseAtAGlanceHelper = new CaseAtAGlanceHelper(prosecutionCase, hearingsList, referenceDataService);
            final JsonObject caseDetailsJson = objectToJsonObjectConverter.convert(caseAtAGlanceHelper.getCaseDetails());
            final JsonObject prosecutorDetailsJson = objectToJsonObjectConverter.convert(caseAtAGlanceHelper.getProsecutorDetails());
            final JsonArray caseDefendantsJsonArray = listToJsonArrayConverter.convert(caseAtAGlanceHelper.getCaagDefendantsList());
            final List<CourtApplicationCaseEntity> courtApplicationCaseEntities = courtApplicationCaseRepository.findByCaseId(caseId.get());

            jsonObjectBuilder.add(CASE_ID, caseId.get().toString());
            jsonObjectBuilder.add("caseDetails", caseDetailsJson);
            jsonObjectBuilder.add("prosecutorDetails", prosecutorDetailsJson);
            jsonObjectBuilder.add("defendants", caseDefendantsJsonArray);

            if (!courtApplicationCaseEntities.isEmpty()) {
                final JsonArrayBuilder jsonApplicationBuilder = Json.createArrayBuilder();
                courtApplicationCaseEntities.forEach(courtApplicationCaseEntity -> buildApplicationSummary(courtApplicationCaseEntity.getCourtApplication().getPayload(), jsonApplicationBuilder));
                jsonObjectBuilder.add("linkedApplications", jsonApplicationBuilder.build());
            }

            final CaseCpsProsecutorEntity caseCpsProsecutorEntity = caseCpsProsecutorRepository.findBy(caseId.get());
            if (nonNull(caseCpsProsecutorEntity) && StringUtils.isNotEmpty(caseCpsProsecutorEntity.getOldCpsProsecutor())) {
                jsonObjectBuilder.add("prosecutorDetails", addProperty(prosecutorDetailsJson, OLD_PROSECUTION_AUTHORITY_CODE, caseCpsProsecutorEntity.getOldCpsProsecutor()));
            }

        } catch (final NoResultException e) {
            LOGGER.warn("# No case found yet for caseId '{}'", caseId.get());
        }

        return JsonEnvelope.envelopeFrom(envelope.metadata(), jsonObjectBuilder.build());
    }

    @Handles("progression.query.casehearings")
    public JsonEnvelope getCaseHearings(final JsonEnvelope envelope) {
        final Optional<UUID> caseId = JsonObjects.getUUID(envelope.payloadAsJsonObject(), CASE_ID);
        final List<Hearings> hearings = hearingAtAGlanceService.getCaseHearings(caseId.get());
        final JsonObject responsePayload = buildCaseHearingsResponse(hearings);
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
                    (searchCriteria));
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

    private void buildApplicationSummary(final String applicationPayload, final JsonArrayBuilder jsonApplicationBuilder) {
        final CourtApplication courtApplication = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert
                (applicationPayload), CourtApplication.class);

        jsonApplicationBuilder.add(objectToJsonObjectConverter.convert(CourtApplicationSummary.applicationSummary()
                .withApplicationId(courtApplication.getId().toString())
                .withApplicationReference(courtApplication.getApplicationReference())
                .withApplicationStatus(courtApplication.getApplicationStatus())
                .withApplicationTitle(courtApplication.getType())
                .withApplicantDisplayName(courtApplication.getApplicant())
                .withRespondentDisplayNames(courtApplication.getRespondents())
                .withIsAppeal(isAppealApplication(courtApplication))
                .withRemovalReason(courtApplication.getRemovalReason())
                .build()));
    }

    private void buildRelatedCasesForDefendant(final UUID masterDefendantId, final List<MatchDefendantCaseHearingEntity> matchDefendantCaseHearingEntityList, final JsonArrayBuilder relatedCasesArrayBuilder, final String statusOfPrimaryCase) {
        final JsonObjectBuilder relatedCaseObjectBuilder = Json.createObjectBuilder();
        final JsonArrayBuilder casesArrayBuilder = Json.createArrayBuilder();
        final List<MatchDefendantCaseHearingEntity> uniqueMatchDefendantCaseHearingEntityList = matchDefendantCaseHearingEntityList.stream()
                .collect(collectingAndThen(toCollection(() -> new TreeSet<>(Comparator.comparing(MatchDefendantCaseHearingEntity::getProsecutionCaseId))), ArrayList::new));

        uniqueMatchDefendantCaseHearingEntityList.forEach(matchDefendantCaseHearingEntity -> buildCases(matchDefendantCaseHearingEntity, casesArrayBuilder, statusOfPrimaryCase));

        final JsonArray cases = casesArrayBuilder.build();
        if(isNotEmpty(cases)) {
            relatedCaseObjectBuilder.add("masterDefendantId", masterDefendantId.toString());
            relatedCaseObjectBuilder.add("cases", cases);
        }

        relatedCasesArrayBuilder.add(relatedCaseObjectBuilder.build());
    }

    private void buildCases(final MatchDefendantCaseHearingEntity matchDefendantCaseHearingEntity, final JsonArrayBuilder casesArrayBuilder, final String statusOfPrimaryCase) {
        final JsonObject prosecutionCaseJson = stringToJsonObjectConverter.convert(matchDefendantCaseHearingEntity.getProsecutionCase().getPayload());
        final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);
        final String prosecutionCaseStatus = Optional.ofNullable(prosecutionCase.getCaseStatus()).orElse(CASE_STATUS_ACTIVE);

        if((statusOfPrimaryCase.equals(CASE_STATUS_ACTIVE) && ! statusOfPrimaryCase.equals(prosecutionCaseStatus)) ||
                (! statusOfPrimaryCase.equals(CASE_STATUS_ACTIVE) && prosecutionCaseStatus.equals(CASE_STATUS_ACTIVE)) ){
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

                pciJsonBuilder.add("prosecutionAuthorityId", prosecutionCaseIdentifier.getProsecutionAuthorityId().toString());
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
}
