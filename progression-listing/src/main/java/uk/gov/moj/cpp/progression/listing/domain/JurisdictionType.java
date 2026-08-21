package uk.gov.moj.cpp.progression.listing.domain;

import java.util.Optional;

public enum JurisdictionType {
  CROWN("CROWN"),

  MAGISTRATES("MAGISTRATES");

  private final String value;

  JurisdictionType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Optional<JurisdictionType> valueFor(final String value) {
    if(CROWN.value.equals(value)) { return Optional.of(CROWN); }
    if(MAGISTRATES.value.equals(value)) { return Optional.of(MAGISTRATES); }
    return Optional.empty();
  }
}
