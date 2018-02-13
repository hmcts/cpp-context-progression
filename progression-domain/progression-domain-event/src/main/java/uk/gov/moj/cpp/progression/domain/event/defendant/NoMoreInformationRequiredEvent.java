package uk.gov.moj.cpp.progression.domain.event.defendant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import uk.gov.justice.domain.annotation.Event;

import java.time.LocalDateTime;
import java.util.UUID;

@Event("progression.events.no-more-information-required")
@JsonIgnoreProperties({"caseProgressionId"})
public class NoMoreInformationRequiredEvent {
    private final UUID defendantId;
    private final UUID caseId;
    private LocalDateTime sentenceHearingReviewDecisionDateTime;

    public NoMoreInformationRequiredEvent( UUID caseId, UUID defendantId ) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.sentenceHearingReviewDecisionDateTime = LocalDateTime.now();
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public LocalDateTime getSentenceHearingReviewDecisionDateTime() {
        return sentenceHearingReviewDecisionDateTime;
    }

    public void setSentenceHearingReviewDecisionDateTime(
                    LocalDateTime sentenceHearingReviewDecisionDateTime) {
        this.sentenceHearingReviewDecisionDateTime = sentenceHearingReviewDecisionDateTime;
    }
    
}
