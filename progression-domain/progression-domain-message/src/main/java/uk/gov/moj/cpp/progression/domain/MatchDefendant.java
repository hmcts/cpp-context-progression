package uk.gov.moj.cpp.progression.domain;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public class MatchDefendant implements Serializable {

  private static final long serialVersionUID = -3670485138081676790L;

  private final UUID prosecutionCaseId;

  private final UUID defendantId;

  private final List<MatchedDefendant> matchedDefendants;

  public MatchDefendant(final UUID prosecutionCaseId, final UUID defendantId, final List<MatchedDefendant> matchedDefendants) {
    this.prosecutionCaseId = prosecutionCaseId;
    this.defendantId = defendantId;
    this.matchedDefendants = matchedDefendants;
  }

  public UUID getProsecutionCaseId() {
    return prosecutionCaseId;
  }

  public UUID getDefendantId() {
    return defendantId;
  }

  public List<MatchedDefendant> getMatchedDefendants() {
    return matchedDefendants;
  }

  public static Builder matchDefendant() {
    return new Builder();
  }

  public static class Builder {
    private UUID prosecutionCaseId;

    private UUID defendantId;

    private List<MatchedDefendant> matchedDefendants;

    public Builder withDefendantId(final UUID defendantId) {
      this.defendantId = defendantId;
      return this;
    }

    public Builder withMatchedDefendants(final List<MatchedDefendant> matchedDefendants) {
      this.matchedDefendants = matchedDefendants;
      return this;
    }

    public Builder withProsecutionCaseId(final UUID prosecutionCaseId) {
      this.prosecutionCaseId = prosecutionCaseId;
      return this;
    }

    public MatchDefendant build() {
      return new MatchDefendant(prosecutionCaseId, defendantId, matchedDefendants);
    }
  }
}
