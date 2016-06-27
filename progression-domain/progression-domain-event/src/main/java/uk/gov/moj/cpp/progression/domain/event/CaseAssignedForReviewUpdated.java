package uk.gov.moj.cpp.progression.domain.event;

import java.util.UUID;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;

/**
 * @author jchondig
 *
 */
@Event("progression.events.case-assigned-for-review-updated")
public class CaseAssignedForReviewUpdated {

    private UUID caseProgressionId;

    private CaseStatusEnum status;

    public CaseAssignedForReviewUpdated(UUID caseProgressionId, CaseStatusEnum status) {
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

}
