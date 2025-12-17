package uk.gov.moj.cpp.progression.event.nows.order;

import java.io.Serializable;

public class OrderAddressee implements Serializable {
  private static final long serialVersionUID = 1870765747443534132L;

  private final Address address;

  private final String name;

  public OrderAddressee(final Address address, final String name) {
    this.address = address;
    this.name = name;
  }

  public Address getAddress() {
    return address;
  }

  public String getName() {
    return name;
  }

  public static Builder orderAddressee() {
    return new OrderAddressee.Builder();
  }

  public static class Builder {
    private Address address;

    private String name;

    public Builder withAddress(final Address address) {
      this.address = address;
      return this;
    }

    public Builder withName(final String name) {
      this.name = name;
      return this;
    }

    public OrderAddressee build() {
      return new OrderAddressee(address, name);
    }
  }
}
