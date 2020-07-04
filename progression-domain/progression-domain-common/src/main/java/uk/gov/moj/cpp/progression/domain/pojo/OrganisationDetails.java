package uk.gov.moj.cpp.progression.domain.pojo;

import java.util.Objects;
import java.util.UUID;

@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
public class OrganisationDetails {

    private final UUID id;
    private final String name;
    private final String type;
    private final String addressLine1;
    private final String addressLine2;
    private final String addressLine3;
    private final String addressLine4;
    private final String addressPostcode;
    private final String phoneNumber;
    private final String email;
    private final String laaContractNumber;

    public OrganisationDetails(final UUID id, final String name, final String type) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.addressLine1 = "";
        this.addressLine2 = "";
        this.addressLine3 = "";
        this.addressLine4 = "";
        this.addressPostcode = "";
        this.phoneNumber = "";
        this.email = "";
        this.laaContractNumber = "";
    }

    private OrganisationDetails(final Builder builder) {
        id = builder.id;
        name = builder.name;
        type = builder.type;
        addressLine1 = builder.addressLine1;
        addressLine2 = builder.addressLine2;
        addressLine3 = builder.addressLine3;
        addressLine4 = builder.addressLine4;
        addressPostcode = builder.addressPostcode;
        phoneNumber = builder.phoneNumber;
        email = builder.email;
        laaContractNumber = builder.laaContractNumber;
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

    public String getAddressLine1() {
        return addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public String getAddressLine3() {
        return addressLine3;
    }

    public String getAddressLine4() {
        return addressLine4;
    }

    public String getAddressPostcode() {
        return addressPostcode;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public String getLaaContractNumber() {
        return laaContractNumber;
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

    @SuppressWarnings("PMD:BeanMembersShouldSerialize")
    public static final class Builder {
        private UUID id;
        private String name;
        private String type;
        private String addressLine1;
        private String addressLine2;
        private String addressLine3;
        private String addressLine4;
        private String addressPostcode;
        private String phoneNumber;
        private String email;
        private String laaContractNumber;

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

        public Builder withAddressLine1(final String val) {
            addressLine1 = val;
            return this;
        }

        public Builder withAddressLine2(final String val) {
            addressLine2 = val;
            return this;
        }

        public Builder withAddressLine3(final String val) {
            addressLine3 = val;
            return this;
        }

        public Builder withAddressLine4(final String val) {
            addressLine4 = val;
            return this;
        }

        public Builder withAddressPostcode(final String val) {
            addressPostcode = val;
            return this;
        }

        public Builder withPhoneNumber(final String val) {
            phoneNumber = val;
            return this;
        }

        public Builder withLaaContractNumber(final String val) {
            laaContractNumber = val;
            return this;
        }

        public Builder withEmail(final String val) {
            email = val;
            return this;
        }

        public OrganisationDetails build() {
            return new OrganisationDetails(this);
        }
    }
}

