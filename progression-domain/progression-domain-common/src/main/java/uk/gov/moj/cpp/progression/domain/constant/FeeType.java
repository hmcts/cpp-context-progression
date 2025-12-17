package uk.gov.moj.cpp.progression.domain.constant;

public enum FeeType {

    INITIAL("Initial"),
    CONTESTED("Contested");

    private String description;

    FeeType(final String description) {
        this.description = description;
    }
}
