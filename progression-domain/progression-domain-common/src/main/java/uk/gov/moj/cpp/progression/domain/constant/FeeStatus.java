package uk.gov.moj.cpp.progression.domain.constant;

public enum FeeStatus {

    OUTSTANDING("Outstanding"),
    SATISFIED("Satisfied"),
    WAIVED("Waived"),
    REFUNDED("Refunded"),
    REDUCED("Reduced"),
    NOT_APPLICABLE("Not applicable"),
    PAYMENT_UNDERTAKING_MADE("Payment undertaking made");

    private String description;

    FeeStatus(final String description) {
        this.description = description;
    }
}
