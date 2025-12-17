package uk.gov.moj.cpp.progression.domain.event.completedsendingsheet;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;
/**
 * 
 * @deprecated
 *
 */
@Deprecated
public class Defendant implements Serializable {

    private static final long serialVersionUID = 1965056227573772587L;

    private UUID id;
    private UUID personId;
    private String title;
    private String firstName;
    private String lastName;
    private String nationality;
    private String gender;
    private Address address;
    private String dateOfBirth;
    private String bailStatus;
    private String custodyTimeLimitDate;
    private String defenceOrganisation;
    private Interpreter interpreter;
    private List<Offence> offences;

    public UUID getId() {
        return this.id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public UUID getPersonId() {
        return this.personId;
    }

    public void setPersonId(final UUID personId) {
        this.personId = personId;
    }

    public String getFirstName() {
        return this.firstName;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return this.lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    public String getNationality() {
        return this.nationality;
    }

    public void setNationality(final String nationality) {
        this.nationality = nationality;
    }

    public String getGender() {
        return this.gender;
    }

    public void setGender(final String gender) {
        this.gender = gender;
    }

    public Address getAddress() {
        return this.address;
    }

    public void setAddress(final Address address) {
        this.address = address;
    }

    public String getDateOfBirth() {
        return this.dateOfBirth;
    }

    public void setDateOfBirth(final String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getBailStatus() {
        return this.bailStatus;
    }

    public void setBailStatus(final String bailStatus) {
        this.bailStatus = bailStatus;
    }

    public String getCustodyTimeLimitDate() {
        return custodyTimeLimitDate;
    }

    public void setCustodyTimeLimitDate(final String custodyTimeLimitDate) {
        this.custodyTimeLimitDate = custodyTimeLimitDate;
    }

    public String getDefenceOrganisation() {
        return this.defenceOrganisation;
    }

    public void setDefenceOrganisation(final String defenceOrganisation) {
        this.defenceOrganisation = defenceOrganisation;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public Interpreter getInterpreter() {
        return this.interpreter;
    }

    public void setInterpreter(final Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    public List<Offence> getOffences() {
        return this.offences;
    }

    public void setOffences(final List<Offence> offences) {
        this.offences = offences;
    }

}