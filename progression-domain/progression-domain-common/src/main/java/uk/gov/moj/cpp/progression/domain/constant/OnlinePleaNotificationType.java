package uk.gov.moj.cpp.progression.domain.constant;

public enum OnlinePleaNotificationType {
    COMPANY_ONLINE_PLEA("CompanyOnlinePlea"),

    COMPANY_FINANCE_DATA("CompanyFinanceData"),

    INDIVIDUAL_ONLINE_PLEA("IndividualOnlinePlea"),

    INDIVIDUAL_FINANCE_DATA("IndividualFinanceData");

    private final String description;

    OnlinePleaNotificationType(final String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
