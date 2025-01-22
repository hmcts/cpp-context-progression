package uk.gov.moj.cpp.progression.domain.pojo;

import java.io.Serializable;
import java.util.List;

@SuppressWarnings({"squid:S2384"})
public class RefDataDirection implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String assignee;

    private final RefData refData;

    private final String text;

    private final String dueDate;

    private final List<Prompt> prompts;

    private final Status status;

    private final Boolean subDirection;

    private final Boolean defaultDirection;

    private final List<String> categories;

    private final Integer sequenceNumber;

    private final String variant;

    private final Boolean nonPublishable;

    public RefDataDirection(final String assignee,
                            final RefData refData,
                            final String text,
                            final String dueDate,
                            final List<Prompt> prompts,
                            final Status status,
                            final Boolean subDirection,
                            final Boolean defaultDirection,
                            final List<String> categories,
                            final Integer sequenceNumber,
                            final String variant,
                            final Boolean nonPublishable) {
        this.assignee = assignee;
        this.refData = refData;
        this.text = text;
        this.dueDate = dueDate;
        this.prompts = prompts;
        this.status = status;
        this.subDirection = subDirection;
        this.defaultDirection = defaultDirection;
        this.categories = categories;
        this.sequenceNumber = sequenceNumber;
        this.variant = variant;
        this.nonPublishable = nonPublishable;
    }

    public String getAssignee() {
        return assignee;
    }

    public RefData getRefData() {
        return refData;
    }

    public String getText() {
        return text;
    }

    public String getDueDate() {
        return dueDate;
    }

    public List<Prompt> getPrompts() {
        return prompts;
    }

    public Status getStatus() {
        return status;
    }

    public Boolean getSubDirection() {
        return subDirection;
    }

    public Boolean getDefaultDirection() {
        return defaultDirection;
    }

    public List<String> getCategories() {
        return categories;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public String getVariant() {
        return variant;
    }

    public Boolean getNonPublishable() {
        return nonPublishable;
    }

    public static Builder refDataDirection() {
        return new Builder();
    }

    public static class Builder {
        private String assignee;

        private RefData refData;

        private String text;

        private String dueDate;

        private List<Prompt> prompts;

        private Status status;

        private Boolean subDirection;

        private Boolean defaultDirection;

        private List<String> categories;

        private Integer sequenceNumber;

        private String variant;

        private Boolean nonPublishable;

        public Builder withAssignee(final String assignee) {
            this.assignee = assignee;
            return this;
        }

        public Builder withRefData(final RefData refData) {
            this.refData = refData;
            return this;
        }

        public Builder withText(final String text) {
            this.text = text;
            return this;
        }

        public Builder withDueDate(final String dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public Builder withPrompts(final List<Prompt> prompts) {
            this.prompts = prompts;
            return this;
        }

        public Builder withCategories(final List<String> categories) {
            this.categories = categories;
            return this;
        }

        public Builder withStatus(final Status status) {
            this.status = status;
            return this;
        }

        public Builder withSubDirection(final Boolean subDirection) {
            this.subDirection = subDirection;
            return this;
        }

        public Builder withDefaultDirection(final Boolean defaultDirection) {
            this.defaultDirection = defaultDirection;
            return this;
        }
        public Builder withSequenceNumber(final Integer sequenceNumber) {
            this.sequenceNumber = sequenceNumber;
            return this;
        }
        public Builder withVariant(final String variant) {
            this.variant = variant;
            return this;
        }

        public Builder withNonPublishable(final Boolean nonPublishable) {
            this.nonPublishable = nonPublishable;
            return this;
        }



        public Builder withValuesFrom(final RefDataDirection direction) {
            this.assignee = direction.getAssignee();
            this.refData = direction.getRefData();
            this.text = direction.getText();
            this.dueDate = direction.getDueDate();
            this.prompts = direction.getPrompts();
            this.status = direction.getStatus();
            this.subDirection = direction.getSubDirection();
            this.defaultDirection = direction.getDefaultDirection();
            this.categories = direction.getCategories();
            this.sequenceNumber = direction.getSequenceNumber();
            this.variant = direction.getVariant();
            this.nonPublishable = direction.getNonPublishable();
            return this;
        }

        public RefDataDirection build() {
            return new RefDataDirection(assignee, refData, text, dueDate, prompts, status, subDirection, defaultDirection, categories, sequenceNumber,variant, nonPublishable);
        }
    }
}
