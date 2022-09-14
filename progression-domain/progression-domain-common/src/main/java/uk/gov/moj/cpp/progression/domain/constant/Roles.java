package uk.gov.moj.cpp.progression.domain.constant;

public enum Roles {

    OPERATIONAL_DELIVERY_ADMIN("Operational Delivery Admin"),
    LISTING_OFFICER("Listing Officer"),
    CASE_PROGRESSION_OFFICER("Case Progression officer"),
    CTSC_ADMIN("CTSC Admin");

    private String role;

    Roles(final String role) {
        this.role = role;
    }
}
