package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import static java.util.Optional.ofNullable;

import uk.gov.justice.core.courts.Defendant;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
public class OnlinePleaLegalEntityDetails {
    @Column(name = "legal_entity_name")
    private String name;

    @AttributeOverrides({
            @AttributeOverride(name = "address1", column = @Column(name = "legal_entity_address1")),
            @AttributeOverride(name = "address2", column = @Column(name = "legal_entity_address2")),
            @AttributeOverride(name = "address3", column = @Column(name = "legal_entity_address3")),
            @AttributeOverride(name = "address4", column = @Column(name = "legal_entity_address4")),
            @AttributeOverride(name = "address5", column = @Column(name = "legal_entity_address5")),
            @AttributeOverride(name = "postcode", column = @Column(name = "legal_entity_postcode"))
    })
    private Address address;

    @Column(name = "legal_entity_telephone_number_home")
    private String homeTelephone;
    @Column(name = "legal_entity_telephone_number_work")
    private String workTelephone;
    @Column(name = "legal_entity_telephone_number_mobile")
    private String mobile;
    @Column(name = "legal_entity_email")
    private String email;
    @Column(name = "legal_entity_incorporation_number")
    private String incorporationNumber;
    @Column(name = "legal_entity_position")
    private String position;

    public OnlinePleaLegalEntityDetails() {
    }

    public OnlinePleaLegalEntityDetails(final Defendant defendantDetail) {
        ofNullable(defendantDetail.getLegalEntityDefendant()).ifPresent(legalEntityDefendant -> ofNullable(legalEntityDefendant.getOrganisation()).ifPresent(organisation -> {
            this.name = organisation.getName();
            this.address = convertToEntity(organisation.getAddress());
            ofNullable(organisation.getContact()).ifPresent(contactDetails -> {
                this.homeTelephone = contactDetails.getHome();
                this.mobile = contactDetails.getMobile();
                this.email = contactDetails.getPrimaryEmail();
                this.workTelephone = contactDetails.getWork();
            });
            this.incorporationNumber = organisation.getIncorporationNumber();
        }));
    }

    private Address convertToEntity(final uk.gov.justice.core.courts.Address address) {
        return new Address(address.getAddress1(), address.getAddress2(), address.getAddress3(), address.getAddress4(), address.getAddress5(), address.getPostcode());
    }

    public String getName() {
        return name;
    }

    public void setName(String firstName) {
        this.name = firstName;
    }

    public String getWorkTelephone() {
        return workTelephone;
    }

    public void setWorkTelephone(String workTelephone) {
        this.workTelephone = workTelephone;
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

    public String getIncorporationNumber() {
        return incorporationNumber;
    }

    public void setIncorporationNumber(String nationalInsuranceNumber) {
        this.incorporationNumber = nationalInsuranceNumber;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(final String driverNumber) {
        this.position = driverNumber;
    }

}
