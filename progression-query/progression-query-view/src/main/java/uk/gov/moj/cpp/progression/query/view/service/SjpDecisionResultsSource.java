package uk.gov.moj.cpp.progression.query.view.service;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;
import java.util.UUID;

/**
 * Where CAAG gets the published results of SJP decisions. Implemented today by {@link ResultsQueryService} (the payloads
 * cpp-context-results stores). The planned Results Store service (Option D in the CHD-3300 options paper) can replace it
 * here without changing the CAAG mapping.
 */
public interface SjpDecisionResultsSource {

    /**
     * @return the published hearing for each decision ID that has one, in the order of {@code decisionIds}. Decisions with
     * no stored results are left out.
     */
    List<SjpDecisionHearing> getDecisionHearings(JsonEnvelope envelope, List<UUID> decisionIds);
}
