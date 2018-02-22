package uk.gov.moj.cpp.progression.domain.event.defendant;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import uk.gov.justice.domain.annotation.Event;

@Event("progression.events.defendant-additional-information-added")
@JsonIgnoreProperties({"caseProgressionId", "defendantProgressionId"})
public class DefendantAdditionalInformationAdded {

    private final UUID defendantId;
    private final UUID caseId;
    private Boolean sentenceHearingReviewDecision;
    private LocalDateTime sentenceHearingReviewDecisionDateTime;
    private final AdditionalInformationEvent additionalInformationEvent;

    public DefendantAdditionalInformationAdded(UUID defendantId, UUID caseId,
            AdditionalInformationEvent additionalInformationEvent, Boolean sentenceHearingReviewDecision,
            LocalDateTime sentenceHearingReviewDecisionDateTime) {
        this.defendantId = defendantId;
        this.additionalInformationEvent = additionalInformationEvent;
        this.caseId = caseId;
        this.sentenceHearingReviewDecision = sentenceHearingReviewDecision;
        this.sentenceHearingReviewDecisionDateTime = sentenceHearingReviewDecisionDateTime;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public AdditionalInformationEvent getAdditionalInformationEvent() {
        return additionalInformationEvent;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public LocalDateTime getSentenceHearingReviewDecisionDateTime() {
        return sentenceHearingReviewDecisionDateTime;
    }

    public Boolean getSentenceHearingReviewDecision() {
        return sentenceHearingReviewDecision;
    }

    public static final class DefendantEventBuilder {
        private UUID defendantId;
        private UUID caseId;
        private Boolean sentenceHearingReviewDecision;
        private LocalDateTime sentenceHearingReviewDecisionDateTime;
        private AdditionalInformationEvent additionalInformationEvent;

        private DefendantEventBuilder() {
        }

        public static DefendantEventBuilder aDefendantEvent() {
            return new DefendantEventBuilder();
        }

        public DefendantEventBuilder setDefendantId(UUID defendantId) {
            this.defendantId = defendantId;
            return this;
        }

        public DefendantEventBuilder setCaseId(UUID caseId) {
            this.caseId = caseId;
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
            return new DefendantAdditionalInformationAdded(defendantId, caseId,
                    additionalInformationEvent, sentenceHearingReviewDecision, sentenceHearingReviewDecisionDateTime);
        }

        public AdditionalInformationEvent getAdditionalInformationEvent() {
            return additionalInformationEvent;
        }

        public void setAdditionalInformationEvent(AdditionalInformationEvent additionalInformationEvent) {
            this.additionalInformationEvent = additionalInformationEvent;
        }

        public UUID getDefendantId() {
            return defendantId;
        }

        public UUID getCaseId() {
            return caseId;
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
