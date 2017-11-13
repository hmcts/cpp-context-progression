package uk.gov.moj.cpp.progression.domain.event.defendant;

import java.io.Serializable;


public final class DefendantOffenderDomain implements Serializable {

    private static final long serialVersionUID = 1183759478277375193L;
    private final String year;
    private final String organisationUnit;
    private final String number;
    private final String checkDigit;

    public DefendantOffenderDomain(String year,
                                   String organisationUnit,
                                   String number,
                                   String checkDigit){
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
}
