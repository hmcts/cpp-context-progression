package uk.gov.moj.cpp.progression.domain.event;

import java.util.List;
import java.util.UUID;

import uk.gov.justice.domain.annotation.Event;

/**
 * 
 * @author jchondig
 *
 */
@Event("progression.events.case-added-to-crown-court")
public class CaseAddedToCrownCourt {

    private UUID caseProgressionId;

    private UUID caseId;

    private String courtCentreId;

    private List<Defendant> defendants;

    public CaseAddedToCrownCourt(UUID caseProgressionId, UUID caseId, String courtCentreId,
                    List<Defendant> defendantIds) {
        super();
        this.caseProgressionId = caseProgressionId;
        this.caseId = caseId;
        this.courtCentreId = courtCentreId;
        this.defendants = defendantIds;
    }

    public List<Defendant> getDefendants() {
        return defendants;
    }

    public UUID getCaseProgressionId() {
        return caseProgressionId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getCourtCentreId() {
        return courtCentreId;
    }

    public void setCaseProgressionId(UUID caseProgressionId) {
        this.caseProgressionId = caseProgressionId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public void setCourtCentreId(String courtCentreId) {
        this.courtCentreId = courtCentreId;
    }

    public void setDefendants(List<Defendant> defendants) {
        this.defendants = defendants;
    }
    
    

}
