package uk.gov.moj.cpp.progression.aggregate.helper;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.JudicialResult;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Narrow hook: reactivate a case when a grant result is shared for selected application types.
 * <p>
 * v1: Application to reopen case ({@link ApplicationTypeConstants#APP_TYPE_REOPEN_CASE_ID}) + Granted (G).
 * Widen later by extending {@link #isGrantThatReactivatesCase(CourtApplication)} without changing
 * {@link uk.gov.moj.cpp.progression.aggregate.CaseAggregate} status wiring.
 */
public final class CaseReactivationOnApplicationGrantHelper {

    private CaseReactivationOnApplicationGrantHelper() {
    }

    public static boolean shouldReactivateCase(final UUID prosecutionCaseId, final List<CourtApplication> courtApplications) {
        if (prosecutionCaseId == null || isEmpty(courtApplications)) {
            return false;
        }

        return courtApplications.stream()
                .filter(Objects::nonNull)
                .filter(application -> isLinkedToCase(application, prosecutionCaseId))
                .anyMatch(CaseReactivationOnApplicationGrantHelper::isGrantThatReactivatesCase);
    }

    /**
     * Case ids linked on a grant that should reopen/reactivate. Empty when the application is not
     * a reopen grant, or when neither the granted payload nor the stored application has linked cases.
     * <p>
     * Application-only shares may omit {@code courtApplicationCases} on the result payload; the stored
     * application (from {@code ApplicationAggregate}) still holds the case link from creation.
     */
    public static List<UUID> linkedCaseIdsToReactivate(final CourtApplication grantedApplication) {
        return linkedCaseIdsToReactivate(grantedApplication, null);
    }

    public static List<UUID> linkedCaseIdsToReactivate(final CourtApplication grantedApplication,
                                                       final CourtApplication storedApplication) {
        if (grantedApplication == null || !isGrantThatReactivatesCase(grantedApplication)) {
            return List.of();
        }
        final List<UUID> fromGranted = linkedProsecutionCaseIds(grantedApplication);
        if (!fromGranted.isEmpty()) {
            return fromGranted;
        }
        return linkedProsecutionCaseIds(storedApplication);
    }

    static List<UUID> linkedProsecutionCaseIds(final CourtApplication application) {
        if (application == null) {
            return List.of();
        }
        final Stream<UUID> fromApplicationCases = ofNullable(application.getCourtApplicationCases())
                .orElse(List.of())
                .stream()
                .filter(Objects::nonNull)
                .map(CourtApplicationCase::getProsecutionCaseId);
        final Stream<UUID> fromCourtOrder = ofNullable(application.getCourtOrder())
                .map(CourtOrder::getCourtOrderOffences)
                .orElse(List.of())
                .stream()
                .filter(Objects::nonNull)
                .map(CourtOrderOffence::getProsecutionCaseId);
        return Stream.concat(fromApplicationCases, fromCourtOrder)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * Extension point for future application types that should reopen/reactivate a case on grant.
     */
    private static boolean isGrantThatReactivatesCase(final CourtApplication application) {
        return isReopenCaseApplication(application) && hasGrantedResult(application);
    }

    private static boolean isReopenCaseApplication(final CourtApplication application) {
        return ofNullable(application.getType())
                .map(CourtApplicationType::getCode)
                .map(ApplicationTypeConstants.APP_TYPE_REOPEN_CASE_ID::equals)
                .orElse(false);
    }

    private static boolean hasGrantedResult(final CourtApplication application) {
        return ofNullable(application.getJudicialResults())
                .orElse(List.of())
                .stream()
                .filter(Objects::nonNull)
                .filter(CaseReactivationOnApplicationGrantHelper::isRootJudicialResult)
                .map(JudicialResult::getJudicialResultTypeId)
                .anyMatch(ResultConstants.G::equals);
    }

    private static boolean isRootJudicialResult(final JudicialResult judicialResult) {
        return nonNull(judicialResult.getJudicialResultTypeId())
                && judicialResult.getJudicialResultTypeId().equals(judicialResult.getRootJudicialResultTypeId());
    }

    /**
     * When courtApplicationCases is present, require a match on prosecutionCaseId.
     * When absent (common on some application-only shares), treat the application as in scope
     * for the case being updated by this command.
     */
    private static boolean isLinkedToCase(final CourtApplication application, final UUID prosecutionCaseId) {
        final List<CourtApplicationCase> applicationCases = application.getCourtApplicationCases();
        if (isEmpty(applicationCases)) {
            return true;
        }
        return applicationCases.stream()
                .filter(Objects::nonNull)
                .map(CourtApplicationCase::getProsecutionCaseId)
                .anyMatch(prosecutionCaseId::equals);
    }
}
