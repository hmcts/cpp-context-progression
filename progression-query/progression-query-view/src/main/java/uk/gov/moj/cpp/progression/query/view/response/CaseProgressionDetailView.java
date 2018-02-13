package uk.gov.moj.cpp.progression.query.view.response;

import java.time.LocalDate;
import java.util.List;

public class CaseProgressionDetailView {

    private String caseId;
    private String caseUrn;

    private String fromCourtCentre;

    private LocalDate sendingCommittalDate;

    private LocalDate sentenceHearingDate;

    private LocalDate sentenceReviewDeadlineDate;

    private String status;

    private String courtCentreId;

    private List<DefendantView> defendants;

    public List<DefendantView> getDefendants() {
        return defendants;
    }

    public void setDefendants(List<DefendantView> defendants) {
        this.defendants = defendants;
    }

    public String getCourtCentreId() {
        return courtCentreId;
    }

    public void setCourtCentreId(String courtCentreId) {
        this.courtCentreId = courtCentreId;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(final String caseId) {
        this.caseId = caseId;
    }

    public String getFromCourtCentre() {
        return fromCourtCentre;
    }

    public void setFromCourtCentre(final String fromCourtCentre) {
        this.fromCourtCentre = fromCourtCentre;
    }

    public LocalDate getSendingCommittalDate() {
        return sendingCommittalDate;
    }

    public void setSendingCommittalDate(final LocalDate sendingCommittalDate) {
        this.sendingCommittalDate = sendingCommittalDate;
    }

    public LocalDate getSentenceHearingDate() {
        return sentenceHearingDate;
    }

    public void setSentenceHearingDate(final LocalDate sentenceHearingDate) {
        this.sentenceHearingDate = sentenceHearingDate;
    }

    public LocalDate getSentenceReviewDeadlineDate() {
        return sentenceReviewDeadlineDate;
    }

    public void setSentenceReviewDeadlineDate(final LocalDate sentenceReviewDeadlineDate) {
        this.sentenceReviewDeadlineDate = sentenceReviewDeadlineDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getCaseUrn() {
        return caseUrn;
    }

    public void setCaseUrn(String caseUrn) {
        this.caseUrn = caseUrn;
    }

}
