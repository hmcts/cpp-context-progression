package uk.gov.moj.cpp.progression.query.view.response;

import java.time.LocalDate;
import java.util.UUID;

/**
 *
 * @deprecated This is deprecated for Release 2.4
 *
 */
@Deprecated
@SuppressWarnings({"squid:S1186", "squid:S1133"})
public class Person {

    private UUID id;

    private String title;

    private String firstName;

    private String lastName;

    private LocalDate dateOfBirth;

    private String nationality;

    private String gender;

    private String homeTelephone;

    private String workTelephone;

    private String mobile;

    private String fax;

    private String email;

    private Address address;

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(final LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(final String nationality) {
        this.nationality = nationality;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(final String gender) {
        this.gender = gender;
    }

    public String getHomeTelephone() {
        return homeTelephone;
    }

    public void setHomeTelephone(final String homeTelephone) {
        this.homeTelephone = homeTelephone;
    }

    public String getWorkTelephone() {
        return workTelephone;
    }

    public void setWorkTelephone(final String workTelephone) {
        this.workTelephone = workTelephone;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(final String mobile) {
        this.mobile = mobile;
    }

    public String getFax() {
        return fax;
    }

    public void setFax(final String fax) {
        this.fax = fax;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(final Address address) {
        this.address = address;
    }

    public Builder builder() {
        return new Builder();
    }

    class Builder {


        public Builder() {

        }

        public Builder personId(final UUID personId) {
            setId(personId);
            return this;
        }

        public Builder title(final String title) {
            setTitle(title);
            return this;
        }

        public Builder firstName(final String firstName) {
            setFirstName(firstName);
            return this;

        }

        public Builder lastName(final String lastName) {
            setLastName(lastName);
            return this;
        }

        public Builder dateOfBirth(final LocalDate dateOfBirth) {
            setDateOfBirth(dateOfBirth);
            return this;
        }

        public Builder nationality(final String nationality) {
            setNationality(nationality);
            return this;
        }

        public Builder gender(final String gender) {
            setGender(gender);
            return this;
        }

        public Builder homeTelephone(final String homeTelephone) {
            setHomeTelephone(homeTelephone);
            return this;
        }

        public Builder workTelephone(final String workTelephone) {
            setWorkTelephone(workTelephone);
            return this;
        }

        public Builder mobile(final String mobile) {
            setMobile(mobile);
            return this;
        }

        public Builder fax(final String fax) {
            setFax(fax);
            return this;
        }

        public Builder email(final String email) {
            setEmail(email);
            return this;
        }

        public Builder address(final uk.gov.moj.cpp.progression.persistence.entity.Address address) {
            final Address personAddress = new Address();
            personAddress.builder()
                    .address1(address.getAddress1())
                    .address2(address.getAddress2())
                    .address3(address.getAddress3())
                    .address4(address.getAddress4())
                    .postCode(address.getPostCode())
                    .build();
            setAddress(personAddress);
            return this;
        }

        public Person build() {
            final Person person = new Person();
            person.setFirstName(getFirstName());
            person.setLastName(getLastName());
            person.setId(getId());
            person.setDateOfBirth(getDateOfBirth());
            person.setEmail(person.getEmail());
            person.setGender(getGender());
            person.setAddress(getAddress());
            person.setFax(getFax());
            person.setHomeTelephone(getHomeTelephone());
            person.setWorkTelephone(getWorkTelephone());
            person.setId(getId());
            person.setTitle(getTitle());
            person.setNationality(getNationality());
            return person;
        }

    }


}





