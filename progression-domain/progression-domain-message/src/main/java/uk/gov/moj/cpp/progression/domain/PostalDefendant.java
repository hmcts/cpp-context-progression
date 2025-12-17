package uk.gov.moj.cpp.progression.domain;

import java.io.Serializable;
import java.time.LocalDate;

@SuppressWarnings({"squid:S2384", "PMD.BeanMembersShouldSerialize", "squid:S00107"})
public class PostalDefendant implements Serializable {

    private static final long serialVersionUID = -3888033919532299214L;

    private String name;

    private String title;

    private String firstName;
    private String middleName;
    private String lastName;

    private LocalDate dateOfBirth;

    private PostalAddress address;

    public PostalDefendant(final String name, final String title, final String firstName,final String middleName, final String lastName, final LocalDate dateOfBirth, final PostalAddress address) {
        this.name = name;
        this.title = title;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
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
                ",title="+ title + '\''+
                ",firstName="+ firstName + '\''+
                ",middleName="+ middleName + '\''+
                ",lastName="+ lastName + '\''+
                ", dateOfBirth=" + dateOfBirth +
                ", address=" + address +
                '}';
    }

    @SuppressWarnings("pmd:BeanMembersShouldSerialize")
    public static class Builder {

        private String name;

        private String title;

        private String firstName;
        private String middleName;
        private String lastName;
        private LocalDate dateOfBirth;

        private PostalAddress address;

        public Builder withName(final String name) {
            this.name = name;
            return this;
        }
        public Builder withTitle(final String title) {
            this.title = title;
            return this;
        }

        public Builder withFirstName(final String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder withMiddleName(final String middleName) {
            this.middleName = middleName;
            return this;
        }
        public Builder withLastName(final String lastName) {
            this.lastName = lastName;
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
            return new PostalDefendant(name,title, firstName,middleName, lastName,  dateOfBirth, address);
        }
    }
}
