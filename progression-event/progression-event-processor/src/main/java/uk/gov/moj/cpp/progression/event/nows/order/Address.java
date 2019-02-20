package uk.gov.moj.cpp.progression.event.nows.order;

import java.io.Serializable;

public class Address implements Serializable {
  private static final long serialVersionUID = 1870765747443534132L;

  private final String line1;

  private final String line2;

  private final String line3;

  private final String line4;

  private final String line5;

  private final String postCode;

  public Address(final String line1, final String line2, final String line3, final String line4, final String line5, final String postCode) {
    this.line1 = line1;
    this.line2 = line2;
    this.line3 = line3;
    this.line4 = line4;
    this.line5 = line5;
    this.postCode = postCode;
  }

  public String getLine1() {
    return line1;
  }

  public String getLine2() {
    return line2;
  }

  public String getLine3() {
    return line3;
  }

  public String getLine4() {
    return line4;
  }

  public String getLine5() {
    return line5;
  }

  public String getPostCode() {
    return postCode;
  }

  public static Builder address() {
    return new Address.Builder();
  }

  public static class Builder {
    private String line1;

    private String line2;

    private String line3;

    private String line4;

    private String line5;

    private String postCode;

    public Builder withLine1(final String line1) {
      this.line1 = line1;
      return this;
    }

    public Builder withLine2(final String line2) {
      this.line2 = line2;
      return this;
    }

    public Builder withLine3(final String line3) {
      this.line3 = line3;
      return this;
    }

    public Builder withLine4(final String line4) {
      this.line4 = line4;
      return this;
    }

    public Builder withLine5(final String line5) {
      this.line5 = line5;
      return this;
    }

    public Builder withPostCode(final String postCode) {
      this.postCode = postCode;
      return this;
    }

    public Address build() {
      return new Address(line1, line2, line3, line4, line5, postCode);
    }
  }
}
