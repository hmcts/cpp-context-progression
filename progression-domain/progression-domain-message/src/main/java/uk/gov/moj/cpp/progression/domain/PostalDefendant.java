package uk.gov.moj.cpp.progression.domain;

import java.time.LocalDate;

public class PostalDefendant {

    private String name;

    private LocalDate dateOfBirth;

    private PostalAddress address;

    public PostalDefendant(final String name, final LocalDate dateOfBirth, final PostalAddress address) {
        this.name = name;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public PostalAddress getAddress() {
        return address;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "PostalDefendant{" +
                "name='" + name + '\'' +
                ", dateOfBirth=" + dateOfBirth +
                ", address=" + address +
                '}';
    }

    public static class Builder {

        private String name;

        private LocalDate dateOfBirth;

        private PostalAddress address;

        public Builder withName(final String name) {
            this.name = name;
            return this;
        }

        public Builder withDateOfBirth(final LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public Builder withAddress(final PostalAddress address) {
            this.address = address;
            return this;
        }

        public PostalDefendant build() {
            return new PostalDefendant(name, dateOfBirth, address);
        }
    }
}
