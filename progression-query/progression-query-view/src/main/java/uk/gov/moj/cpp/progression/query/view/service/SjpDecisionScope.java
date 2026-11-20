package uk.gov.moj.cpp.progression.query.view.service;

import static java.util.Objects.nonNull;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.InitiationCode;

import java.util.List;
import java.util.Set;

/**
 * Decides whether Case at a Glance should show the SJP decision for a case (CHD-3300 / CHD-3311).
 *
 * <p>SJP and Results are called only when the case came from SJP ({@code initiationCode = J}) and at least one
 * non-ejected linked application has an in-scope type code. The list lives in the query view because
 * {@code progression-query-view} does not depend on {@code progression-domain-aggregate}, where
 * {@code ApplicationTypeConstants} is.
 */
public final class SjpDecisionScope {

    /**
     * MC80528 statutory declaration (SJP case, s16E MCA 1980), MC80524 reopen, MC80801/MC80802/MC80803 appeals against
     * conviction and sentence, conviction, sentence. MC80527 (statutory declaration, s14, non-SJP) is not in scope.
     */
    public static final Set<String> IN_SCOPE_APPLICATION_TYPE_CODES =
            Set.of("MC80528", "MC80524", "MC80801", "MC80802", "MC80803");

    private SjpDecisionScope() {
    }

    public static boolean isInScope(final InitiationCode initiationCode, final List<CourtApplication> linkedApplications) {
        return InitiationCode.J == initiationCode
                && nonNull(linkedApplications)
                && linkedApplications.stream().anyMatch(SjpDecisionScope::isInScopeLiveApplication);
    }

    private static boolean isInScopeLiveApplication(final CourtApplication courtApplication) {
        return nonNull(courtApplication)
                && ApplicationStatus.EJECTED != courtApplication.getApplicationStatus()
                && nonNull(courtApplication.getType())
                && IN_SCOPE_APPLICATION_TYPE_CODES.contains(courtApplication.getType().getCode());
    }
}
