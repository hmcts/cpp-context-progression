package uk.gov.moj.cpp.progression.domain.event;

import uk.gov.justice.domain.annotation.Event;

import java.time.LocalDate;
import java.util.UUID;

@Event("progression.events.sentence-hearing-date-updated")
public class SentenceHearingDateUpdated {

    private UUID caseProgressionId;
    private UUID caseId;
    private LocalDate sentenceHearingDate;

    public SentenceHearingDateUpdated(UUID caseProgressionId, LocalDate sentenceHearingDate, UUID caseId) {

        super();
        this.caseProgressionId = caseProgressionId;
        this.sentenceHearingDate = sentenceHearingDate;
        this.caseId=caseId;
    }

    public UUID getCaseProgressionId() {
        return caseProgressionId;
    }

    public LocalDate getSentenceHearingDate() {
        return sentenceHearingDate;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseProgressionId(UUID caseProgressionId) {
        this.caseProgressionId = caseProgressionId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public void setSentenceHearingDate(LocalDate sentenceHearingDate) {
        this.sentenceHearingDate = sentenceHearingDate;
    }
    
    
}
