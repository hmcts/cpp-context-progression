package uk.gov.moj.cpp.progression.listing.domain;

import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class BailStatus {

    private final String code;
    private final String description;
    private final UUID id;

    public BailStatus(final String code, final String description, final UUID id) {
        this.code = code;
        this.description = description;
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public UUID getId() {
        return id;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof BailStatus)) {
            return false;
        }

        final BailStatus that = (BailStatus) o;

        return new EqualsBuilder()
                .append(getCode(), that.getCode())
                .append(getDescription(), that.getDescription())
                .append(getId(), that.getId())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getCode())
                .append(getDescription())
                .append(getId())
                .toHashCode();
    }

    @Override
    public String toString() {
        return "BailStatus{" +
                "code='" + code + '\'' +
                ", description='" + description + '\'' +
                ", id=" + id +
                '}';
    }

    @SuppressWarnings("pmd:BeanMembersShouldSerialize")
    public static class Builder {
        private String code;
        private String description;
        private UUID id;


        public BailStatus.Builder withCode(final String code) {
            this.code = code;
            return this;
        }

        public BailStatus.Builder withDescription(final String description) {
            this.description = description;
            return this;
        }

        public BailStatus.Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public BailStatus build() {
            return new BailStatus(code, description, id);
        }
    }

}
