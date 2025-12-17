package uk.gov.moj.cpp.progression.event.nows.order;

import java.io.Serializable;
import java.util.Set;

public class Cases implements Serializable {
    private static final long serialVersionUID = 1870765747443534132L;

    private final Set<DefendantCaseOffences> defendantCaseOffences;

    private final Set<DefendantCaseResults> defendantCaseResults;

    private final String urn;

    public Cases(final Set<DefendantCaseOffences> defendantCaseOffences, final Set<DefendantCaseResults> defendantCaseResults, final String urn) {
        this.defendantCaseOffences = defendantCaseOffences;
        this.defendantCaseResults = defendantCaseResults;
        this.urn = urn;
    }

    public Set<DefendantCaseOffences> getDefendantCaseOffences() {
        return defendantCaseOffences;
    }

    public Set<DefendantCaseResults> getDefendantCaseResults() {
        return defendantCaseResults;
    }

    public String getUrn() {
        return urn;
    }

    public static Builder cases() {
        return new Cases.Builder();
    }

    public static class Builder {
        private Set<DefendantCaseOffences> defendantCaseOffences;

        private Set<DefendantCaseResults> defendantCaseResults;

        private String urn;

        public Builder withDefendantCaseOffences(final Set<DefendantCaseOffences> defendantCaseOffences) {
            this.defendantCaseOffences = defendantCaseOffences;
            return this;
        }

        public Builder withDefendantCaseResults(final Set<DefendantCaseResults> defendantCaseResults) {
            this.defendantCaseResults = defendantCaseResults;
            return this;
        }

        public Builder withUrn(final String urn) {
            this.urn = urn;
            return this;
        }

        public Cases build() {
            return new Cases(defendantCaseOffences, defendantCaseResults, urn);
        }
    }
}
