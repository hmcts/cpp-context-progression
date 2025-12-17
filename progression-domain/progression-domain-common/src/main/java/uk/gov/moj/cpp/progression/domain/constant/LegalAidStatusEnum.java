package uk.gov.moj.cpp.progression.domain.constant;

public enum LegalAidStatusEnum {
    GRANTED("Granted"),
    REFUSED("Refused"),
    PENDING("Pending"),
    WITHDRAWN("Withdrawn"),
    NO_VALUE("NO_VALUE");
    private String description;

    LegalAidStatusEnum(final String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
