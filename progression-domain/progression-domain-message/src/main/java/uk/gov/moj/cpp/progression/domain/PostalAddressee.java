package uk.gov.moj.cpp.progression.domain;

public class PostalAddressee {

    private String name;

    private PostalAddress address;

    public PostalAddressee(final String name, final PostalAddress address) {
        this.name = name;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public PostalAddress getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return "PostalAddressee{" +
                "name='" + name + '\'' +
                ", address=" + address +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String name;

        private PostalAddress address;

        public Builder withName(final String name) {
            this.name = name;
            return this;
        }

        public Builder withAddress(final PostalAddress address) {
            this.address = address;
            return this;
        }

        public PostalAddressee build() {
            return new PostalAddressee(name, address);
        }
    }
}
