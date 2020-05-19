package uk.gov.moj.cpp.progression.domain;

import java.io.Serializable;
import java.util.List;

public class UnmatchDefendant implements Serializable {

    private static final long serialVersionUID = -3670485138081676790L;

    private final List<UnmatchedDefendant> unmatchedDefendants;

    public UnmatchDefendant(final List<UnmatchedDefendant> unmatchedDefendants) {
        this.unmatchedDefendants = unmatchedDefendants;
    }

    public List<UnmatchedDefendant> getUnmatchedDefendants() {
        return unmatchedDefendants;
    }

    public static Builder unmatchDefendant() {
        return new Builder();
    }

    public static class Builder {
        private List<UnmatchedDefendant> unmatchedDefendants;

        public Builder withUnmatchedDefendants(final List<UnmatchedDefendant> unmatchedDefendants) {
            this.unmatchedDefendants = unmatchedDefendants;
            return this;
        }

        public UnmatchDefendant build() {
            return new UnmatchDefendant(unmatchedDefendants);
        }
    }
}
