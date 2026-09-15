package uk.gov.moj.cpp.progression.listing.domain;

import static java.util.Optional.empty;

import java.util.Optional;
import java.util.UUID;

@SuppressWarnings({"squid:S00121", "squid:S1067", "pmd:BeanMembersShouldSerialize"})
public class LaaReference {
    private final String applicationReference;

    private Optional<String> effectiveEndDate;

    private Optional<String> effectiveStartDate;

    private final String statusCode;

    private final String statusDate;

    private final String statusDescription;

    private final UUID statusId;

    public LaaReference(final String applicationReference, final Optional<String> effectiveEndDate, final Optional<String> effectiveStartDate, final String statusCode, final String statusDate, final String statusDescription, final UUID statusId) {
        this.applicationReference = applicationReference;
        this.effectiveEndDate = effectiveEndDate;
        this.effectiveStartDate = effectiveStartDate;
        this.statusCode = statusCode;
        this.statusDate = statusDate;
        this.statusDescription = statusDescription;
        this.statusId = statusId;
    }

    public static Builder laaReference() {
        return new LaaReference.Builder();
    }

    public String getApplicationReference() {
        return applicationReference;
    }

    public Optional<String> getEffectiveEndDate() {
        return effectiveEndDate.isPresent() ? effectiveEndDate : empty();
    }

    public Optional<String> getEffectiveStartDate() {
        return effectiveStartDate.isPresent() ? effectiveStartDate : empty();
    }

    public String getStatusCode() {
        return statusCode;
    }

    public String getStatusDate() {
        return statusDate;
    }

    public String getStatusDescription() {
        return statusDescription;
    }

    public UUID getStatusId() {
        return statusId;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()){
            return false;
        }
        final LaaReference that = (LaaReference) obj;

        return java.util.Objects.equals(this.applicationReference, that.applicationReference) &&
                java.util.Objects.equals(this.effectiveEndDate, that.effectiveEndDate) &&
                java.util.Objects.equals(this.effectiveStartDate, that.effectiveStartDate) &&
                java.util.Objects.equals(this.statusCode, that.statusCode) &&
                java.util.Objects.equals(this.statusDate, that.statusDate) &&
                java.util.Objects.equals(this.statusDescription, that.statusDescription) &&
                java.util.Objects.equals(this.statusId, that.statusId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(applicationReference, effectiveEndDate, effectiveStartDate, statusCode, statusDate, statusDescription, statusId);
    }

    @Override
    public String toString() {
        return "LaaReference{" +
                "applicationReference='" + applicationReference + "'," +
                "effectiveEndDate='" + effectiveEndDate + "'," +
                "effectiveStartDate='" + effectiveStartDate + "'," +
                "statusCode='" + statusCode + "'," +
                "statusDate='" + statusDate + "'," +
                "statusDescription='" + statusDescription + "'," +
                "statusId='" + statusId + "'" +
                "}";
    }

    @SuppressWarnings("pmd:BeanMembersShouldSerialize")
    public static class Builder {
        private String applicationReference;

        private Optional<String> effectiveEndDate = empty();

        private Optional<String> effectiveStartDate = empty();

        private String statusCode;

        private String statusDate;

        private String statusDescription;

        private UUID statusId;

        public Builder withApplicationReference(final String applicationReference) {
            this.applicationReference = applicationReference;
            return this;
        }

        public Builder withEffectiveEndDate(final Optional<String> effectiveEndDate) {
            this.effectiveEndDate = effectiveEndDate;
            return this;
        }

        public Builder withEffectiveStartDate(final Optional<String> effectiveStartDate) {
            this.effectiveStartDate = effectiveStartDate;
            return this;
        }

        public Builder withStatusCode(final String statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder withStatusDate(final String statusDate) {
            this.statusDate = statusDate;
            return this;
        }

        public Builder withStatusDescription(final String statusDescription) {
            this.statusDescription = statusDescription;
            return this;
        }

        public Builder withStatusId(final UUID statusId) {
            this.statusId = statusId;
            return this;
        }

        public LaaReference build() {
            return new LaaReference(applicationReference, effectiveEndDate, effectiveStartDate, statusCode, statusDate, statusDescription, statusId);
        }
    }
}
