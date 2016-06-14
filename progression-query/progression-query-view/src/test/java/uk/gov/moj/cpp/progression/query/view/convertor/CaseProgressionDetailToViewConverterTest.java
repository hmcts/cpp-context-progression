package uk.gov.moj.cpp.progression.query.view.convertor;

import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.Test;

import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.domain.utils.LocalDateUtils;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.query.view.converter.CaseProgressionDetailToViewConverter;
import uk.gov.moj.cpp.progression.query.view.response.CaseProgressionDetailView;

public class CaseProgressionDetailToViewConverterTest {
    private static final UUID ID = UUID.randomUUID();
    private static final UUID CASEID = UUID.randomUUID();
    private Long cmiSubmissionDeadlineDate = 21l;
    private static final String DEFENCEISSUE = "defence issue one";
    private static final String SFRISSUE = "streamlined forensics report one";
    private static final String COURT_CENTRE = "liverpool";


    @Test
    public void getCaseProgressionDetailTest() {
        CaseProgressionDetailToViewConverter caseProgressionDetailToVOConverter =
                        new CaseProgressionDetailToViewConverter(21);

        CaseProgressionDetail caseProgressionDetail = new CaseProgressionDetail();
        caseProgressionDetail.setId(ID);
        final LocalDate now = LocalDate.now();
        caseProgressionDetail.setDateOfSending(now);
        caseProgressionDetail.setCaseId(CASEID);
        caseProgressionDetail.setDefenceIssue(DEFENCEISSUE);
        caseProgressionDetail.setSfrIssue(SFRISSUE);
        caseProgressionDetail.setTrialEstimateDefence(10l);
        caseProgressionDetail.setTrialEstimateProsecution(20l);
        caseProgressionDetail.setPtpHearingVacatedDate(now);
        caseProgressionDetail.setIsAllStatementsIdentified(true);
        caseProgressionDetail.setIsAllStatementsServed(true);
        caseProgressionDetail.setVersion(1L);
        caseProgressionDetail.setFromCourtCentre(COURT_CENTRE);
        caseProgressionDetail.setSendingCommittalDate(now);
        caseProgressionDetail.setIsPSROrdered(true);
        caseProgressionDetail.setSentenceHearingDate(now);
        caseProgressionDetail.setStatus(CaseStatusEnum.READY_FOR_REVIEW);

        CaseProgressionDetailView caseProgressionDetailVO =
                        caseProgressionDetailToVOConverter.convert(caseProgressionDetail);
        assertTrue(caseProgressionDetailVO.getCaseId().equals(CASEID.toString()));
        assertTrue(caseProgressionDetailVO.getCaseProgressionId().equals(ID.toString()));
        assertTrue(caseProgressionDetailVO.getDateOfSending().equals(now));
        assertTrue((caseProgressionDetailVO
                        .getNoOfDaysForCMISubmission()) == cmiSubmissionDeadlineDate);
        assertTrue(caseProgressionDetailVO.getDateCMISubmissionDeadline()
                        .equals(caseProgressionDetail.getDateOfSending()
                                        .plusDays(cmiSubmissionDeadlineDate)));
        assertTrue(caseProgressionDetailVO.getDefenceIssues().equals(DEFENCEISSUE));
        assertTrue(caseProgressionDetailVO.getSfrIssues().equals(SFRISSUE));
        assertTrue(caseProgressionDetailVO.getDefenceTrialEstimate() == 10l);
        assertTrue(caseProgressionDetailVO.getProsecutionTrialEstimate() == 20l);
        assertTrue(caseProgressionDetailVO.getPtpHearingVacatedDate().equals(now));
        assertTrue(caseProgressionDetailVO.getVersion().equals("1"));
        assertTrue(caseProgressionDetailVO.getIsAllStatementsIdentified().equals(true));
        assertTrue(caseProgressionDetailVO.getIsAllStatementsServed().equals(true));
        assertTrue(caseProgressionDetailVO.getFromCourtCentre().equals(COURT_CENTRE));
        assertTrue(caseProgressionDetailVO.getSendingCommittalDate().equals(now));
        assertTrue(caseProgressionDetailVO.getIsPSROrdered().equals(true));
        assertTrue(caseProgressionDetailVO.getSentenceHearingDate().equals(now));
        assertTrue(caseProgressionDetailVO.getStatus()
                        .equals(CaseStatusEnum.READY_FOR_REVIEW.toString()));
        assertTrue(caseProgressionDetailVO.getSentenceReviewDeadlineDate().equals(LocalDateUtils
                        .addWorkingDays(caseProgressionDetail.getDateOfSending(), 7)));

        caseProgressionDetail.setDateOfSending(null);
        caseProgressionDetail.setSendingCommittalDate(now);
        caseProgressionDetailVO = caseProgressionDetailToVOConverter.convert(caseProgressionDetail);
        assertTrue(caseProgressionDetailVO.getSentenceReviewDeadlineDate().equals(LocalDateUtils
                        .addWorkingDays(caseProgressionDetail.getSendingCommittalDate(), 7)));
    }

}
