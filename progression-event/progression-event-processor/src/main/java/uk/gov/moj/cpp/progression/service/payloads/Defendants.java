package uk.gov.moj.cpp.progression.service.payloads;

import java.util.UUID;

public class Defendants {
  private final UUID associatedOrganisation;

  private final UUID defendantId;

  public Defendants(final UUID associatedOrganisation, final UUID defendantId) {
    this.associatedOrganisation = associatedOrganisation;
    this.defendantId = defendantId;
  }

  public UUID getAssociatedOrganisation() {
    return associatedOrganisation;
  }

  public UUID getDefendantId() {
    return defendantId;
  }

  public static Builder defendants() {
    return new Defendants.Builder();
  }

  public static class Builder {
    private UUID associatedOrganisation;

    private UUID defendantId;

    public Builder withAssociatedOrganisation(final UUID associatedOrganisation) {
      this.associatedOrganisation = associatedOrganisation;
      return this;
    }

    public Builder withDefendantId(final UUID defendantId) {
      this.defendantId = defendantId;
      return this;
    }

    public Defendants build() {
      return new  Defendants(associatedOrganisation, defendantId);
    }
  }
}
