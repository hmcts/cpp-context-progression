package uk.gov.moj.cpp.progression;

public enum SourceType {
    EMAIL("Email"),
    LETTER("Letter");

    private String name;

    SourceType(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
