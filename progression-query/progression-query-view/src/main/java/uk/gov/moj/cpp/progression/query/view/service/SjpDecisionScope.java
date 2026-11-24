package uk.gov.moj.cpp.progression.query.view.service;

import static java.util.Objects.nonNull;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.InitiationCode;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Decides whether Case at a Glance should show the SJP decision for a case (CHD-3300 / CHD-3311).
 *
 * <p>SJP and Results are called only when the case came from SJP ({@code initiationCode = J}) and at least one
 * non-ejected linked application has an in-scope type. The in-scope types are the reference data statdec, appeal and
 * reopen application type groups, the same ones Case at a Glance shows application statuses for.
 */
public final class SjpDecisionScope {

    private SjpDecisionScope() {
    }

    public static boolean isInScope(final InitiationCode initiationCode, final List<CourtApplication> linkedApplications,
                                    final Set<UUID> inScopeApplicationTypeIds) {
        return InitiationCode.J == initiationCode
                && nonNull(linkedApplications)
                && nonNull(inScopeApplicationTypeIds)
                && linkedApplications.stream().anyMatch(application -> isInScopeLiveApplication(application, inScopeApplicationTypeIds));
    }

    private static boolean isInScopeLiveApplication(final CourtApplication courtApplication, final Set<UUID> inScopeApplicationTypeIds) {
        return nonNull(courtApplication)
                && ApplicationStatus.EJECTED != courtApplication.getApplicationStatus()
                && nonNull(courtApplication.getType())
                && inScopeApplicationTypeIds.contains(courtApplication.getType().getId());
    }
}
