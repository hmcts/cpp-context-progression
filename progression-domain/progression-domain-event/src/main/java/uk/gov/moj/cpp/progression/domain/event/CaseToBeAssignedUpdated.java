package uk.gov.moj.cpp.progression.domain.event;

import java.util.UUID;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;

/**
 * @author jchondig
 *
 */
@Event("progression.events.case-to-be-assigned-updated")
public class CaseToBeAssignedUpdated {

    private UUID caseProgressionId;

    private CaseStatusEnum status;

    public CaseToBeAssignedUpdated(UUID caseProgressionId, CaseStatusEnum status) {
        super();
        this.caseProgressionId = caseProgressionId;
        this.status = status;
    }

    public CaseStatusEnum getStatus() {
        return status;
    }

    public UUID getCaseProgressionId() {
        return caseProgressionId;
    }

    public void setCaseProgressionId(UUID caseProgressionId) {
        this.caseProgressionId = caseProgressionId;
    }

    public void setStatus(CaseStatusEnum status) {
        this.status = status;
    }

    
}
