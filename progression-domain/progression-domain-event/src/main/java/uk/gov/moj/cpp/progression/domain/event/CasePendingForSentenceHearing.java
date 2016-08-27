package uk.gov.moj.cpp.progression.domain.event;

import java.util.UUID;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;

/**
 * @author jchondig
 *
 */
@Event("progression.events.case-pending-for-sentence-hearing")
public class CasePendingForSentenceHearing {

    private UUID caseProgressionId;

    private CaseStatusEnum status;

    public CasePendingForSentenceHearing(UUID caseProgressionId, CaseStatusEnum status) {
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
