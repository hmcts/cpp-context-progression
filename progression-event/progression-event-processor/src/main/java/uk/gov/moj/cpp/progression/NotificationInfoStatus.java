package uk.gov.moj.cpp.progression;

public enum NotificationInfoStatus {
    PENDING("PENDING"),
    PROCESSED("PROCESSED");

    private String type;

    NotificationInfoStatus(final String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
