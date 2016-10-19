package uk.gov.moj.cpp.progression.query.view.response;

import java.time.LocalDate;

public class CaseProgressionDetailView {

    private String caseProgressionId;

    private String caseId;

    private Long version;

    private LocalDate directionIssuedOn;

    private String fromCourtCentre;

    private LocalDate sendingCommittalDate;

    private LocalDate sentenceHearingDate;

    private LocalDate sentenceReviewDeadlineDate;

    private String status;

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getCaseProgressionId() {
        return caseProgressionId;
    }

    public void setCaseProgressionId(String caseProgressionId) {
        this.caseProgressionId = caseProgressionId;
    }

    public LocalDate getDirectionIssuedOn() {
        return directionIssuedOn;
    }

    public void setDirectionIssuedOn(LocalDate directionIssuedOn) {
        this.directionIssuedOn = directionIssuedOn;
    }

    public String getFromCourtCentre() {
        return fromCourtCentre;
    }

    public void setFromCourtCentre(String fromCourtCentre) {
        this.fromCourtCentre = fromCourtCentre;
    }

    public LocalDate getSendingCommittalDate() {
        return sendingCommittalDate;
    }

    public void setSendingCommittalDate(LocalDate sendingCommittalDate) {
        this.sendingCommittalDate = sendingCommittalDate;
    }

    public LocalDate getSentenceHearingDate() {
        return sentenceHearingDate;
    }

    public void setSentenceHearingDate(LocalDate sentenceHearingDate) {
        this.sentenceHearingDate = sentenceHearingDate;
    }

    public LocalDate getSentenceReviewDeadlineDate() {
        return sentenceReviewDeadlineDate;
    }

    public void setSentenceReviewDeadlineDate(LocalDate sentenceReviewDeadlineDate) {
        this.sentenceReviewDeadlineDate = sentenceReviewDeadlineDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
