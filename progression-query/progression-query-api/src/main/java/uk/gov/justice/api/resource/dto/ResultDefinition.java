package uk.gov.justice.api.resource.dto;

import java.util.UUID;

public class ResultDefinition {
    private final UUID id;
    private final String category;
    private final String resultDefinitionGroup;

    public ResultDefinition(final UUID id, final String category, final String resultDefinitionGroup) {
        this.id = id;
        this.category = category;
        this.resultDefinitionGroup = resultDefinitionGroup;
    }

    public UUID getId() {
        return id;
    }

    public String getResultDefinitionGroup() {
        return resultDefinitionGroup;
    }

    public String getCategory() {
        return category;
    }

    public static ResultDefinition.Builder builder() {
        return new ResultDefinition.Builder();
    }

    public static class Builder {
        private UUID id;
        private String category;
        private String resultDefinitionGroup;

        public ResultDefinition.Builder withId(final UUID id) {
            this.id = id;
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


        public ResultDefinition build() {
            return new ResultDefinition(id, category, resultDefinitionGroup);
        }
    }

}
