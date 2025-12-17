package uk.gov.moj.cpp.progression.query.view;

import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Permission {
    private final String action;

    private final String description;

    private final UUID id;

    private final Boolean isAgent;

    private final String object;

    private final UUID source;

    private final String status;

    private final UUID target;

    public Permission(final String action, final String description, final UUID id, final Boolean isAgent, final String object, final UUID source, final String status, final UUID target) {
        this.action = action;
        this.description = description;
        this.id = id;
        this.isAgent = isAgent;
        this.object = object;
        this.source = source;
        this.status = status;
        this.target = target;
    }

    public String getAction() {
        return action;
    }

    public String getDescription() {
        return description;
    }

    public UUID getId() {
        return id;
    }

    public Boolean getIsAgent() {
        return isAgent;
    }

    public String getObject() {
        return object;
    }

    public UUID getSource() {
        return source;
    }

    public String getStatus() {
        return status;
    }

    public UUID getTarget() {
        return target;
    }

    public static Builder permission() {
        return new Permission.Builder();
    }

    public static class Builder {
        private String action;

        private String description;

        private UUID id;

        private Boolean isAgent;

        private String object;

        private UUID source;

        private String status;

        private UUID target;

        public Builder withAction(final String action) {
            this.action = action;
            return this;
        }

        public Builder withDescription(final String description) {
            this.description = description;
            return this;
        }

        public Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public Builder withIsAgent(final Boolean isAgent) {
            this.isAgent = isAgent;
            return this;
        }

        public Builder withObject(final String object) {
            this.object = object;
            return this;
        }

        public Builder withSource(final UUID source) {
            this.source = source;
            return this;
        }

        public Builder withStatus(final String status) {
            this.status = status;
            return this;
        }

        public Builder withTarget(final UUID target) {
            this.target = target;
            return this;
        }

        public Permission build() {
            return new Permission(action, description, id, isAgent, object, source, status, target);
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

        final Permission that = (Permission) o;

        return new EqualsBuilder()
                .append(source, that.source)
                .append(target, that.target)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(source)
                .append(target)
                .toHashCode();
    }
}
