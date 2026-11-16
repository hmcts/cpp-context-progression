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

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
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
 * Builds the linkedApplicationStatuses block shown as labels on Case At A Glance (CHD-3304).
 * Each STAT DEC, REOPEN or APPEAL application linked to the case gets one entry, with a status
 * derived from its shared application-level results.
 */
public class LinkedApplicationStatusHelper {

    public static final String LINKED_APPLICATION_STATUSES = "linkedApplicationStatuses";
    public static final String APPLICATION_TYPE_GROUPS = "statdec,appeal,reopen";

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

    public enum ApplicationCategory {
        STAT_DEC("statdec"),
        REOPEN("reopen"),
        APPEAL("appeal");

        private final String groupName;

        ApplicationCategory(final String groupName) {
            this.groupName = groupName;
        }

        static Optional<ApplicationCategory> fromGroupName(final String groupName) {
            return Arrays.stream(values()).filter(category -> category.groupName.equalsIgnoreCase(groupName)).findFirst();
        }
    }

    /**
     * Precedence decides the status when an application has more than one mapped result:
     * WITHDRAWN over REFUSED / DISMISSED over GRANTED / ALLOWED.
     */
    public enum ApplicationLabelStatus {
        LODGED(0),
        PENDING(0),
        STATUTORY_DECLARATION_GRANTED(1),
        REOPENING_GRANTED(1),
        APPEAL_ALLOWED(1),
        STATUTORY_DECLARATION_REFUSED(2),
        STATUTORY_DECLARATION_DISMISSED(2),
        REOPENING_REFUSED(2),
        REOPENING_DISMISSED(2),
        APPEAL_REFUSED(2),
        APPEAL_DISMISSED(2),
        STATUTORY_DECLARATION_WITHDRAWN(3),
        REOPENING_WITHDRAWN(3),
        APPEAL_WITHDRAWN(3);

        private final int precedence;

        ApplicationLabelStatus(final int precedence) {
            this.precedence = precedence;
        }
    }

    // Keyed by category first: G, RFSD, WDRN and DISM mean different things per category.
    private static final Map<ApplicationCategory, Map<UUID, ApplicationLabelStatus>> RESULT_STATUS_MAP = Map.of(
            ApplicationCategory.STAT_DEC, Map.of(
                    G, ApplicationLabelStatus.STATUTORY_DECLARATION_GRANTED,
                    STDEC, ApplicationLabelStatus.STATUTORY_DECLARATION_GRANTED,
                    RFSD, ApplicationLabelStatus.STATUTORY_DECLARATION_REFUSED,
                    WDRN, ApplicationLabelStatus.STATUTORY_DECLARATION_WITHDRAWN,
                    DISM, ApplicationLabelStatus.STATUTORY_DECLARATION_DISMISSED),
            ApplicationCategory.REOPEN, Map.of(
                    G, ApplicationLabelStatus.REOPENING_GRANTED,
                    ROPENED, ApplicationLabelStatus.REOPENING_GRANTED,
                    RFSD, ApplicationLabelStatus.REOPENING_REFUSED,
                    WDRN, ApplicationLabelStatus.REOPENING_WITHDRAWN,
                    DISM, ApplicationLabelStatus.REOPENING_DISMISSED),
            ApplicationCategory.APPEAL, Map.of(
                    AACA, ApplicationLabelStatus.APPEAL_ALLOWED,
                    AASA, ApplicationLabelStatus.APPEAL_ALLOWED,
                    RFSD, ApplicationLabelStatus.APPEAL_REFUSED,
                    AW, ApplicationLabelStatus.APPEAL_WITHDRAWN,
                    WDRN, ApplicationLabelStatus.APPEAL_WITHDRAWN,
                    APA, ApplicationLabelStatus.APPEAL_WITHDRAWN,
                    AACD, ApplicationLabelStatus.APPEAL_DISMISSED,
                    AASD, ApplicationLabelStatus.APPEAL_DISMISSED,
                    ACSD, ApplicationLabelStatus.APPEAL_DISMISSED));

    private LinkedApplicationStatusHelper() {
    }

    /**
     * Maps each application type id in the reference data application-type-groups response to its category.
     */
    public static Map<UUID, ApplicationCategory> getApplicationTypeCategories(final JsonObject applicationTypeGroups) {
        final Map<UUID, ApplicationCategory> categories = new HashMap<>();
        if (isNull(applicationTypeGroups) || !applicationTypeGroups.containsKey(GROUPS)) {
            return categories;
        }
        applicationTypeGroups.getJsonArray(GROUPS).getValuesAs(JsonObject.class).forEach(group ->
                ApplicationCategory.fromGroupName(group.getString(GROUP_NAME, null)).ifPresent(category ->
                        ofNullable(group.getJsonArray(APPLICATION_TYPES)).ifPresent(applicationTypes ->
                                applicationTypes.getValuesAs(JsonObject.class).stream()
                                        .map(applicationType -> applicationType.get(APPLICATION_TYPE_ID))
                                        .filter(JsonString.class::isInstance)
                                        .map(id -> UUID.fromString(((JsonString) id).getString()))
                                        .forEach(id -> categories.put(id, category)))));
        return categories;
    }

    public static ApplicationLabelStatus resolveStatus(final ApplicationCategory category, final CourtApplication courtApplication) {
        final List<JudicialResult> applicationResults = ofNullable(courtApplication.getJudicialResults()).orElse(List.of());
        if (applicationResults.isEmpty()) {
            return ApplicationLabelStatus.LODGED;
        }
        final Map<UUID, ApplicationLabelStatus> resultStatuses = RESULT_STATUS_MAP.get(category);
        return applicationResults.stream()
                .map(JudicialResult::getJudicialResultTypeId)
                .filter(Objects::nonNull)
                .map(resultStatuses::get)
                .filter(Objects::nonNull)
                .max(Comparator.comparingInt(status -> status.precedence))
                .orElse(ApplicationLabelStatus.PENDING);
    }

    public static JsonArray buildLinkedApplicationStatuses(final UUID caseId,
                                                           final Collection<CourtApplication> courtApplications,
                                                           final Map<UUID, ApplicationCategory> applicationTypeCategories) {
        final Map<UUID, CourtApplication> uniqueApplications = new LinkedHashMap<>();
        courtApplications.forEach(courtApplication -> uniqueApplications.putIfAbsent(courtApplication.getId(), courtApplication));

        final JsonArrayBuilder statusesBuilder = createArrayBuilder();
        uniqueApplications.values().stream()
                .filter(courtApplication -> nonNull(courtApplication.getType()) && nonNull(courtApplication.getType().getId()))
                .forEach(courtApplication -> {
                    final ApplicationCategory category = applicationTypeCategories.get(courtApplication.getType().getId());
                    if (nonNull(category)) {
                        statusesBuilder.add(buildLinkedApplicationStatus(caseId, courtApplication, category));
                    }
                });
        return statusesBuilder.build();
    }

    private static JsonObject buildLinkedApplicationStatus(final UUID caseId, final CourtApplication courtApplication, final ApplicationCategory category) {
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
