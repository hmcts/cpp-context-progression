package uk.gov.moj.cpp.progression.domain.event.defendant;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;
/**
 * 
 * @deprecated
 *
 */
@Deprecated
public class Person implements Serializable {

    private static final long serialVersionUID = 1L;

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

    @SuppressWarnings("squid:S00107")
    public Person(final UUID id, final String title, final String firstName, final String lastName, final LocalDate dateOfBirth, final String nationality, final String gender, final String homeTelephone, final String workTelephone, final String mobile, final String fax, final String email, final Address address) {
        this.id = id;
        this.title = title;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.nationality = nationality;
        this.gender = gender;
        this.homeTelephone = homeTelephone;
        this.workTelephone = workTelephone;
        this.mobile = mobile;
        this.fax = fax;
        this.email = email;
        this.address = address;
    }

    //default constructor
    public Person() {
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getNationality() {
        return nationality;
    }

    public String getGender() {
        return gender;
    }

    public String getHomeTelephone() {
        return homeTelephone;
    }

    public String getWorkTelephone() {
        return workTelephone;
    }

    public String getMobile() {
        return mobile;
    }

    public String getFax() {
        return fax;
    }

    public String getEmail() {
        return email;
    }

    public Address getAddress() {
        return address;
    }
}
