package uk.gov.moj.cpp.progression.query.view.service;

import static java.util.Collections.emptyList;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gets the SJP decisions to show on Case at a Glance for a case created from SJP (CHD-3300 / CHD-3311): the decision IDs
 * from SJP, cut at the latest set-aside, then each decision's published results.
 *
 * <p>Never fails the CAAG page: if SJP or Results is unavailable, it logs and returns no decisions.
 */
public class SjpDecisionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SjpDecisionService.class);

    @Inject
    private SjpQueryService sjpQueryService;

    @Inject
    private SjpDecisionResultsSource sjpDecisionResultsSource;

    public List<SjpDecisionHearing> getDecisionHearings(final JsonEnvelope envelope, final UUID caseId,
                                                        final InitiationCode initiationCode,
                                                        final List<CourtApplication> linkedApplications) {
        if (!SjpDecisionScope.isInScope(initiationCode, linkedApplications)) {
            return emptyList();
        }
        try {
            final List<UUID> decisionIds = SjpDecisionSelector.selectDecisionIds(sjpQueryService.getCaseDecisions(envelope, caseId));
            if (decisionIds.isEmpty()) {
                LOGGER.info("No SJP decisions to show on CAAG for case {}", caseId);
                return emptyList();
            }
            return sjpDecisionResultsSource.getDecisionHearings(envelope, decisionIds);
        } catch (final RuntimeException e) {
            LOGGER.warn("Unable to get SJP decisions for case {}; CAAG is returned without them", caseId, e);
            return emptyList();
        }
    }
}
