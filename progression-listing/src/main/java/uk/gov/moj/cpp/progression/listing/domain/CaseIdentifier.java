package uk.gov.moj.cpp.progression.listing.domain;

import java.io.Serializable;
import java.util.UUID;

@SuppressWarnings({"squid:S00107", "squid:S00121"})
public class CaseIdentifier implements Serializable {
  private final String authorityCode;

  private final UUID authorityId;

  private final String caseReference;

  public CaseIdentifier(final String authorityCode, final UUID authorityId, final String caseReference) {
    this.authorityCode = authorityCode;
    this.authorityId = authorityId;
    this.caseReference = caseReference;
  }

  public String getAuthorityCode() {
    return authorityCode;
  }

  public UUID getAuthorityId() {
    return authorityId;
  }

  public String getCaseReference() {
    return caseReference;
  }

  public static Builder caseIdentifier() {
    return new CaseIdentifier.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final CaseIdentifier that = (CaseIdentifier) obj;

    return java.util.Objects.equals(this.authorityCode, that.authorityCode) &&
    java.util.Objects.equals(this.authorityId, that.authorityId) &&
    java.util.Objects.equals(this.caseReference, that.caseReference);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(authorityCode, authorityId, caseReference);}

  @Override
  public String toString() {
    return "CaseIdentifier{" +
    	"authorityCode='" + authorityCode + "'," +
    	"authorityId='" + authorityId + "'," +
    	"caseReference='" + caseReference + "'" +
    "}";
  }

  public static class Builder {
    private String authorityCode;

    private UUID authorityId;

    private String caseReference;

    public Builder withAuthorityCode(final String authorityCode) {
      this.authorityCode = authorityCode;
      return this;
    }

    public Builder withAuthorityId(final UUID authorityId) {
      this.authorityId = authorityId;
      return this;
    }

    public Builder withCaseReference(final String caseReference) {
      this.caseReference = caseReference;
      return this;
    }

    public CaseIdentifier build() {
      return new CaseIdentifier(authorityCode, authorityId, caseReference);
    }
  }
}
