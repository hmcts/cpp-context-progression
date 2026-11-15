package uk.gov.moj.cpp.progression.query.view.service;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

/**
 * Where CAAG gets the published results of SJP decisions. Implemented today by {@link ResultsQueryService} (the payloads
 * cpp-context-results stores). The planned Results Store service (Option D in the CHD-3300 options paper) can replace it
 * here without changing the CAAG mapping.
 */
public interface SjpDecisionResultsSource {

    /**
     * @return the stored payload ({@code hearing}, {@code sharedTime}) for each decision ID that has one, in the order of
     * {@code decisionIds}. Decisions with no stored results are left out.
     */
    List<JsonObject> getDecisionPayloads(JsonEnvelope envelope, List<UUID> decisionIds);
}
