package uk.gov.moj.cpp.progression.service.payloads;

import java.util.List;
import java.util.UUID;

public class CaseDefendantsWithOrganisation {
  private final UUID caseId;

  private final List<Defendants> defendants;

  private final String urn;

  public CaseDefendantsWithOrganisation(final UUID caseId, final List<Defendants> defendants, final String urn) {
    this.caseId = caseId;
    this.defendants = defendants;
    this.urn = urn;
  }

  public UUID getCaseId() {
    return caseId;
  }

  public List<Defendants> getDefendants() {
    return defendants;
  }

  public String getUrn() {
    return urn;
  }

  public static Builder caseDefendantsWithOrganisation() {
    return new uk.gov.moj.cpp.progression.service.payloads.CaseDefendantsWithOrganisation.Builder();
  }

  public static class Builder {
    private UUID caseId;

    private List<Defendants> defendants;

    private String urn;

    public Builder withCaseId(final UUID caseId) {
      this.caseId = caseId;
      return this;
    }

    public Builder withDefendants(final List<Defendants> defendants) {
      this.defendants = defendants;
      return this;
    }

    public Builder withUrn(final String urn) {
      this.urn = urn;
      return this;
    }

    public CaseDefendantsWithOrganisation build() {
      return new uk.gov.moj.cpp.progression.service.payloads.CaseDefendantsWithOrganisation(caseId, defendants, urn);
    }
  }
}
