package uk.gov.moj.cpp.progression.domain.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * @author jchondig
 *
 */
@Event("progression.events.case-pending-for-sentence-hearing")
public class CasePendingForSentenceHearing {

    private UUID caseProgressionId;

    private CaseStatusEnum status;

    private ZonedDateTime caseStatusUpdatedDateTime;

    public CasePendingForSentenceHearing(UUID caseProgressionId, CaseStatusEnum status, ZonedDateTime caseStatusUpdatedDateTime) {
        super();
        this.caseProgressionId = caseProgressionId;
        this.status = status;
        this.caseStatusUpdatedDateTime = caseStatusUpdatedDateTime;
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

    public ZonedDateTime getCaseStatusUpdatedDateTime() {
        return caseStatusUpdatedDateTime;
    }

    public void setCaseStatusUpdatedDateTime(ZonedDateTime caseStatusUpdatedDateTime) {
        this.caseStatusUpdatedDateTime = caseStatusUpdatedDateTime;
    }
}
