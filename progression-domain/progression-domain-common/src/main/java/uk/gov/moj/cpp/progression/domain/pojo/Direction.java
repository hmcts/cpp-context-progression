package uk.gov.moj.cpp.progression.domain.pojo;


import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@SuppressWarnings({"squid:S2384"})
public class Direction implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String assignee;

    private final UUID directionId;

    private final String displayText;

    private final String dueDate;

    private final List<Prompt> prompts;

    private final Status status;

    private final Boolean subDirection;

    private final String displayTextStatic;

    private final Boolean defaultDirection;

    private final Boolean nonPublishable;

    private final Integer sequenceNumber;

    public Direction(final String assignee, final UUID directionId, final String displayText, final String dueDate, final List<Prompt> prompts, final Status status, final Boolean subDirection,
                     final Boolean defaultDirection,final String displayTextStatic, final Boolean nonPublishable, final Integer sequenceNumber) {
        this.assignee = assignee;
        this.directionId = directionId;
        this.displayText = displayText;
        this.dueDate = dueDate;
        this.prompts = prompts;
        this.status = status;
        this.subDirection = subDirection;
        this.defaultDirection = defaultDirection;
        this.displayTextStatic = displayTextStatic;
        this.nonPublishable = nonPublishable;
        this.sequenceNumber = sequenceNumber;
    }

    public String getAssignee() {
        return assignee;
    }

    public UUID getDirectionId() {
        return directionId;
    }

    public String getDisplayText() {
        return displayText;
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

    public static Builder direction() {
        return new Builder();
    }

    public String getDisplayTextStatic() {
        return displayTextStatic;
    }

    public Boolean getNonPublishable() {
        return nonPublishable;
    }

    public Integer getSequenceNumber() { return sequenceNumber; }

    public static class Builder {
        private String assignee;

        private UUID directionId;

        private String displayText;

        private String dueDate;

        private List<Prompt> prompts;

        private Status status;

        private Boolean subDirection;

        private Boolean defaultDirection;

        private String displayTextStatic;

        private Boolean nonPublishable;

        private Integer sequenceNumber;

        public Builder withAssignee(final String assignee) {
            this.assignee = assignee;
            return this;
        }

        public Builder withDirectionId(final UUID directionId) {
            this.directionId = directionId;
            return this;
        }

        public Builder withDisplayText(final String displayText) {
            this.displayText = displayText;
            return this;
        }

        public Builder withDisplayTextStatic(final String displayTextStatic) {
            this.displayTextStatic = displayTextStatic;
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

        public Builder withNonPublishable(final Boolean nonPublishable) {
            this.nonPublishable = nonPublishable;
            return this;
        }

        public Builder withSequenceNumber(final Integer sequenceNumber) {
            this.sequenceNumber = sequenceNumber;
            return this;
        }

        public Builder withValuesFrom(final Direction direction) {
            this.assignee = direction.getAssignee();
            this.directionId = direction.getDirectionId();
            this.displayText = direction.getDisplayText();
            this.dueDate = direction.getDueDate();
            this.prompts = direction.getPrompts();
            this.status = direction.getStatus();
            this.subDirection = direction.getSubDirection();
            this.defaultDirection = direction.getDefaultDirection();
            this.displayTextStatic = direction.getDisplayTextStatic();
            this.nonPublishable = direction.getNonPublishable();
            this.sequenceNumber = direction.getSequenceNumber();
            return this;
        }

        public Direction build() {
            return new Direction(assignee, directionId, displayText, dueDate, prompts, status, subDirection, defaultDirection, displayTextStatic, nonPublishable, sequenceNumber);
        }
    }
}
