package uk.gov.moj.cpp.progression.domain.event.defendant;

import java.time.LocalDateTime;
import java.util.UUID;

import uk.gov.justice.domain.annotation.Event;

@Event("progression.events.additional-information-added")
public class DefendantEvent {
    private final UUID defendantProgressionId;
    private final UUID defendantId;
    private final UUID caseProgressionId;
    private Boolean sentenceHearingReviewDecision;
    private LocalDateTime sentenceHearingReviewDecisionDateTime;
    private final AdditionalInformationEvent additionalInformationEvent;

    public DefendantEvent(UUID defendantProgressionId, UUID defendantId, UUID caseProgressionId,
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

        public DefendantEventBuilder defendantProgressionId(UUID defendantProgressionId) {
            this.defendantProgressionId = defendantProgressionId;
            return this;
        }

        public DefendantEventBuilder defendantId(UUID defendantId) {
            this.defendantId = defendantId;
            return this;
        }

        public DefendantEventBuilder caseProgressionId(UUID caseProgressionId) {
            this.caseProgressionId = caseProgressionId;
            return this;
        }

        public DefendantEventBuilder sentenceHearingReviewDecision(Boolean sentenceHearingReviewDecision) {
            this.sentenceHearingReviewDecision = sentenceHearingReviewDecision;
            return this;
        }

        public DefendantEventBuilder sentenceHearingReviewDecisionDateTime(
                LocalDateTime sentenceHearingReviewDecisionDateTime) {
            this.sentenceHearingReviewDecisionDateTime = sentenceHearingReviewDecisionDateTime;
            return this;
        }
        public DefendantEventBuilder additionalInformation(AdditionalInformationEvent additionalInformationEvent) {
            this.additionalInformationEvent = additionalInformationEvent;
            return this;
        }

        public DefendantEvent build() {
            DefendantEvent defendantEvent = new DefendantEvent(defendantProgressionId, defendantId, caseProgressionId,
                    additionalInformationEvent, sentenceHearingReviewDecision, sentenceHearingReviewDecisionDateTime);
            return defendantEvent;
        }
    }
}
