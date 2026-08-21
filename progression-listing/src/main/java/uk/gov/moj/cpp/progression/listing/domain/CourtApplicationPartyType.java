package uk.gov.moj.cpp.progression.listing.domain;

import java.util.Optional;

public enum CourtApplicationPartyType {
  PROSECUTING_AUTHORITY("PROSECUTING_AUTHORITY"),

  PERSON("PERSON"),

  ORGANISATION("ORGANISATION"),

  PERSON_DEFENDANT("PERSON_DEFENDANT");

  private final String value;

  CourtApplicationPartyType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Optional<CourtApplicationPartyType> valueFor(final String value) {
    if(PROSECUTING_AUTHORITY.value.equals(value)) {
      return Optional.of(PROSECUTING_AUTHORITY);
    }
    if(PERSON.value.equals(value)) {
      return Optional.of(PERSON);
    }
    if(ORGANISATION.value.equals(value)) {
      return Optional.of(ORGANISATION);
    }
    if(PERSON_DEFENDANT.value.equals(value)) {
      return Optional.of(PERSON_DEFENDANT);
    }
    return Optional.empty();
  }
}
