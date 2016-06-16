package uk.gov.moj.cpp.progression.query.view.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.domain.constant.TimeLineDateType;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.TimeLineDate;
import uk.gov.moj.progression.persistence.repository.CaseProgressionDetailRepository;

public class CaseProgressionDetailService {

    @Inject
    private CaseProgressionDetailRepository caseProgressionDetailRepo;

    @Transactional
    public CaseProgressionDetail getCaseProgressionDetail(UUID caseId) {
        final CaseProgressionDetail caseProgressionDetail = caseProgressionDetailRepo.findByCaseId(caseId);
        caseProgressionDetail.setTimeLine(this.getTimeline(caseProgressionDetail));

        return caseProgressionDetail;

    }

    private List<TimeLineDate> getTimeline(CaseProgressionDetail cpd) {

        if (cpd.getDateOfSending() == null) {
            return Collections.emptyList();
        }
        final LocalDate dateNow = LocalDate.now();

        final TimeLineDate dateOfSending = new TimeLineDate(TimeLineDateType.dateOfSending, cpd.getDateOfSending(), dateNow,
                        0);

        final TimeLineDate cmiSubmissionDeadline = new TimeLineDate(TimeLineDateType.cmiSubmissionDeadline,
                        cpd.getDateOfSending(), dateNow,
                        ProgressionDataConstant.cmiSubmissionDeadlineDateDaysFromDateOfSending);

        final TimeLineDate serviceOfProsecutionCaseForBailCases = new TimeLineDate(
                        TimeLineDateType.serviceOfProsecutionCaseForBailCases, cpd.getDateOfSending(), dateNow,
                        ProgressionDataConstant.serviceOfProsecutionCaseForBailCasesDaysFromDateOfSending);

        final TimeLineDate defenceCaseStatement = new TimeLineDate(TimeLineDateType.defenceCaseStatement,
                        cpd.getDateOfSending(), dateNow, ProgressionDataConstant.defenceCaseStatementDaysFromDateOfSending);

        final TimeLineDate kpiDateForCommencementOfTrial = new TimeLineDate(TimeLineDateType.kpiDateForCommencementOfTrial,
                        cpd.getDateOfSending(), dateNow,
                        ProgressionDataConstant.kpiDateForCommencementOfTrialDaysFromDateOfSending);

        return Arrays.asList(dateOfSending, cmiSubmissionDeadline, serviceOfProsecutionCaseForBailCases,
                        defenceCaseStatement, kpiDateForCommencementOfTrial);
    }

    @Transactional
    public List<CaseProgressionDetail> getCases(Optional<String> status) {

        List<CaseProgressionDetail> caseProgressionDetails;

        if (status.isPresent()) {

            caseProgressionDetails = caseProgressionDetailRepo.findByStatus(getCaseStatusList(status.get()));
        } else {
            caseProgressionDetails = caseProgressionDetailRepo.findOpenStatus();

        }
        caseProgressionDetails.stream().forEach((caseProgressionDetail) -> {
            caseProgressionDetail.setTimeLine(getTimeline(caseProgressionDetail));
        });
        return caseProgressionDetails;

    }

    List<CaseStatusEnum> getCaseStatusList(String status) {
        final List<CaseStatusEnum> listOfStatus = new ArrayList<>();
        final StringTokenizer st = new StringTokenizer(status, ",");
        while (st.hasMoreTokens()) {
            listOfStatus.add(CaseStatusEnum.getCaseStatusk(st.nextToken()));
        }
        return listOfStatus;
    }
}
