package uk.gov.moj.cpp.progression.domain.event.defendant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import uk.gov.justice.domain.annotation.Event;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

@Event("progression.events.no-more-information-required")
@JsonIgnoreProperties({"caseProgressionId"})
public class NoMoreInformationRequiredEvent {
    private final UUID defendantId;
    private final UUID caseId;
    private ZonedDateTime sentenceHearingReviewDecisionDateTime;

    public NoMoreInformationRequiredEvent( UUID caseId, UUID defendantId ) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.sentenceHearingReviewDecisionDateTime = ZonedDateTime.now();
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public ZonedDateTime getSentenceHearingReviewDecisionDateTime() {
        return sentenceHearingReviewDecisionDateTime;
    }

    public void setSentenceHearingReviewDecisionDateTime(
            ZonedDateTime sentenceHearingReviewDecisionDateTime) {
        this.sentenceHearingReviewDecisionDateTime = sentenceHearingReviewDecisionDateTime;
    }
    
}
