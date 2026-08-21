package uk.gov.moj.cpp.progression.listing.domain;

import java.util.Optional;

public enum HearingLanguageNeeds {
  ENGLISH("ENGLISH"),

  WELSH("WELSH");

  private final String value;

  HearingLanguageNeeds(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Optional<HearingLanguageNeeds> valueFor(final String value) {
    if(ENGLISH.value.equals(value)) { return Optional.of(ENGLISH); };
    if(WELSH.value.equals(value)) { return Optional.of(WELSH); };
    return Optional.empty();
  }
}
