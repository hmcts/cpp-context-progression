package uk.gov.moj.cpp.progression.domain.event;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import uk.gov.justice.domain.annotation.Event;

@Event("progression.events.sentence-hearing-date-added")
@JsonIgnoreProperties({"caseProgressionId"})
public class SentenceHearingDateAdded {

    private UUID caseId;
    private LocalDate sentenceHearingDate;

    public SentenceHearingDateAdded(LocalDate sentenceHearingDate, UUID caseId) {

        super();
        this.sentenceHearingDate = sentenceHearingDate;
        this.caseId=caseId;
    }


    public LocalDate getSentenceHearingDate() {
        return sentenceHearingDate;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public void setSentenceHearingDate(LocalDate sentenceHearingDate) {
        this.sentenceHearingDate = sentenceHearingDate;
    }
    
    
}
