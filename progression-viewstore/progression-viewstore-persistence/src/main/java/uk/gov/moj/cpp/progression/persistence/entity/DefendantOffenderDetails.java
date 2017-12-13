package uk.gov.moj.cpp.progression.persistence.entity;


import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class DefendantOffenderDetails implements Serializable {

    private static final long serialVersionUID = 1183759478277375193L;
    private String year;
    private String organisationUnit;
    private String number;
    private String checkDigit;

    public DefendantOffenderDetails() {
        super();
    }

    public DefendantOffenderDetails(String year, String organisationUnit, String number, String checkDigit){
        this.year = year;
        this.organisationUnit = organisationUnit;
        this.number = number;
        this.checkDigit = checkDigit;
    }

    public String getYear() {
        return year;
    }

    public String getOrganisationUnit() {
        return organisationUnit;
    }

    public String getNumber() {
        return number;
    }

    public String getCheckDigit() {
        return checkDigit;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public void setOrganisationUnit(String organisationUnit) {
        this.organisationUnit = organisationUnit;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public void setCheckDigit(String checkDigit) {
        this.checkDigit = checkDigit;
    }


}
