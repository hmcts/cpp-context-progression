package uk.gov.moj.cpp.progression.command.defendant;

import java.util.UUID;

public class DefendantCommand {
    private UUID defendantId;
    private UUID caseId;
    private AdditionalInformationCommand additionalInformation;

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
    
    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    @Override
    public String toString() {
        return "DefendantCommand{ defendantId=" + defendantId
                + ", additionalInformation=" + additionalInformation + '}';
    }

}
