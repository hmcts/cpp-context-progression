package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

public enum OnlinePleaOutgoingOption {
    ACCOMMODATION("accommodation"),
    COUNCIL_TAX("council tax"),
    HOUSEHOLD_BILLS("household bills"),
    TRAVEL_EXPENSES("travel expenses"),
    CHILD_MAINTENANCE("child maintenance");

    private final String description;

    OnlinePleaOutgoingOption(final String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}