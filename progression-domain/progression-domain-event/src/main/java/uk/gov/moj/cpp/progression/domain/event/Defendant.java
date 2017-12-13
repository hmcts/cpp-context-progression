package uk.gov.moj.cpp.progression.domain.event;

import java.io.Serializable;
import java.util.UUID;

public class Defendant implements Serializable {

    private UUID id;
    private Boolean sentenceHearingReviewDecision;
    private Boolean isAdditionalInfoAvilable;

    public Defendant() {
        super();
    }

    public Defendant(UUID id) {
        super();
        this.id = id;
    }
    
    /**
     * @return the id
     */
    public UUID getId() {
        return id;
    }

    public Boolean getSentenceHearingReviewDecision() {
        return sentenceHearingReviewDecision;
    }

    public void setSentenceHearingReviewDecision(Boolean sentenceHearingReviewDecision) {
        this.sentenceHearingReviewDecision = sentenceHearingReviewDecision;
    }

    public Boolean getIsAdditionalInfoAvilable() {
        return isAdditionalInfoAvilable;
    }

    public void setIsAdditionalInfoAvilable(Boolean isAdditionalInfoAvilable) {
        this.isAdditionalInfoAvilable = isAdditionalInfoAvilable;
    }

    public void setId(UUID id) {
        this.id = id;
    }

}
