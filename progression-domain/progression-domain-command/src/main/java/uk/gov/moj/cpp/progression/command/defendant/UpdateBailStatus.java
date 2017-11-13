package uk.gov.moj.cpp.progression.command.defendant;

import java.util.UUID;

public class UpdateBailStatus {

    private final UUID caseId;
    private final UUID suspectId;
    private final String bailStatus;

    public UpdateBailStatus(UUID caseId,
                            UUID suspectId,
                            String bailStatus) {
        this.caseId = caseId;
        this.suspectId = suspectId;
        this.bailStatus = bailStatus;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getSuspectId() {
        return suspectId;
    }

    public String getBailStatus() {
        return bailStatus;
    }
}
