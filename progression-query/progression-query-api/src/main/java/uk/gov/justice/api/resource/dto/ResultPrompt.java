package uk.gov.justice.api.resource.dto;

import java.util.Objects;
import java.util.UUID;

import javax.json.JsonValue;

import com.fasterxml.jackson.annotation.JsonCreator;

public class ResultPrompt {

    private UUID promptId;
    private String promptRef;
    private String type;
    private String label;
    private JsonValue value;

    @JsonCreator
    public ResultPrompt(final UUID promptId, final String promptRef, final String type, final String label, final JsonValue value) {
        this.promptId = promptId;
        this.promptRef = promptRef;
        this.type = type;
        this.label = label;
        this.value = value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ResultPrompt that = (ResultPrompt) o;
        return Objects.equals(promptId, that.promptId)
                && Objects.equals(promptRef, that.promptRef)
                && Objects.equals(type, that.type)
                && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(promptId, promptRef, type, value);
    }

    public UUID getPromptId() {
        return promptId;
    }

    public String getPromptRef() {
        return promptRef;
    }

    public String getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }

    public JsonValue getValue() {
        return value;
    }

    public void setPromptId(final UUID promptId) {
        this.promptId = promptId;
    }

    public void setPromptRef(final String promptRef) {
        this.promptRef = promptRef;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public void setValue(final JsonValue value) {
        this.value = value;
    }

    public static Builder prompt() {
        return new Builder();
    }

    public static class Builder {
        private UUID promptId;
        private String promptRef;
        private String type;
        private String label;
        private JsonValue value;


        public Builder withId(final UUID promptId) {
            this.promptId = promptId;
            return this;
        }

        public Builder withPromptRef(final String promptRef) {
            this.promptRef = promptRef;
            return this;
        }

        public Builder withLabel(final String label) {
            this.label = label;
            return this;
        }

        public Builder withValue(final JsonValue value) {
            this.value = value;
            return this;
        }

        public Builder withType(final String type) {
            this.type = type;
            return this;
        }


        public ResultPrompt build() {
            return new ResultPrompt(promptId, promptRef, type, label, value);
        }
    }
}
