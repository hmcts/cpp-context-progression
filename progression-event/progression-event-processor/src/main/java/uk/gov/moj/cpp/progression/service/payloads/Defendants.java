package uk.gov.moj.cpp.progression.service.payloads;

import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Defendants {
  private final UUID associatedOrganisation;

  private final UUID defendantId;

  private final String defendantFirstName;
  
  private final String defendantLastName;



  private final String organisationName;

  public Defendants(final UUID associatedOrganisation, final UUID defendantId, final String defendantFirstName, final String defendantLastName, final String organisationName) {
    this.associatedOrganisation = associatedOrganisation;
    this.defendantId = defendantId;
    this.defendantFirstName = defendantFirstName;
    this.defendantLastName = defendantLastName;
    this.organisationName = organisationName;
  }

  public UUID getAssociatedOrganisation() {
    return associatedOrganisation;
  }

  public UUID getDefendantId() {
    return defendantId;
  }

  public String getOrganisationName() {
    return organisationName;
  }

  public String getDefendantFirstName() { return defendantFirstName; }

  public String getDefendantLastName() { return defendantLastName;  }

  public String getDefendantFullName() { return Stream.of(defendantFirstName,defendantLastName).filter(Objects::nonNull).collect(Collectors.joining(" "));}

  public static Builder defendants() {
    return new Defendants.Builder();
  }

  public static class Builder {
    private UUID associatedOrganisation;

    private UUID defendantId;

    private String defendantFirstName;
    private String defendantLastName ;
    private String organisationName;

    public Builder withAssociatedOrganisation(final UUID associatedOrganisation) {
      this.associatedOrganisation = associatedOrganisation;
      return this;
    }

    public Builder withDefendantId(final UUID defendantId) {
      this.defendantId = defendantId;
      return this;
    }

    public Builder  withdefendantFirstName(final String defendantFirstName) {
      this.defendantFirstName = defendantFirstName;
      return this;
    }

    public Builder  withdefendantLastName(final String defendantLastName) {
      this.defendantLastName = defendantLastName;
      return this;
    }

    public Builder withOrganisationName(final String organisationName) {
      this.organisationName = organisationName;
      return this;
    }


    public Defendants build() {
      return new  Defendants(associatedOrganisation, defendantId, defendantFirstName, defendantLastName, organisationName);
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) { return true;}
    if (!(o instanceof Defendants)) {return false;}
    final Defendants that = (Defendants) o;
    return getDefendantId().equals(that.getDefendantId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getDefendantId());
  }
}
