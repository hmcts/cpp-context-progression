package uk.gov.moj.cpp.progression.service.payloads;


import uk.gov.moj.cpp.progression.events.RepresentationType;

import java.time.ZonedDateTime;
import java.util.UUID;

public class AssociatedDefenceOrganisation {
    private String status;
    private UUID organisationId;
    private String organisationName;
    private RepresentationType representationType;
    private ZonedDateTime startDate;
    private DefenceOrganisationAddress address;
    private String phoneNumber;
    private String email;

    public AssociatedDefenceOrganisation(final String status, final UUID organisationId, final String organisationName,
                                         final RepresentationType representationType, final ZonedDateTime startDate,
                                         final DefenceOrganisationAddress address, final String phoneNumber, final String email) {
        this.status = status;
        this.organisationId = organisationId;
        this.organisationName = organisationName;
        this.representationType = representationType;
        this.startDate = startDate;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.email = email;
    }

    public AssociatedDefenceOrganisation() {

    }

    public String getStatus() {
        return status;
    }

    public UUID getOrganisationId() {
        return organisationId;
    }

    public String getOrganisationName() {
        return organisationName;
    }

    public RepresentationType getRepresentationType() {
        return representationType;
    }

    public ZonedDateTime getStartDate() {
        return startDate;
    }

    public DefenceOrganisationAddress getAddress() {
        return address;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public static Builder associatedDefenceOrganisationBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private String status;
        private UUID organisationId;
        private String organisationName;
        private RepresentationType representationType;
        private ZonedDateTime startDate;
        private DefenceOrganisationAddress address;
        private String phoneNumber;
        private String email;

        private Builder() {
        }

        public Builder withStatus(final String status) {
            this.status = status;
            return this;
        }

        public Builder withOrganisationId(final UUID organisationId) {
            this.organisationId = organisationId;
            return this;
        }

        public Builder withOrganisationName(final String organisationName) {
            this.organisationName = organisationName;
            return this;
        }

        public Builder withRepresentationType(final RepresentationType representationType) {
            this.representationType = representationType;
            return this;
        }

        public Builder withStartDate(final ZonedDateTime startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder withAddress(final DefenceOrganisationAddress address) {
            this.address = address;
            return this;
        }

        public Builder withPhoneNumber(final String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public Builder withEmail(final String email) {
            this.email = email;
            return this;
        }

        public AssociatedDefenceOrganisation build() {
            return new AssociatedDefenceOrganisation(status, organisationId, organisationName, representationType, startDate, address, phoneNumber, email);
        }
    }
}
