package uk.gov.moj.cpp.progression.event.nows.order;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DefendantCaseResults implements Serializable {
    private static final long serialVersionUID = 1870765747443534132L;

    private final String label;

    private final List<Prompts> prompts;

    @SuppressWarnings({"squid:S1948"})
    private final Map<String, Object> additionalProperties;

    public DefendantCaseResults(final String label, final List<Prompts> prompts, final Map<String, Object> additionalProperties) {
        this.label = label;
        this.prompts = prompts;
        this.additionalProperties = additionalProperties;
    }

    public String getLabel() {
        return label;
    }

    public List<Prompts> getPrompts() {
        return prompts;
    }

    public static Builder defendantCaseResults() {
        return new DefendantCaseResults.Builder();
    }

    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    public void setAdditionalProperty(final String name, final Object value) {
        additionalProperties.put(name, value);
    }

    public static class Builder {
        private String label;

        private List<Prompts> prompts;

        private final Map<String, Object> additionalProperties = new HashMap<String, Object>();

        public Builder withLabel(final String label) {
            this.label = label;
            return this;
        }

        public Builder withPrompts(final List<Prompts> prompts) {
            this.prompts = prompts;
            return this;
        }

        public Builder withAdditionalProperty(final String name, final Object value) {
            additionalProperties.put(name, value);
            return this;
        }

        public DefendantCaseResults build() {
            return new DefendantCaseResults(label, prompts, additionalProperties);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DefendantCaseResults that = (DefendantCaseResults) o;
        return Objects.equals(label, that.label) &&
                Objects.equals(prompts, that.prompts) &&
                Objects.equals(additionalProperties, that.additionalProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, prompts, additionalProperties);
    }
}
