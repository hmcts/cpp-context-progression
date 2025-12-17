package uk.gov.moj.cpp.progression.domain.pojo;

import static java.util.Objects.hash;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PrisonCustodySuite implements Serializable {
    private static final long serialVersionUID = 2263527652172125205L;

    private final UUID id;

    private final String name;

    private final String type;

    public PrisonCustodySuite(final UUID id, final String name, final String type) {
        this.id = id;
        this.name = name;
        this.type = type;
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

    public static Builder prisonCustodySuite() {
        return new PrisonCustodySuite.Builder();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final PrisonCustodySuite that = (PrisonCustodySuite) obj;

        return java.util.Objects.equals(this.id, that.id) &&
                java.util.Objects.equals(this.name, that.name) &&
                java.util.Objects.equals(this.type, that.type);
    }

    @Override
    public int hashCode() {
        return hash(id, name, type);
    }

    public static final class Builder {
        private UUID id;

        private String name;

        private String type;

        public Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public Builder withName(final String name) {
            this.name = name;
            return this;
        }

        public Builder withType(final String type) {
            this.type = type;
            return this;
        }

        public Builder withValuesFrom(final PrisonCustodySuite prisonCustodySuite) {
            this.id = prisonCustodySuite.getId();
            this.name = prisonCustodySuite.getName();
            this.type = prisonCustodySuite.getType();
            return this;
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

        public PrisonCustodySuite build() {
            return new PrisonCustodySuite(id, name, type);
        }
    }
}
