package uk.gov.moj.cpp.progression.domain.pojo;

public enum Status {

    PENDING("Pending"),
    OVERDUE("Overdue"),
    COMPLETED("Completed"),
    RESCINDED("Rescinded"),
    NOT_APPLICABLE("Not applicable"),
    BREACHED("Breached"),
    SUPERSEDED("Superseded");

    private String cpsStatus;

    Status(final String status) {
        this.cpsStatus = status;
    }

    public String getCpsStatus() {
        return cpsStatus;
    }
}
