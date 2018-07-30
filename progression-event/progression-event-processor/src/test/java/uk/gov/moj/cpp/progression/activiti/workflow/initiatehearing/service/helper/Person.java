package uk.gov.moj.cpp.progression.activiti.workflow.initiatehearing.service.helper;

import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;


public class Person {
    private UUID id;
    private String title;
    private String firstName;
    private String lastName;
    private String dateOfBirth;
    private String nationality;
    private String gender;
    private String homeTelephone;
    private String workTelephone;
    private String mobile;
    private String fax;
    private String email;

    public Person() {

    }

    public Person(UUID id, String title, String firstName, String lastName, String dateOfBirth,
                    String nationality, String gender, String homeTelephone, String workTelephone,
                    String mobile, String fax, String email) {
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

    public String getDateOfBirth() {
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

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(id).append(title).append(firstName).append(dateOfBirth)
                        .append(nationality).append(gender).append(homeTelephone)
                        .append(workTelephone).append(mobile).append(email).append(fax).hashCode();
    }

    @Override
    public boolean equals(Object obj) {


        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Person other = (Person) obj;
        return new EqualsBuilder().append(id, other.id).append(title, other.getTitle())
                        .append(firstName, other.getFirstName())
                        .append(dateOfBirth, other.getDateOfBirth())
                        .append(nationality, other.getNationality())
                        .append(gender, other.getGender())
                        .append(homeTelephone, other.getHomeTelephone())
                        .append(workTelephone, other.getWorkTelephone())
                        .append(mobile, other.getMobile()).append(email, other.getEmail())
                        .append(fax, other.getFax()).isEquals();

    }



}
