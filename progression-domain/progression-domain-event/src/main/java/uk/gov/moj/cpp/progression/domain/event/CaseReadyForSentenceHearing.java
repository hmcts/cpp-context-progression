package uk.gov.moj.cpp.progression.domain.event;

import java.time.LocalDate;
import java.util.UUID;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;

/**
 * @author jchondig
 *
 */
@Event("progression.events.case-ready-for-sentence-hearing")
public class CaseReadyForSentenceHearing {

    private UUID caseProgressionId;

    private CaseStatusEnum status;

    private LocalDate readyForSentenceHearingDate;

    public CaseReadyForSentenceHearing(UUID caseProgressionId, CaseStatusEnum status,
                    LocalDate readyForSentenceHearingDate) {
        super();
        this.caseProgressionId = caseProgressionId;
        this.status = status;
        this.readyForSentenceHearingDate = readyForSentenceHearingDate;
    }

    public LocalDate getReadyForSentenceHearingDate() {
        return readyForSentenceHearingDate;
    }

    public CaseStatusEnum getStatus() {
        return status;
    }

    public UUID getCaseProgressionId() {
        return caseProgressionId;
    }

}
