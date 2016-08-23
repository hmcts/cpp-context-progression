package uk.gov.moj.cpp.progression.command.defendant;

import java.util.UUID;

public class DefendantCommand {
    private UUID defendantProgressionId;
    private UUID caseProgressionId;
    private UUID defendantId;
    private AdditionalInformationCommand additionalInformation;

    public UUID getDefendantProgressionId() {
        return defendantProgressionId;
    }

    public void setDefendantProgressionId(UUID defendantProgressionId) {
        this.defendantProgressionId = defendantProgressionId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public void setDefendantId(UUID defendantId) {
        this.defendantId = defendantId;
    }

    public AdditionalInformationCommand getAdditionalInformation() {
        return additionalInformation;
    }

    public void setAdditionalInformation(AdditionalInformationCommand additionalInformation) {
        this.additionalInformation = additionalInformation;
    }

    public UUID getCaseProgressionId() {
        return caseProgressionId;
    }

    public void setCaseProgressionId(UUID caseProgressionId) {
        this.caseProgressionId = caseProgressionId;
    }

    @Override
    public String toString() {
        return "DefendantCommand{" + "defendantProgressionId=" + defendantProgressionId + ", defendantId=" + defendantId
                + ", additionalInformation=" + additionalInformation + '}';
    }
}
