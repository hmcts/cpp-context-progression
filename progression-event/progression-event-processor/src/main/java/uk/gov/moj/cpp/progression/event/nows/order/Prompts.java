package uk.gov.moj.cpp.progression.event.nows.order;

import java.io.Serializable;

public class Prompts implements Serializable {
  private static final long serialVersionUID = 1870765747443534132L;

  private final String label;

  private final String value;

  public Prompts(final String label, final String value) {
    this.label = label;
    this.value = value;
  }

  public String getLabel() {
    return label;
  }

  public String getValue() {
    return value;
  }

  public static Builder prompts() {
    return new Prompts.Builder();
  }

  public static class Builder {
    private String label;

    private String value;

    public Builder withLabel(final String label) {
      this.label = label;
      return this;
    }

    public Builder withValue(final String value) {
      this.value = value;
      return this;
    }

    public Prompts build() {
      return new Prompts(label, value);
    }
  }
}
