package uk.gov.moj.cpp.progression.listing.domain;

import java.util.Optional;
import java.util.UUID;

@SuppressWarnings({"squid:S1067", "PMD:BeanMembersShouldSerialize"})
public class CourtApplicationParty {

  private final UUID id ;

  private final String firstName;

  private final Boolean isRespondent;

  private final String lastName;

  private final CourtApplicationPartyType courtApplicationPartyType;

  private final Address address;

  private final UUID masterDefendantId;

  private final String dateOfBirth;

  public CourtApplicationParty(final UUID id, final String firstName, final Boolean isRespondent,
                               final String lastName, final CourtApplicationPartyType courtApplicationPartyType,
                               final Address address, final UUID masterDefendantId, final String dateOfBirth) {
    this.id = id;
    this.firstName = firstName;
    this.isRespondent = isRespondent;
    this.lastName = lastName;
    this.courtApplicationPartyType = courtApplicationPartyType;
    this.address = address;
    this.masterDefendantId = masterDefendantId;
    this.dateOfBirth = dateOfBirth;
  }

  public Optional<String> getFirstName() {
    return Optional.ofNullable(firstName);
  }

  public Boolean getIsRespondent() {
    return isRespondent;
  }

  public String getLastName() {
    return lastName;
  }

  public UUID getId() {
    return id;
  }

  public Address getAddress() {
    return address;
  }

  public Optional<UUID> getMasterDefendantId() {
    return Optional.ofNullable(masterDefendantId);
  }

  public Optional<String> getDateOfBirth() {
    return Optional.ofNullable(dateOfBirth);
  }

  public static Builder courtApplicationParty() {
    return new CourtApplicationParty.Builder();
  }

  public CourtApplicationPartyType getCourtApplicationPartyType() {
    return courtApplicationPartyType;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final CourtApplicationParty that = (CourtApplicationParty) obj;

    return java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.firstName, that.firstName) &&
    java.util.Objects.equals(this.isRespondent, that.isRespondent) &&
    java.util.Objects.equals(this.lastName, that.lastName) &&
    java.util.Objects.equals(this.courtApplicationPartyType, that.courtApplicationPartyType) &&
    java.util.Objects.equals(this.masterDefendantId, that.masterDefendantId) &&
    java.util.Objects.equals(this.dateOfBirth, that.dateOfBirth) ;
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(id, firstName, isRespondent, lastName, courtApplicationPartyType, masterDefendantId, dateOfBirth);
  }

  @Override
  public String toString() {
    return "CourtApplicationParty{" +
        "id='" + id + "'," +
    	"firstName='" + firstName + "'," +
    	"isRespondent='" + isRespondent + "'," +
    	"lastName='" + lastName + "'," +
        "courtApplicationPartyType='" + courtApplicationPartyType + "'," +
        "address=" + address + "," +
        "masterDefendantId='" + masterDefendantId + "'," +
        "dateOfBirth='" + dateOfBirth + "'" +
    "}";
  }

  public static class Builder {

    private UUID id ;

    private String firstName;

    private Boolean isRespondent;

    private String lastName;

    private CourtApplicationPartyType courtApplicationPartyType;

    private Address address;

    private UUID masterDefendantId;

    private String dateOfBirth;

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withFirstName(final String firstName) {
      this.firstName = firstName;
      return this;
    }

    public Builder withIsRespondent(final Boolean isRespondent) {
      this.isRespondent = isRespondent;
      return this;
    }

    public Builder withLastName(final String lastName) {
      this.lastName = lastName;
      return this;
    }

    public Builder withCourtApplicationPartyType(final CourtApplicationPartyType courtApplicationPartyType) {
      this.courtApplicationPartyType = courtApplicationPartyType;
      return this;
    }

    public Builder withAddress(final Address address) {
      this.address = address;
      return this;
    }

    public Builder withMasterDefendantId(final UUID masterDefendantId) {
      this.masterDefendantId = masterDefendantId;
      return this;
    }

    public Builder withDateOfBirth(final String dateOfBirth) {
      this.dateOfBirth = dateOfBirth;
      return this;
    }

    public CourtApplicationParty build() {
      return new CourtApplicationParty(id, firstName, isRespondent, lastName, courtApplicationPartyType, address, masterDefendantId, dateOfBirth);
    }
  }
}
