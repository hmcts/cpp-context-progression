package uk.gov.moj.cpp.progression.event.nows.order;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class NowResultDefinitionsText implements Serializable {
  private static final long serialVersionUID = 1870765747443534132L;

  @SuppressWarnings({"squid:S1948"})
  private final Map<String, Object> additionalProperties;

  public NowResultDefinitionsText(final Map<String, Object> additionalProperties) {
    this.additionalProperties = additionalProperties;
  }

  public static Builder nowResultDefinitionsText() {
    return new NowResultDefinitionsText.Builder();
  }

  public Map<String, Object> getAdditionalProperties() {
    return additionalProperties;
  }

  public void setAdditionalProperty(final String name, final Object value) {
    additionalProperties.put(name, value);
  }

  public static class Builder {
    private final Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Builder withAdditionalProperty(final String name, final Object value) {
      additionalProperties.put(name, value);
      return this;
    }

    public NowResultDefinitionsText build() {
      return new NowResultDefinitionsText(additionalProperties);
    }
  }
}
