package uk.gov.moj.cpp.progression.query.view.response;

import uk.gov.moj.cpp.progression.domain.utils.LocalDateUtils;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.query.view.service.ProgressionDataConstant;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

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

    private CaseProgressionDetailView(final CaseProgressionDetail caseProgressionDetail) {
        this.setCaseId(caseProgressionDetail.getCaseId().toString());
        this.setCaseUrn(caseProgressionDetail.getCaseUrn());
        if (caseProgressionDetail.getStatus() != null) {
            this.setStatus(caseProgressionDetail.getStatus().toString());
        }
        this.setSentenceReviewDeadlineDate(calcSentenceReviewDeadlineDate(caseProgressionDetail.getSendingCommittalDate()));
        this.setFromCourtCentre(caseProgressionDetail.getFromCourtCentre());
        this.setSendingCommittalDate(caseProgressionDetail.getSendingCommittalDate());
        this.setSentenceHearingDate(caseProgressionDetail.getSentenceHearingDate());
        this.setCourtCentreId(caseProgressionDetail.getCourtCentreId());
    }

    public static CaseProgressionDetailView createCaseView(final CaseProgressionDetail caseProgressionDetail) {
        CaseProgressionDetailView caseProgressionDetailView=new CaseProgressionDetailView(caseProgressionDetail);
        List<DefendantView> defendantViews = caseProgressionDetailView.getDefendantsView(caseProgressionDetail).getDefendants();
        caseProgressionDetailView.setDefendants(defendantViews);
        return caseProgressionDetailView;
    }

    public static CaseProgressionDetailView createCaseWithoutDefendantView(final CaseProgressionDetail caseProgressionDetail) {
        CaseProgressionDetailView caseProgressionDetailView=new CaseProgressionDetailView(caseProgressionDetail);
        return caseProgressionDetailView;
    }


    private DefendantsView getDefendantsView(CaseProgressionDetail caseProgressionDetail) {
        return new DefendantsView(caseProgressionDetail.getDefendants().stream().map(DefendantView::new).collect(Collectors.toList()));
    }

    private LocalDate calcSentenceReviewDeadlineDate(
            final LocalDate sendingCommittalDate) {
        return LocalDateUtils.addWorkingDays(sendingCommittalDate,
                ProgressionDataConstant.sentenceReviewDeadlineDateDaysFromDateOfSending);
    }

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
