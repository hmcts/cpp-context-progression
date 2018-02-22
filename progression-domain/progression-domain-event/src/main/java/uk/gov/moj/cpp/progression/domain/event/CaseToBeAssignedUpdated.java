package uk.gov.moj.cpp.progression.domain.event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;

/**
 * @author jchondig
 *
 */
@Event("progression.events.case-to-be-assigned-updated")
@JsonIgnoreProperties({"caseProgressionId"})
public class CaseToBeAssignedUpdated {

    private UUID caseId;

    private CaseStatusEnum status;

    public CaseToBeAssignedUpdated(UUID caseId, CaseStatusEnum status) {
        super();
        this.caseId = caseId;
        this.status = status;
    }

    public CaseStatusEnum getStatus() {
        return status;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public void setStatus(CaseStatusEnum status) {
        this.status = status;
    }

    
}
