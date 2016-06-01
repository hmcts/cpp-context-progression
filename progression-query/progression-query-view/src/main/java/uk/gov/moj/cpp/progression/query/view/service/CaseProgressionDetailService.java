package uk.gov.moj.cpp.progression.query.view.service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

import uk.gov.moj.cpp.progression.domain.constant.TimeLineDateType;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.TimeLineDate;
import uk.gov.moj.progression.persistence.repository.CaseProgressionDetailRepository;

public class CaseProgressionDetailService {

	@Inject
	private CaseProgressionDetailRepository caseProgressionDetailRepo;

	@Transactional
	public Optional<CaseProgressionDetail> getCaseProgressionDetail(UUID caseId) {
		CaseProgressionDetail caseProgressionDetail = caseProgressionDetailRepo.findByCaseId(caseId);
		if (caseProgressionDetail == null) {
			return Optional.empty();
		}
		caseProgressionDetail.setTimeLine(this.getTimeline(caseProgressionDetail));
		return Optional.of(caseProgressionDetail);

	}

	private List<TimeLineDate> getTimeline(CaseProgressionDetail cpd) {

		LocalDate dateNow = LocalDate.now();

		TimeLineDate dateOfSending = new TimeLineDate(TimeLineDateType.dateOfSending, cpd.getDateOfSending(), dateNow,
				0);

		TimeLineDate cmiSubmissionDeadline = new TimeLineDate(TimeLineDateType.cmiSubmissionDeadline,
				cpd.getDateOfSending(), dateNow,
				ProgressionDataConstant.cmiSubmissionDeadlineDateDaysFromDateOfSending);

		TimeLineDate serviceOfProsecutionCaseForBailCases = new TimeLineDate(
				TimeLineDateType.serviceOfProsecutionCaseForBailCases, cpd.getDateOfSending(), dateNow,
				ProgressionDataConstant.serviceOfProsecutionCaseForBailCasesDaysFromDateOfSending);

		TimeLineDate defenceCaseStatement = new TimeLineDate(TimeLineDateType.defenceCaseStatement,
				cpd.getDateOfSending(), dateNow, ProgressionDataConstant.defenceCaseStatementDaysFromDateOfSending);

		TimeLineDate kpiDateForCommencementOfTrial = new TimeLineDate(TimeLineDateType.kpiDateForCommencementOfTrial,
				cpd.getDateOfSending(), dateNow,
				ProgressionDataConstant.kpiDateForCommencementOfTrialDaysFromDateOfSending);

		return Arrays.asList(dateOfSending, cmiSubmissionDeadline, serviceOfProsecutionCaseForBailCases,
				defenceCaseStatement, kpiDateForCommencementOfTrial);
	}

}
