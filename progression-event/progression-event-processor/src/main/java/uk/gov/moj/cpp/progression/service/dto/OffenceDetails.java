package uk.gov.moj.cpp.progression.service.dto;

public class OffenceDetails {

    private String offenceTitle;
    private String offenceLegislation;

    public String getOffenceTitle() {
        return offenceTitle;
    }

    public String getOffenceLegislation() {
        return offenceLegislation;
    }

    public void setOffenceTitle(String value) {
        this.offenceTitle = value;
    }

    public void setOffenceLegislation(String value) {
        this.offenceLegislation = value;
    }
}
