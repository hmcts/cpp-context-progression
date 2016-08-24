package uk.gov.moj.cpp.progression.query.view.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class DefendantView {
    private LocalDateTime sentenceHearingReviewDecisionDateTime;

    private String caseProgressionId;

    private String defendantId;

    private AdditionalInformation additionalInformation;

    private String defendantProgressionId;

    private Boolean sentenceHearingReviewDecision;

    public LocalDateTime getSentenceHearingReviewDecisionDateTime() {
        return sentenceHearingReviewDecisionDateTime;
    }

    public void setSentenceHearingReviewDecisionDateTime(
                    LocalDateTime sentenceHearingReviewDecisionDateTime) {
        this.sentenceHearingReviewDecisionDateTime = sentenceHearingReviewDecisionDateTime;
    }

    public String getCaseProgressionId() {
        return caseProgressionId;
    }

    public void setCaseProgressionId(String caseProgressionId) {
        this.caseProgressionId = caseProgressionId;
    }

    public String getDefendantId() {
        return defendantId;
    }

    public void setDefendantId(String defendantId) {
        this.defendantId = defendantId;
    }

    public AdditionalInformation getAdditionalInformation() {
        return additionalInformation;
    }

    public void setAdditionalInformation(AdditionalInformation additionalInformation) {
        this.additionalInformation = additionalInformation;
    }

    public String getDefendantProgressionId() {
        return defendantProgressionId;
    }

    public void setDefendantProgressionId(String defendantProgressionId) {
        this.defendantProgressionId = defendantProgressionId;
    }

    public Boolean getSentenceHearingReviewDecision() {
        return sentenceHearingReviewDecision;
    }

    public void setSentenceHearingReviewDecision(Boolean sentenceHearingReviewDecision) {
        this.sentenceHearingReviewDecision = sentenceHearingReviewDecision;
    }


}
