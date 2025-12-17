package uk.gov.moj.cpp.progression.persistence.entity;


import java.io.Serializable;

import javax.persistence.Embeddable;
/**
 * @deprecated
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
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

    public DefendantOffenderDetails(final String year, final String organisationUnit, final String number, final String checkDigit){
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

    public void setYear(final String year) {
        this.year = year;
    }

    public void setOrganisationUnit(final String organisationUnit) {
        this.organisationUnit = organisationUnit;
    }

    public void setNumber(final String number) {
        this.number = number;
    }

    public void setCheckDigit(final String checkDigit) {
        this.checkDigit = checkDigit;
    }


}
