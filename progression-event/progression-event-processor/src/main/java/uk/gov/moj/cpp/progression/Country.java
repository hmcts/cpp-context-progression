package uk.gov.moj.cpp.progression;

public enum Country {
    WALES("Wales"),
    ENGLAND("England");

    private String name;

    Country(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
