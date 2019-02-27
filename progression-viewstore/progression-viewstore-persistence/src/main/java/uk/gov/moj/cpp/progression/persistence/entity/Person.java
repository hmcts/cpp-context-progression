package uk.gov.moj.cpp.progression.persistence.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;


/**
 *
 * @deprecated
 *
 */
@Deprecated
@Entity
@Table(name = "person")
@SuppressWarnings({"squid:S1186","squid:S1067", "squid:S1133"})
public class Person implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "person_id")
    private UUID personId;

    @Column(name = "title")
    private String title;
    @Column(name = "first_name", nullable = false)
    private String firstName;
    @Column(name = "last_name", nullable = false)
    private String lastName;
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;
    @Column(name = "nationality")
    private String nationality;
    @Column(name = "gender")
    private String gender;
    @Column(name = "home_telephone")
    private String homeTelephone;
    @Column(name = "work_telephone")
    private String workTelephone;
    @Column(name = "mobile")
    private String mobile;
    @Column(name = "fax")
    private String fax;
    @Column(name = "email")
    private String email;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "address_id")
    private Address address;

    public Builder builder() {
        return new Builder();
    }

    public Person() {
    }

    public UUID getPersonId() {
        return personId;
    }

    public void setPersonId(final UUID personId) {
        this.personId = personId;
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


    public class Builder {


        public Builder() {

        }

        public Builder personId(final UUID personId) {
            setPersonId(personId);
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

        public Builder address(final UUID addressId, final String address1, final String address2, final String address3, final String address4, final String postCode) {
            final Address personAddress = new Address();
            personAddress.builder()
                    .addressId(addressId)
                    .address1(address1)
                    .address2(address2)
                    .address3(address3)
                    .address4(address4)
                    .postCode(postCode)
                    .build();
            setAddress(personAddress);
            return this;
        }

        public Builder address(final uk.gov.moj.cpp.progression.persistence.entity.Address address) {
            final Address personAddress = new Address();
            personAddress.builder()
                    .addressId(address.getAddressId())
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
            person.setPersonId(getPersonId());
            person.setDateOfBirth(getDateOfBirth());
            person.setEmail(getEmail());
            person.setGender(getGender());
            person.setAddress(getAddress());
            person.setFax(getFax());
            person.setMobile(getMobile());
            person.setHomeTelephone(getHomeTelephone());
            person.setWorkTelephone(getWorkTelephone());
            person.setPersonId(getPersonId());
            person.setTitle(getTitle());
            person.setNationality(getNationality());
            return person;
        }

    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Person person = (Person) o;
        return Objects.equals(getPersonId(), person.getPersonId()) &&
                Objects.equals(getTitle(), person.getTitle()) &&
                Objects.equals(getFirstName(), person.getFirstName()) &&
                Objects.equals(getLastName(), person.getLastName()) &&
                Objects.equals(getDateOfBirth(), person.getDateOfBirth()) &&
                Objects.equals(getNationality(), person.getNationality()) &&
                Objects.equals(getGender(), person.getGender()) &&
                this.getAddress().equals(person.getAddress());

    }

    @Override
    public int hashCode() {
        return Objects.hash(getPersonId(), getTitle(), getFirstName(), getLastName(), getDateOfBirth(),
                getNationality(), getGender(),
                this.address != null ? this.address.hashCode() : 0);
    }


}

