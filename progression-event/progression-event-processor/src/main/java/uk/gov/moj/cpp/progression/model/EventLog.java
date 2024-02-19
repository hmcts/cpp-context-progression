package uk.gov.moj.cpp.progression.model;

import java.util.UUID;

public class EventLog {

    private String time;
    private String label;
    private String note;
    private UUID userId;

    public String getTime() {
        return time;
    }

    public void setTime(final String time) {
        this.time = time;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public String getNote() {
        return note;
    }

    public void setNote(final String note) {
        this.note = note;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(final UUID userId) {
        this.userId = userId;
    }
}
