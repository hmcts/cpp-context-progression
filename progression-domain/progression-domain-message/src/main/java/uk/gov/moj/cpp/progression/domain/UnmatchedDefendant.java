package uk.gov.moj.cpp.progression.domain;

import java.io.Serializable;
import java.util.UUID;

public class UnmatchedDefendant implements Serializable {

    private static final long serialVersionUID = 4150539872337376511L;

    private final UUID prosecutionCaseId;

    private final UUID defendantId;

    public UnmatchedDefendant(final UUID prosecutionCaseId, final UUID defendantId) {
        this.prosecutionCaseId = prosecutionCaseId;
        this.defendantId = defendantId;
    }

    public UUID getProsecutionCaseId() {
        return prosecutionCaseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public static Builder unmatchedDefendant() {
        return new Builder();
    }

    public static class Builder {

        private UUID prosecutionCaseId;

        private UUID defendantId;

        public Builder withProsecutionCaseId(final UUID prosecutionCaseId) {
            this.prosecutionCaseId = prosecutionCaseId;
            return this;
        }

        public Builder withDefendantId(final UUID defendantId) {
            this.defendantId = defendantId;
            return this;
        }

        public UnmatchedDefendant build() {
            return new UnmatchedDefendant(prosecutionCaseId, defendantId);
        }

    }
}
