package uk.gov.moj.cpp.progression;

public enum CommunicationType {
    EMAIL("Email"),
    LETTER("Letter");

    private String type;

    CommunicationType(final String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
