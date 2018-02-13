package uk.gov.moj.cpp.progression.domain.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * @author jchondig
 *
 */
@Event("progression.events.case-ready-for-sentence-hearing")
@JsonIgnoreProperties({"caseProgressionId"})
public class CaseReadyForSentenceHearing {

    private UUID caseId;

    private CaseStatusEnum status;

    private ZonedDateTime caseStatusUpdatedDateTime;

    public CaseReadyForSentenceHearing(UUID caseId, CaseStatusEnum status,
                                       ZonedDateTime caseStatusUpdatedDateTime) {
        super();
        this.caseId = caseId;
        this.status = status;
        this.caseStatusUpdatedDateTime = caseStatusUpdatedDateTime;
    }

    public ZonedDateTime getCaseStatusUpdatedDateTime() {
        return caseStatusUpdatedDateTime;
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

    public void setCaseStatusUpdatedDateTime(ZonedDateTime caseStatusUpdatedDateTime) {
        this.caseStatusUpdatedDateTime = caseStatusUpdatedDateTime;
    }
    
}
