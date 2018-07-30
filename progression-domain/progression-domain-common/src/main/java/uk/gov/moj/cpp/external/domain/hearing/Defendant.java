package uk.gov.moj.cpp.external.domain.hearing;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;


@JsonInclude(NON_NULL)
public final class Defendant implements Serializable {

    private static final long serialVersionUID = 1965056227573772587L;

    private UUID id;
    private UUID personId;
    private String firstName;
    private String lastName;
    private String nationality;
    private String gender;
    private Address address;
    private String dateOfBirth;
    private String defenceOrganisation;
    private Interpreter interpreter;
    private List<Offence> offences;
    private List<DefendantCases> defendantCases;

    public Defendant(){
    }

    public Defendant(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return this.id;
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


    public String getDefenceOrganisation() {
        return this.defenceOrganisation;
    }

    public void setDefenceOrganisation(final String defenceOrganisation) {
        this.defenceOrganisation = defenceOrganisation;
    }

    public Interpreter getInterpreter() {
        return this.interpreter;
    }

    public void setInterpreter(final Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    public List<Offence> getOffences() {
        if(offences == null){
            offences = new ArrayList<Offence>();
        }
        return this.offences;
    }

    public void setOffences(final List<Offence> offences) {
        this.offences = offences;
    }

    public List<DefendantCases> getDefendantCases() {
        if(defendantCases == null){
            defendantCases = new ArrayList<DefendantCases>();
        }
        return defendantCases;
    }

    public void setDefendantCases(List<DefendantCases> defendantCases) {
        this.defendantCases = defendantCases;
    }

}