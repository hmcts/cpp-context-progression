package uk.gov.justice.api.resource.dto;

import java.util.List;
import java.util.UUID;

public class ResultDefinition {
    private final UUID id;
    private final String label;
    private final String category;
    private final String resultDefinitionGroup;

    private final List<ResultDefinitionPrompt> prompts;

    public ResultDefinition(final UUID id, final String label, final String category, final String resultDefinitionGroup, final List<ResultDefinitionPrompt> prompts) {
        this.id = id;
        this.label = label;
        this.category = category;
        this.resultDefinitionGroup = resultDefinitionGroup;
        this.prompts = prompts;
    }

    public UUID getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getResultDefinitionGroup() {
        return resultDefinitionGroup;
    }

    public String getCategory() {
        return category;
    }

    public List<ResultDefinitionPrompt> getPrompts() {
        return prompts;
    }

    public static ResultDefinition.Builder builder() {
        return new ResultDefinition.Builder();
    }

    public static class Builder {
        private UUID id;
        private String label;
        private String category;
        private String resultDefinitionGroup;
        private List<ResultDefinitionPrompt> prompts;

        public ResultDefinition.Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public ResultDefinition.Builder withLabel(final String label) {
            this.label = label;
            return this;
        }

        public ResultDefinition.Builder withCategory(final String category) {
            this.category = category;
            return this;
        }

        public ResultDefinition.Builder withResultDefinitionGroup(final String resultDefinitionGroup) {
            this.resultDefinitionGroup = resultDefinitionGroup;
            return this;
        }

        public ResultDefinition.Builder withPrompts(final List<ResultDefinitionPrompt> prompts) {
            this.prompts = prompts;
            return this;
        }

        public ResultDefinition build() {
            return new ResultDefinition(id, label, category, resultDefinitionGroup, prompts);
        }
    }

}
