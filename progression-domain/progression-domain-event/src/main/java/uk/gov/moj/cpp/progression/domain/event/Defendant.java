package uk.gov.moj.cpp.progression.domain.event;

import java.util.UUID;

public class Defendant {

    private UUID id;
    private Boolean sentenceHearingReviewDecision;

    /**
     * @return the id
     */
    public UUID getId() {
        return id;
    }

    public Defendant(UUID id) {
        super();
        this.id = id;
    }

    public Defendant() {
        super();
    }

    public Boolean getSentenceHearingReviewDecision() {
        return sentenceHearingReviewDecision;
    }

    public void setSentenceHearingReviewDecision(Boolean sentenceHearingReviewDecision) {
        this.sentenceHearingReviewDecision = sentenceHearingReviewDecision;
    }

}
