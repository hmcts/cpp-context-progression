package uk.gov.moj.cpp.progression.command.handler.service.payloads;

import java.util.Objects;
import java.util.UUID;

public class OrganisationDetails {

    private final UUID id;
    private final String name;
    private final String type;

    public OrganisationDetails(final UUID id, final String name, final String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    private OrganisationDetails(final Builder builder) {
        id = builder.id;
        name = builder.name;
        type = builder.type;
    }

    public static OrganisationDetails of(final UUID id, final String name, final String type) {
        return new OrganisationDetails(id, name, type);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(final OrganisationDetails copy) {
        final Builder builder = new Builder();
        builder.id = copy.getId();
        builder.name = copy.getName();
        builder.type = copy.getType();
        return builder;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final OrganisationDetails that = (OrganisationDetails) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, type);
    }

    @Override
    public String toString() {
        return "OrganisationDetails{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                '}';
    }

    public static final class Builder {
        private UUID id;
        private String name;
        private String type;

        private Builder() {
        }

        public Builder withId(final UUID val) {
            id = val;
            return this;
        }

        public Builder withName(final String val) {
            name = val;
            return this;
        }

        public Builder withType(final String val) {
            type = val;
            return this;
        }

        public OrganisationDetails build() {
            return new OrganisationDetails(this);
        }
    }
}
