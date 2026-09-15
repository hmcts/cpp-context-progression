package uk.gov.moj.cpp.progression.listing.domain;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings({"squid:S1067", "squid:S00107", "squid:S2384" })
public class CourtApplication {
  private final UUID id;

  private final List<UUID> linkedCaseIds;

  private final UUID parentApplicationId;

  private final CourtApplicationParty applicant;

  private final String applicationType;

  private final List<CourtApplicationParty> respondents;

  private final CourtApplicationParty subject;

  private final Boolean requiresResponse;

  private final Optional<String> applicationReference;

  private final Optional<String> applicationParticulars;

  private final List<Offence> offences;

  public CourtApplication(final UUID id, final List<UUID> linkedCaseIds, final UUID parentApplicationId, final CourtApplicationParty applicant,
                          final String applicationType, final List<CourtApplicationParty> respondents, final Boolean requiresResponse,
                          final Optional<String> applicationReference, final Optional<String> applicationParticulars, final List<Offence> offences,
                          final CourtApplicationParty subject) {
    this.id = id;
    this.linkedCaseIds = linkedCaseIds;
    this.parentApplicationId = parentApplicationId;
    this.applicant = applicant;
    this.applicationType = applicationType;
    this.respondents = respondents;
    this.requiresResponse = requiresResponse;
    this.applicationReference = applicationReference;
    this.applicationParticulars = applicationParticulars;
    this.offences = offences;
    this.subject = subject;
  }

  public CourtApplicationParty getApplicant() {
    return applicant;
  }

  public String getApplicationType() {
    return applicationType;
  }

  public UUID getId() {
    return id;
  }

  public List<CourtApplicationParty> getRespondents() {
    return respondents;
  }

  public List<UUID> getLinkedCaseIds() {
    return linkedCaseIds;
  }

  public Optional<UUID> getParentApplicationId() {
    return ofNullable(parentApplicationId);
  }

  public Boolean getRequiresResponse() {
    return requiresResponse;
  }

  public Optional<String> getApplicationReference() {
    return applicationReference;
  }

  public Optional<String> getApplicationParticulars() {
    return applicationParticulars;
  }

  public static Builder courtApplication() {
    return new CourtApplication.Builder();
  }

  public List<Offence> getOffences() {
    return offences;
  }

  public CourtApplicationParty getSubject() {
    return subject;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final CourtApplication that = (CourtApplication) obj;

    return java.util.Objects.equals(this.applicant, that.applicant) &&
    java.util.Objects.equals(this.applicationType, that.applicationType) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.respondents, that.respondents) &&
    java.util.Objects.equals(this.linkedCaseIds, that.linkedCaseIds) && java.util.Objects.equals(this.offences, that.offences) &&
    java.util.Objects.equals(this.parentApplicationId, that.parentApplicationId)&&
    java.util.Objects.equals(this.requiresResponse, that.requiresResponse) &&
    java.util.Objects.equals(this.applicationReference, that.applicationReference) &&
    java.util.Objects.equals(this.applicationParticulars, that.applicationParticulars) &&
    java.util.Objects.equals(this.subject, that.subject);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(applicant, applicationType, id, respondents, linkedCaseIds, parentApplicationId, requiresResponse, applicationReference, applicationParticulars, offences, subject);
  }

  @Override
  public String toString() {
    return "CourtApplication{" +
    	"applicant='" + applicant + "'," +
    	"applicationType='" + applicationType + "'," +
    	"id='" + id + "'," +
    	"respondents='" + respondents + "'" +
        "linkedCaseIds='" + linkedCaseIds + "'" + "offences='" + offences + "'" +
        "parentApplicationId='" + parentApplicationId + "'" +
        "requiresResponse='" + requiresResponse + "'" +
        "applicationReference='" + applicationReference + "'" +
        "applicationParticulars='" + applicationParticulars + "'" +
        "subject='" + subject + "'" +
    "}";
  }

  public static class Builder {
    private CourtApplicationParty applicant;

    private String applicationType;

    private UUID id;

    private List<CourtApplicationParty> respondents;

    private List<UUID> linkedCaseIds;

    private UUID parentApplicationId;

    private Boolean requiresResponse;

    private Optional<String> applicationReference = empty();

    private Optional<String> applicationParticulars = empty();

    private List<Offence> offences;

    private CourtApplicationParty subject;

    public Builder withApplicant(final CourtApplicationParty applicant) {
      this.applicant = applicant;
      return this;
    }

    public Builder withApplicationType(final String applicationType) {
      this.applicationType = applicationType;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withRespondents(final List<CourtApplicationParty> respondents) {
      this.respondents = respondents;
      return this;
    }

    public Builder withLinkedCaseIds(final List<UUID> linkedCaseIds) {
      this.linkedCaseIds = linkedCaseIds;
      return this;
    }

    public Builder withOffences(final List<Offence> offences) {
      this.offences = offences;
      return this;
    }

    public Builder withParentApplicationId(final UUID parentApplicationId) {
      this.parentApplicationId = parentApplicationId;
      return this;
    }

    public Builder withRequiresResponse(final Boolean requiresResponse) {
      this.requiresResponse = requiresResponse;
      return this;
    }

    public Builder withApplicationReference(final Optional<String> applicationReference) {
      this.applicationReference = applicationReference;
      return this;
    }

    public Builder withApplicationParticulars(final Optional<String> applicationParticulars) {
      this.applicationParticulars = applicationParticulars;
      return this;
    }

    public Builder withSubject(final CourtApplicationParty subject) {
      this.subject = subject;
      return this;
    }

    public CourtApplication build() {
      return new CourtApplication(id, linkedCaseIds, parentApplicationId,  applicant, applicationType,  respondents, requiresResponse, applicationReference, applicationParticulars, offences, subject);
    }
  }
}
