package uk.gov.moj.cpp.progression.domain.event.defendant;

import java.time.LocalDateTime;
import java.util.UUID;

import uk.gov.justice.domain.annotation.Event;

@Event("progression.events.defendant-additional-information-added")
public class DefendantAdditionalInformationAdded {
    private final UUID defendantProgressionId;
    private final UUID defendantId;
    private final UUID caseProgressionId;
    private Boolean sentenceHearingReviewDecision;
    private LocalDateTime sentenceHearingReviewDecisionDateTime;
    private final AdditionalInformationEvent additionalInformationEvent;

    public DefendantAdditionalInformationAdded(UUID defendantProgressionId, UUID defendantId, UUID caseProgressionId,
            AdditionalInformationEvent additionalInformationEvent, Boolean sentenceHearingReviewDecision,
            LocalDateTime sentenceHearingReviewDecisionDateTime) {
        this.defendantProgressionId = defendantProgressionId;
        this.defendantId = defendantId;
        this.additionalInformationEvent = additionalInformationEvent;
        this.caseProgressionId = caseProgressionId;
        this.sentenceHearingReviewDecision = sentenceHearingReviewDecision;
        this.sentenceHearingReviewDecisionDateTime = sentenceHearingReviewDecisionDateTime;
    }

    public UUID getDefendantProgressionId() {
        return defendantProgressionId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public AdditionalInformationEvent getAdditionalInformationEvent() {
        return additionalInformationEvent;
    }

    public UUID getCaseProgressionId() {
        return caseProgressionId;
    }

    public LocalDateTime getSentenceHearingReviewDecisionDateTime() {
        return sentenceHearingReviewDecisionDateTime;
    }

    public Boolean getSentenceHearingReviewDecision() {
        return sentenceHearingReviewDecision;
    }

    public static final class DefendantEventBuilder {
        private UUID defendantProgressionId;
        private UUID defendantId;
        private UUID caseProgressionId;
        private Boolean sentenceHearingReviewDecision;
        private LocalDateTime sentenceHearingReviewDecisionDateTime;
        private AdditionalInformationEvent additionalInformationEvent;

        private DefendantEventBuilder() {
        }

        public static DefendantEventBuilder aDefendantEvent() {
            return new DefendantEventBuilder();
        }

        public DefendantEventBuilder setDefendantProgressionId(UUID defendantProgressionId) {
            this.defendantProgressionId = defendantProgressionId;
            return this;
        }

        public DefendantEventBuilder setDefendantId(UUID defendantId) {
            this.defendantId = defendantId;
            return this;
        }

        public DefendantEventBuilder setCaseProgressionId(UUID caseProgressionId) {
            this.caseProgressionId = caseProgressionId;
            return this;
        }

        public DefendantEventBuilder setSentenceHearingReviewDecision(Boolean sentenceHearingReviewDecision) {
            this.sentenceHearingReviewDecision = sentenceHearingReviewDecision;
            return this;
        }

        public DefendantEventBuilder setSentenceHearingReviewDecisionDateTime(
                LocalDateTime sentenceHearingReviewDecisionDateTime) {
            this.sentenceHearingReviewDecisionDateTime = sentenceHearingReviewDecisionDateTime;
            return this;
        }
        public DefendantEventBuilder setAdditionalInformation(AdditionalInformationEvent additionalInformationEvent) {
            this.additionalInformationEvent = additionalInformationEvent;
            return this;
        }

        public DefendantAdditionalInformationAdded build() {
            return new DefendantAdditionalInformationAdded(defendantProgressionId, defendantId, caseProgressionId,
                    additionalInformationEvent, sentenceHearingReviewDecision, sentenceHearingReviewDecisionDateTime);
        }

        public AdditionalInformationEvent getAdditionalInformationEvent() {
            return additionalInformationEvent;
        }

        public void setAdditionalInformationEvent(AdditionalInformationEvent additionalInformationEvent) {
            this.additionalInformationEvent = additionalInformationEvent;
        }

        public UUID getDefendantProgressionId() {
            return defendantProgressionId;
        }

        public UUID getDefendantId() {
            return defendantId;
        }

        public UUID getCaseProgressionId() {
            return caseProgressionId;
        }

        public Boolean getSentenceHearingReviewDecision() {
            return sentenceHearingReviewDecision;
        }

        public LocalDateTime getSentenceHearingReviewDecisionDateTime() {
            return sentenceHearingReviewDecisionDateTime;
        }
        
        
    }

    public void setSentenceHearingReviewDecision(Boolean sentenceHearingReviewDecision) {
        this.sentenceHearingReviewDecision = sentenceHearingReviewDecision;
    }

    public void setSentenceHearingReviewDecisionDateTime(
                    LocalDateTime sentenceHearingReviewDecisionDateTime) {
        this.sentenceHearingReviewDecisionDateTime = sentenceHearingReviewDecisionDateTime;
    }
    
    
}
