package uk.gov.moj.cpp.progression.service.payloads;

import com.fasterxml.jackson.annotation.JsonCreator;

public class CaseDefendantsOrganisations {
  private final CaseDefendantsWithOrganisation caseDefendantOrganisation;

  @JsonCreator
  public CaseDefendantsOrganisations(final CaseDefendantsWithOrganisation caseDefendantOrganisation) {
    this.caseDefendantOrganisation = caseDefendantOrganisation;
  }

  public CaseDefendantsWithOrganisation getCaseDefendantOrganisation() {
    return caseDefendantOrganisation;
  }

  public static Builder caseDefendantsOrganisations() {
    return new CaseDefendantsOrganisations.Builder();
  }

  public static class Builder {
    private CaseDefendantsWithOrganisation caseDefendantOrganisation;

    public Builder withCaseDefendantOrganisation(final CaseDefendantsWithOrganisation caseDefendantOrganisation) {
      this.caseDefendantOrganisation = caseDefendantOrganisation;
      return this;
    }

    public CaseDefendantsOrganisations build() {
      return new CaseDefendantsOrganisations(caseDefendantOrganisation);
    }
  }
}
