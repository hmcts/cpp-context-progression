package uk.gov.moj.cpp.progression.query.view.converter;

import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
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
		caseProgressionDetailVo.setId(caseProgressionDetail.getId().toString());
		caseProgressionDetailVo.setCaseId(caseProgressionDetail.getCaseId().toString());
		if(caseProgressionDetail.getStatus() != null){
		    caseProgressionDetailVo.setStatus(caseProgressionDetail.getStatus().toString());
		}
		if (caseProgressionDetail.getDateOfSending() != null) {
			caseProgressionDetailVo.setDateOfSending(caseProgressionDetail.getDateOfSending());
			caseProgressionDetailVo.setDateCMISubmissionDeadline(
					caseProgressionDetail.getDateOfSending().plusDays(cmiSubmissionDeadlineDate));
			caseProgressionDetailVo.setNoOfDaysForCMISubmission(LocalDateUtils
					.noOfDaysUntil(caseProgressionDetail.getDateOfSending().plusDays(cmiSubmissionDeadlineDate)));
			if(CaseStatusEnum.READY_FOR_REVIEW.equals(caseProgressionDetail.getStatus())){
	            caseProgressionDetailVo.setSentenceReviewDeadlineDate(LocalDateUtils.addWorkingDays(caseProgressionDetail.getDateOfSending(),ProgressionDataConstant.sentenceReviewDeadlineDateDaysFromDateOfSending)); 
	        }
		}
		caseProgressionDetailVo.setDefenceIssues(caseProgressionDetail.getDefenceIssue());
		caseProgressionDetailVo.setSfrIssues(caseProgressionDetail.getSfrIssue());
		caseProgressionDetailVo.setDefenceTrialEstimate(caseProgressionDetail.getTrialEstimateDefence());
		caseProgressionDetailVo.setProsecutionTrialEstimate(caseProgressionDetail.getTrialEstimateProsecution());
		caseProgressionDetailVo.setIsAllStatementsIdentified(caseProgressionDetail.getIsAllStatementsIdentified());
		caseProgressionDetailVo.setVersion(caseProgressionDetail.getVersion());
		caseProgressionDetailVo.setIsAllStatementsServed(caseProgressionDetail.getIsAllStatementsServed());
		caseProgressionDetailVo.setDirectionIssuedOn(caseProgressionDetail.getDirectionIssuedOn());
		if (caseProgressionDetail.getPtpHearingVacatedDate() != null) {
			caseProgressionDetailVo
					.setPtpHearingVacatedDate(caseProgressionDetail.getPtpHearingVacatedDate());
		}
		caseProgressionDetailVo.setFromCourtCentre(caseProgressionDetail.getFromCourtCentre());
		caseProgressionDetailVo.setSendingCommittalDate(caseProgressionDetail.getSendingCommittalDate());
		caseProgressionDetailVo.setIsPSROrdered(caseProgressionDetail.getIsPSROrdered());
		caseProgressionDetailVo.setSentenceHearingDate(caseProgressionDetail.getSentenceHearingDate());

		return caseProgressionDetailVo;
	}
}
