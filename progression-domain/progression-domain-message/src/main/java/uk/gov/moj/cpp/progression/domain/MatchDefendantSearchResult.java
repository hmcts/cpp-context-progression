package uk.gov.moj.cpp.progression.domain;

import uk.gov.justice.core.courts.Cases;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MatchDefendantSearchResult implements Serializable {

    private static final long serialVersionUID = 1155333358999282567L;

    private Map<UUID, List<Cases>> partiallyMatchedDefendants;

    private Map<UUID, List<Cases>> fullyMatchedDefendants;

    private MatchDefendantSearchResult(Map<UUID, List<Cases>> partiallyMatchedDefendants, Map<UUID, List<Cases>> fullyMatchedDefendants) {
        this.partiallyMatchedDefendants = partiallyMatchedDefendants;
        this.fullyMatchedDefendants = fullyMatchedDefendants;
    }

    public Map<UUID, List<Cases>> getPartiallyMatchedDefendants() {
        return partiallyMatchedDefendants;
    }

    public Map<UUID, List<Cases>> getFullyMatchedDefendants() {
        return fullyMatchedDefendants;
    }

    public static MatchDefendantSearchResult.Builder matchDefendantSearchResult() {
        return new MatchDefendantSearchResult.Builder();
    }

    public static class Builder {

        private Map<UUID, List<Cases>> partiallyMatchedDefendants;

        private Map<UUID, List<Cases>> fullyMatchedDefendants;

        public MatchDefendantSearchResult.Builder withPartiallyMatchedDefendants(final Map<UUID, List<Cases>> partiallyMatchedDefendants) {
            this.partiallyMatchedDefendants = partiallyMatchedDefendants;
            return this;
        }

        public MatchDefendantSearchResult.Builder withFullyMatchedDefendants(final Map<UUID, List<Cases>> fullyMatchedDefendants) {
            this.fullyMatchedDefendants = fullyMatchedDefendants;
            return this;
        }

        public MatchDefendantSearchResult build() {
            return new MatchDefendantSearchResult(partiallyMatchedDefendants, fullyMatchedDefendants);
        }
    }
}
