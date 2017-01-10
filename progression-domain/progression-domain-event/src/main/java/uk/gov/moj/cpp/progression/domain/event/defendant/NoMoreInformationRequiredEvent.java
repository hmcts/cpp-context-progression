package uk.gov.moj.cpp.progression.domain.event.defendant;

import uk.gov.justice.domain.annotation.Event;

import java.time.LocalDateTime;
import java.util.UUID;

@Event("progression.events.no-more-information-required")
public class NoMoreInformationRequiredEvent {
    private final UUID defendantId;
    private final UUID caseId;
    private final UUID caseProgressionId;
    private LocalDateTime sentenceHearingReviewDecisionDateTime;

    public NoMoreInformationRequiredEvent( UUID caseId, UUID defendantId, UUID caseProgressionId ) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.caseProgressionId =caseProgressionId;
        this.sentenceHearingReviewDecisionDateTime = LocalDateTime.now();
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public UUID getCaseProgressionId() {
        return caseProgressionId;
    }

    public LocalDateTime getSentenceHearingReviewDecisionDateTime() {
        return sentenceHearingReviewDecisionDateTime;
    }
}
