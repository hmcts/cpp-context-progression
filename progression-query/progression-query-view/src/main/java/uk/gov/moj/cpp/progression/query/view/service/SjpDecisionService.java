package uk.gov.moj.cpp.progression.query.view.service;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gets the SJP decision to show on Case at a Glance for a case created from SJP (CHD-3300 / CHD-3311): the case's most
 * recent SJP decision that is not a set-aside (an application decision), then that decision's published results.
 *
 * <p>Never fails the CAAG page: if SJP or Results is unavailable, it logs and returns no decision.
 */
public class SjpDecisionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SjpDecisionService.class);

    @Inject
    private SjpQueryService sjpQueryService;

    @Inject
    private SjpDecisionResultsSource sjpDecisionResultsSource;

    /**
     * @param inScopeApplicationTypeIds the application types that bring the decision onto CAAG (see {@link SjpDecisionScope})
     * @return the most recent SJP decision's published results, or empty if the case is out of scope, SJP has no decision
     * to show, Results has no stored results for it, or SJP or Results fails.
     */
    public Optional<SjpDecisionHearing> getDecisionHearing(final JsonEnvelope envelope, final UUID caseId,
                                                           final InitiationCode initiationCode,
                                                           final List<CourtApplication> linkedApplications,
                                                           final Set<UUID> inScopeApplicationTypeIds) {
        if (!SjpDecisionScope.isInScope(initiationCode, linkedApplications, inScopeApplicationTypeIds)) {
            return Optional.empty();
        }
        try {
            final Optional<UUID> decisionId = sjpQueryService.getLatestCaseDecision(envelope, caseId)
                    .map(SjpCaseDecision::id);
            if (decisionId.isEmpty()) {
                LOGGER.info("No SJP decision to show on CAAG for case {}", caseId);
                return Optional.empty();
            }
            return sjpDecisionResultsSource.getDecisionHearing(envelope, decisionId.get());
        } catch (final RuntimeException e) {
            LOGGER.warn("Unable to get the SJP decision for case {}; CAAG is returned without it", caseId, e);
            return Optional.empty();
        }
    }
}
