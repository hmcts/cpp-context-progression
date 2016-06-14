package uk.gov.moj.cpp.progression.query.view.converter;

import java.time.LocalDate;

import uk.gov.moj.cpp.progression.domain.utils.LocalDateUtils;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.query.view.response.CaseProgressionDetailView;
import uk.gov.moj.cpp.progression.query.view.service.ProgressionDataConstant;

public class CaseProgressionDetailToViewConverter {
    private long cmiSubmissionDeadlineDate;

    public CaseProgressionDetailToViewConverter() {
        super();
    }

    public CaseProgressionDetailToViewConverter(long cmiSubmissionDeadlineDate) {
        super();
        this.cmiSubmissionDeadlineDate = cmiSubmissionDeadlineDate;
    }

    public CaseProgressionDetailView convert(CaseProgressionDetail caseProgressionDetail) {
        CaseProgressionDetailView caseProgressionDetailVo = null;
        caseProgressionDetailVo = new CaseProgressionDetailView();
        caseProgressionDetailVo.setCaseProgressionId(caseProgressionDetail.getId().toString());
        caseProgressionDetailVo.setCaseId(caseProgressionDetail.getCaseId().toString());
        if (caseProgressionDetail.getStatus() != null) {
            caseProgressionDetailVo.setStatus(caseProgressionDetail.getStatus().toString());
        }
        if (caseProgressionDetail.getDateOfSending() != null) {
            caseProgressionDetailVo.setDateOfSending(caseProgressionDetail.getDateOfSending());
            caseProgressionDetailVo.setDateCMISubmissionDeadline(caseProgressionDetail
                            .getDateOfSending().plusDays(cmiSubmissionDeadlineDate));
            caseProgressionDetailVo.setNoOfDaysForCMISubmission(
                            LocalDateUtils.noOfDaysUntil(caseProgressionDetail.getDateOfSending()
                                            .plusDays(cmiSubmissionDeadlineDate)));
        }
        caseProgressionDetailVo.setSentenceReviewDeadlineDate(
                        calcSentenceReviewDeadlineDate(caseProgressionDetail));

        caseProgressionDetailVo.setDefenceIssues(caseProgressionDetail.getDefenceIssue());
        caseProgressionDetailVo.setSfrIssues(caseProgressionDetail.getSfrIssue());
        caseProgressionDetailVo
                        .setDefenceTrialEstimate(caseProgressionDetail.getTrialEstimateDefence());
        caseProgressionDetailVo.setProsecutionTrialEstimate(
                        caseProgressionDetail.getTrialEstimateProsecution());
        caseProgressionDetailVo.setIsAllStatementsIdentified(
                        caseProgressionDetail.getIsAllStatementsIdentified());
        caseProgressionDetailVo.setVersion(caseProgressionDetail.getVersion().toString());
        caseProgressionDetailVo
                        .setIsAllStatementsServed(caseProgressionDetail.getIsAllStatementsServed());
        caseProgressionDetailVo.setDirectionIssuedOn(caseProgressionDetail.getDirectionIssuedOn());
        if (caseProgressionDetail.getPtpHearingVacatedDate() != null) {
            caseProgressionDetailVo.setPtpHearingVacatedDate(
                            caseProgressionDetail.getPtpHearingVacatedDate());
        }
        caseProgressionDetailVo.setFromCourtCentre(caseProgressionDetail.getFromCourtCentre());
        caseProgressionDetailVo
                        .setSendingCommittalDate(caseProgressionDetail.getSendingCommittalDate());
        caseProgressionDetailVo.setIsPSROrdered(caseProgressionDetail.getIsPSROrdered());
        caseProgressionDetailVo
                        .setSentenceHearingDate(caseProgressionDetail.getSentenceHearingDate());

        return caseProgressionDetailVo;
    }

    private LocalDate calcSentenceReviewDeadlineDate(CaseProgressionDetail caseProgressionDetail) {
        LocalDate basedOnDate = null;
        if (caseProgressionDetail.getDateOfSending() != null) {
            basedOnDate = caseProgressionDetail.getDateOfSending();
        } else if (caseProgressionDetail.getSendingCommittalDate() != null) {
            basedOnDate = caseProgressionDetail.getSendingCommittalDate();
        }
        return LocalDateUtils.addWorkingDays(basedOnDate,
                        ProgressionDataConstant.sentenceReviewDeadlineDateDaysFromDateOfSending);
    }
}
