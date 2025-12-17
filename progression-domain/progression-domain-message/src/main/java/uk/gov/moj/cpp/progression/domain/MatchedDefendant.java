package uk.gov.moj.cpp.progression.domain;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

public class MatchedDefendant implements Serializable {

  private static final long serialVersionUID = 4150539872337376511L;

  private final UUID prosecutionCaseId;

  private final UUID defendantId;

  private final UUID masterDefendantId;

  private final ZonedDateTime courtProceedingsInitiated;

  public MatchedDefendant(UUID prosecutionCaseId, UUID defendantId, UUID masterDefendantId, ZonedDateTime courtProceedingsInitiated) {
    this.prosecutionCaseId = prosecutionCaseId;
    this.defendantId = defendantId;
    this.masterDefendantId = masterDefendantId;
    this.courtProceedingsInitiated = courtProceedingsInitiated;
  }

  public UUID getProsecutionCaseId() {
    return prosecutionCaseId;
  }

  public UUID getDefendantId() {
    return defendantId;
  }

  public UUID getMasterDefendantId() {
    return masterDefendantId;
  }

  public ZonedDateTime getCourtProceedingsInitiated() {
    return courtProceedingsInitiated;
  }

  public static Builder matchedDefendant() {
    return new Builder();
  }

  public static class Builder {

    private UUID prosecutionCaseId;

    private UUID defendantId;

    private UUID masterDefendantId;

    private ZonedDateTime courtProceedingsInitiated;

    public Builder withProsecutionCaseId(final UUID prosecutionCaseId) {
      this.prosecutionCaseId = prosecutionCaseId;
      return this;
    }

    public Builder withDefendantId(final UUID defendantId) {
      this.defendantId = defendantId;
      return this;
    }

    public Builder withMasterDefendantId(final UUID masterDefendantId) {
      this.masterDefendantId = masterDefendantId;
      return this;
    }

    public Builder withCourtProceedingsInitiated(final ZonedDateTime courtProceedingsInitiated) {
      this.courtProceedingsInitiated = courtProceedingsInitiated;
      return this;
    }

    public MatchedDefendant build() {
      return new MatchedDefendant(prosecutionCaseId, defendantId, masterDefendantId, courtProceedingsInitiated);
    }

  }
}
