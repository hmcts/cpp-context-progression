package uk.gov.moj.cpp.progression;

public enum RecipientType {
    DEFENDANT("Defendant"),
    DEFENCE("Defence"),
    PROSECUTOR("Prosecutor");

    private final String recipientName;

    RecipientType(final String name) {
        this.recipientName = name;
    }

    public String getRecipientName() {
        return recipientName;
    }
}
