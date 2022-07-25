package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import static java.util.Optional.ofNullable;

import uk.gov.justice.core.courts.Defendant;

import java.time.LocalDate;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
public class OnlinePleaPersonalDetails {
    @Column(name = "personal_details_first_name")
    private String firstName;
    @Column(name = "personal_details_last_name")
    private String lastName;

    @AttributeOverrides({
            @AttributeOverride(name = "address1", column = @Column(name = "personal_details_address1")),
            @AttributeOverride(name = "address2", column = @Column(name = "personal_details_address2")),
            @AttributeOverride(name = "address3", column = @Column(name = "personal_details_address3")),
            @AttributeOverride(name = "address4", column = @Column(name = "personal_details_address4")),
            @AttributeOverride(name = "address5", column = @Column(name = "personal_details_address5")),
            @AttributeOverride(name = "postcode", column = @Column(name = "personal_details_postcode"))
    })
    private Address address;

    @Column(name = "personal_details_telephone_number_home")
    private String homeTelephone;
    @Column(name = "personal_details_telephone_number_mobile")
    private String mobile;
    @Column(name = "personal_details_email")
    private String email;
    @Column(name = "personal_details_date_of_birth")
    private LocalDate dateOfBirth;
    @Column(name = "personal_details_national_insurance_number")
    private String nationalInsuranceNumber;
    @Column(name = "personal_details_driver_number")
    private String driverNumber;
    @Column(name = "personal_details_driver_licence_details")
    private String driverLicenceDetails;

    public OnlinePleaPersonalDetails() {
    }

    public OnlinePleaPersonalDetails(final Defendant defendantDetail) {
        ofNullable(defendantDetail.getPersonDefendant()).ifPresent(personalDetails -> ofNullable(personalDetails.getPersonDetails()).ifPresent(person -> {
            this.firstName = person.getFirstName();
            this.lastName = person.getLastName();
            this.address = convertToEntity(person.getAddress());
            ofNullable(person.getContact()).ifPresent(contactDetails -> {
                this.homeTelephone = contactDetails.getHome();
                this.mobile = contactDetails.getMobile();
                this.email = contactDetails.getPrimaryEmail();
            });
            this.dateOfBirth = person.getDateOfBirth();
            this.nationalInsuranceNumber = person.getNationalInsuranceNumber();
            this.driverNumber = personalDetails.getDriverNumber();
        }));
    }

    private Address convertToEntity(final uk.gov.justice.core.courts.Address address) {
        return new Address(address.getAddress1(), address.getAddress2(), address.getAddress3(), address.getAddress4(), address.getAddress5(), address.getPostcode());
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getHomeTelephone() {
        return homeTelephone;
    }

    public void setHomeTelephone(String homeTelephone) {
        this.homeTelephone = homeTelephone;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getNationalInsuranceNumber() {
        return nationalInsuranceNumber;
    }

    public void setNationalInsuranceNumber(String nationalInsuranceNumber) {
        this.nationalInsuranceNumber = nationalInsuranceNumber;
    }

    public String getDriverNumber() {
        return driverNumber;
    }

    public void setDriverNumber(final String driverNumber) {
        this.driverNumber = driverNumber;
    }

    public String getDriverLicenceDetails() {
        return driverLicenceDetails;
    }

    public void setDriverLicenceDetails(final String driverLicenceDetails) {
        this.driverLicenceDetails = driverLicenceDetails;
    }
}
