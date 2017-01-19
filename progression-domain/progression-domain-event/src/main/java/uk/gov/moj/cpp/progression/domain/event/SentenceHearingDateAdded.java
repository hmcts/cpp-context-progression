package uk.gov.moj.cpp.progression.domain.event;

import java.time.LocalDate;
import java.util.UUID;

import uk.gov.justice.domain.annotation.Event;

@Event("progression.events.sentence-hearing-date-added")
public class SentenceHearingDateAdded {

    private UUID caseProgressionId;
    private UUID caseId;
    private LocalDate sentenceHearingDate;

    public SentenceHearingDateAdded(UUID caseProgressionId, LocalDate sentenceHearingDate, UUID caseId) {

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
}
