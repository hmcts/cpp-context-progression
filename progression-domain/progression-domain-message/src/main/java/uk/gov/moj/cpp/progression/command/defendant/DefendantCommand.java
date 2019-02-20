package uk.gov.moj.cpp.progression.command.defendant;

import java.util.UUID;
/**
 * 
 * @deprecated
 *
 */
@Deprecated
public class DefendantCommand {
    private UUID defendantId;
    private UUID caseId;

    public UUID getDefendantId() {
        return defendantId;
    }

    public void setDefendantId(final UUID defendantId) {
        this.defendantId = defendantId;
    }


    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }



}
