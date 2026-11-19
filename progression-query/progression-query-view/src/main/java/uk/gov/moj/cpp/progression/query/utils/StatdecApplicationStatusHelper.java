package uk.gov.moj.cpp.progression.query.utils;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.progression.domain.constant.ResultConstants.AACA;
import static uk.gov.moj.cpp.progression.domain.constant.ResultConstants.AACD;
import static uk.gov.moj.cpp.progression.domain.constant.ResultConstants.AASA;
import static uk.gov.moj.cpp.progression.domain.constant.ResultConstants.AASD;
import static uk.gov.moj.cpp.progression.domain.constant.ResultConstants.ACSD;
import static uk.gov.moj.cpp.progression.domain.constant.ResultConstants.APA;
import static uk.gov.moj.cpp.progression.domain.constant.ResultConstants.AW;
import static uk.gov.moj.cpp.progression.domain.constant.ResultConstants.DISM;
import static uk.gov.moj.cpp.progression.domain.constant.ResultConstants.G;
import static uk.gov.moj.cpp.progression.domain.constant.ResultConstants.RFSD;
import static uk.gov.moj.cpp.progression.domain.constant.ResultConstants.ROPENED;
import static uk.gov.moj.cpp.progression.domain.constant.ResultConstants.STDEC;
import static uk.gov.moj.cpp.progression.domain.constant.ResultConstants.WDRN;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.DefendantCase;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.moj.cpp.progression.enums.StatdecApplicationCategory;
import uk.gov.moj.cpp.progression.enums.StatdecApplicationLabelStatus;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;

/**
 * Builds the applicationStatuses block shown as labels on Case At A Glance (CHD-3304).
 * Each STAT DEC, REOPEN or APPEAL application linked to the case gets one entry, with a status
 * derived from its shared application-level results.
 */
public class StatdecApplicationStatusHelper {

    public static final String STATDEC_APPLICATION_STATUSES = "applicationStatuses";
    public static final String STATDEC_APPLICATION_TYPE_GROUPS = "statdec,appeal,reopen";

    private static final String GROUPS = "groups";
    private static final String GROUP_NAME = "groupName";
    private static final String APPLICATION_TYPES = "applicationTypes";
    private static final String APPLICATION_TYPE_ID = "applicationTypeId";
    private static final String APPLICATION_ID = "applicationId";
    private static final String APPLICATION_TYPE_CODE = "applicationTypeCode";
    private static final String APPLICATION_CATEGORY = "applicationCategory";
    private static final String STATUS = "status";
    private static final String DEFENDANT_OFFENCES = "defendantOffences";
    private static final String DEFENDANT_ID = "defendantId";
    private static final String OFFENCE_IDS = "offenceIds";

    // Reference data group name for each category, matched ignoring case.
    private static final Map<String, StatdecApplicationCategory> GROUP_CATEGORIES = Map.of(
            "statdec", StatdecApplicationCategory.STAT_DEC,
            "reopen", StatdecApplicationCategory.REOPEN,
            "appeal", StatdecApplicationCategory.APPEAL);

    // Keyed by category first: G, RFSD, WDRN and DISM mean different things per category.
    private static final Map<StatdecApplicationCategory, Map<UUID, StatdecApplicationLabelStatus>> RESULT_STATUS_MAP = Map.of(
            StatdecApplicationCategory.STAT_DEC, Map.of(
                    G, StatdecApplicationLabelStatus.STATUTORY_DECLARATION_GRANTED,
                    STDEC, StatdecApplicationLabelStatus.STATUTORY_DECLARATION_GRANTED,
                    RFSD, StatdecApplicationLabelStatus.STATUTORY_DECLARATION_REFUSED,
                    WDRN, StatdecApplicationLabelStatus.STATUTORY_DECLARATION_WITHDRAWN,
                    DISM, StatdecApplicationLabelStatus.STATUTORY_DECLARATION_DISMISSED),
            StatdecApplicationCategory.REOPEN, Map.of(
                    G, StatdecApplicationLabelStatus.REOPENING_GRANTED,
                    ROPENED, StatdecApplicationLabelStatus.REOPENING_GRANTED,
                    RFSD, StatdecApplicationLabelStatus.REOPENING_REFUSED,
                    WDRN, StatdecApplicationLabelStatus.REOPENING_WITHDRAWN,
                    DISM, StatdecApplicationLabelStatus.REOPENING_DISMISSED),
            StatdecApplicationCategory.APPEAL, Map.of(
                    AACA, StatdecApplicationLabelStatus.APPEAL_ALLOWED,
                    AASA, StatdecApplicationLabelStatus.APPEAL_ALLOWED,
                    RFSD, StatdecApplicationLabelStatus.APPEAL_REFUSED,
                    AW, StatdecApplicationLabelStatus.APPEAL_WITHDRAWN,
                    WDRN, StatdecApplicationLabelStatus.APPEAL_WITHDRAWN,
                    APA, StatdecApplicationLabelStatus.APPEAL_WITHDRAWN,
                    AACD, StatdecApplicationLabelStatus.APPEAL_DISMISSED,
                    AASD, StatdecApplicationLabelStatus.APPEAL_DISMISSED,
                    ACSD, StatdecApplicationLabelStatus.APPEAL_DISMISSED));

    private StatdecApplicationStatusHelper() {
    }

    /**
     * Maps each application type id in the reference data application-type-groups response to its category.
     */
    public static Map<UUID, StatdecApplicationCategory> getApplicationTypeCategories(final JsonObject applicationTypeGroups) {
        final Map<UUID, StatdecApplicationCategory> categories = new HashMap<>();
        if (isNull(applicationTypeGroups) || !applicationTypeGroups.containsKey(GROUPS)) {
            return categories;
        }
        applicationTypeGroups.getJsonArray(GROUPS).getValuesAs(JsonObject.class).forEach(group ->
                categoryForGroup(group.getString(GROUP_NAME, null)).ifPresent(category ->
                        ofNullable(group.getJsonArray(APPLICATION_TYPES)).ifPresent(applicationTypes ->
                                applicationTypes.getValuesAs(JsonObject.class).stream()
                                        .map(applicationType -> applicationType.get(APPLICATION_TYPE_ID))
                                        .filter(JsonString.class::isInstance)
                                        .map(id -> UUID.fromString(((JsonString) id).getString()))
                                        .forEach(id -> categories.put(id, category)))));
        return categories;
    }

    private static Optional<StatdecApplicationCategory> categoryForGroup(final String groupName) {
        return ofNullable(groupName).map(name -> GROUP_CATEGORIES.get(name.toLowerCase(Locale.ROOT)));
    }

    static StatdecApplicationLabelStatus resolveStatus(final StatdecApplicationCategory category, final CourtApplication courtApplication) {
        final List<JudicialResult> applicationResults = ofNullable(courtApplication.getJudicialResults()).orElse(List.of()).stream()
                .filter(Objects::nonNull)
                .filter(judicialResult -> !ofNullable(judicialResult.getIsDeleted()).orElse(false))
                .toList();
        if (applicationResults.isEmpty()) {
            return StatdecApplicationLabelStatus.LODGED;
        }
        final Map<UUID, StatdecApplicationLabelStatus> resultStatuses = RESULT_STATUS_MAP.get(category);
        return applicationResults.stream()
                .map(JudicialResult::getJudicialResultTypeId)
                .filter(Objects::nonNull)
                .map(resultStatuses::get)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(StatdecApplicationLabelStatus.PENDING);
    }

    public static JsonArray buildApplicationStatuses(final UUID caseId,
                                                           final Collection<CourtApplication> courtApplications,
                                                           final Map<UUID, StatdecApplicationCategory> applicationTypeCategories) {
        final Map<UUID, CourtApplication> uniqueApplications = new LinkedHashMap<>();
        courtApplications.forEach(courtApplication -> uniqueApplications.putIfAbsent(courtApplication.getId(), courtApplication));

        final JsonArrayBuilder statusesBuilder = createArrayBuilder();
        uniqueApplications.values().stream()
                .filter(courtApplication -> nonNull(courtApplication.getType()) && nonNull(courtApplication.getType().getId()))
                .forEach(courtApplication -> {
                    final StatdecApplicationCategory category = applicationTypeCategories.get(courtApplication.getType().getId());
                    if (nonNull(category)) {
                        statusesBuilder.add(buildApplicationStatus(caseId, courtApplication, category));
                    }
                });
        return statusesBuilder.build();
    }

    private static JsonObject buildApplicationStatus(final UUID caseId, final CourtApplication courtApplication, final StatdecApplicationCategory category) {
        final JsonObjectBuilder statusBuilder = createObjectBuilder()
                .add(APPLICATION_ID, courtApplication.getId().toString());
        ofNullable(courtApplication.getType().getCode()).ifPresent(code -> statusBuilder.add(APPLICATION_TYPE_CODE, code));
        statusBuilder.add(APPLICATION_CATEGORY, category.name())
                .add(STATUS, resolveStatus(category, courtApplication).name());

        final Set<UUID> offenceIds = getOffenceIds(caseId, courtApplication);
        getDefendantId(caseId, courtApplication).filter(defendantId -> !offenceIds.isEmpty()).ifPresent(defendantId -> {
            final JsonArrayBuilder offenceIdsBuilder = createArrayBuilder();
            offenceIds.forEach(offenceId -> offenceIdsBuilder.add(offenceId.toString()));
            statusBuilder.add(DEFENDANT_OFFENCES, createArrayBuilder().add(createObjectBuilder()
                    .add(DEFENDANT_ID, defendantId.toString())
                    .add(OFFENCE_IDS, offenceIdsBuilder)));
        });
        return statusBuilder.build();
    }

    private static Set<UUID> getOffenceIds(final UUID caseId, final CourtApplication courtApplication) {
        final Set<UUID> offenceIds = new LinkedHashSet<>();
        ofNullable(courtApplication.getCourtApplicationCases()).orElse(List.of()).stream()
                .filter(courtApplicationCase -> caseId.equals(courtApplicationCase.getProsecutionCaseId()))
                .filter(courtApplicationCase -> nonNull(courtApplicationCase.getOffences()))
                .flatMap(courtApplicationCase -> courtApplicationCase.getOffences().stream())
                .forEach(offence -> offenceIds.add(offence.getId()));
        return offenceIds;
    }

    private static Optional<UUID> getDefendantId(final UUID caseId, final CourtApplication courtApplication) {
        return ofNullable(courtApplication.getSubject())
                .map(CourtApplicationParty::getMasterDefendant)
                .map(MasterDefendant::getDefendantCase)
                .flatMap(defendantCases -> defendantCases.stream()
                        .filter(defendantCase -> caseId.equals(defendantCase.getCaseId()))
                        .map(DefendantCase::getDefendantId)
                        .filter(Objects::nonNull)
                        .findFirst());
    }
}
