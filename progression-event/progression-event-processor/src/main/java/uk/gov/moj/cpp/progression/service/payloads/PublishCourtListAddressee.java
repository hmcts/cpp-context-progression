package uk.gov.moj.cpp.progression.service.payloads;

public class PublishCourtListAddressee {
    private final String name;
    private final String email;
    private final PublishCourtListAddress address;

    private PublishCourtListAddressee(final String name, final String email, final PublishCourtListAddress address) {
        this.name = name;
        this.email = email;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public PublishCourtListAddress getAddress() {
        return address;
    }

    public static PublishCourtListAddresseeBuilder publishCourtListAddresseeBuilder() {
        return new PublishCourtListAddresseeBuilder();
    }

    public static final class PublishCourtListAddresseeBuilder {
        private String name;
        private String email;
        private PublishCourtListAddress address;

        private PublishCourtListAddresseeBuilder() {
        }

        public PublishCourtListAddresseeBuilder withName(final String name) {
            this.name = name;
            return this;
        }

        public PublishCourtListAddresseeBuilder withEmail(final String email) {
            this.email = email;
            return this;
        }

        public PublishCourtListAddresseeBuilder withAddress(final PublishCourtListAddress address) {
            this.address = address;
            return this;
        }

        public PublishCourtListAddressee build() {
            return new PublishCourtListAddressee(name, email, address);
        }
    }
}
