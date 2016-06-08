package uk.gov.moj.cpp.progression.domain.event;

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

    public CaseAddedToCrownCourt(UUID caseProgressionId, UUID caseId, String courtCentreId) {
        super();
        this.caseProgressionId = caseProgressionId;
        this.caseId = caseId;
        this.courtCentreId = courtCentreId;
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

}
