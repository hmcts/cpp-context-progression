package uk.gov.moj.cpp.progression.event.nows.order;

import java.io.Serializable;

public class Defendant implements Serializable {
  private static final long serialVersionUID = 1870765747443534132L;

  private final Address address;

  private final String dateOfBirth;

  private final String name;

  public Defendant(final Address address, final String dateOfBirth, final String name) {
    this.address = address;
    this.dateOfBirth = dateOfBirth;
    this.name = name;
  }

  public Address getAddress() {
    return address;
  }

  public String getDateOfBirth() {
    return dateOfBirth;
  }

  public String getName() {
    return name;
  }

  public static Builder defendant() {
    return new Defendant.Builder();
  }

  public static class Builder {
    private Address address;

    private String dateOfBirth;

    private String name;

    public Builder withAddress(final Address address) {
      this.address = address;
      return this;
    }

    public Builder withDateOfBirth(final String dateOfBirth) {
      this.dateOfBirth = dateOfBirth;
      return this;
    }

    public Builder withName(final String name) {
      this.name = name;
      return this;
    }

    public Defendant build() {
      return new Defendant(address, dateOfBirth, name);
    }
  }
}
