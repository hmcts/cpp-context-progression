package uk.gov.moj.cpp.progression.domain.event;

import uk.gov.justice.domain.annotation.Event;

import java.time.LocalDate;
import java.util.UUID;

@Event("progression.events.sentence-hearing-added")
public class SentenceHearingAdded {

    private final UUID caseProgressionId;
    private final UUID caseId;
    private final UUID sentenceHearingId;

    public SentenceHearingAdded(UUID caseProgressionId, UUID sentenceHearingId, UUID caseId) {

        super();
        this.caseProgressionId = caseProgressionId;
        this.sentenceHearingId = sentenceHearingId;
        this.caseId=caseId;
    }

    public UUID getCaseProgressionId() {
        return caseProgressionId;
    }

    public UUID getSentenceHearingId() {
        return sentenceHearingId;
    }

    public UUID getCaseId() {
        return caseId;
    }


}
