package uk.gov.moj.cpp.progression.query.view.converter;

import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.Test;

import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.domain.utils.LocalDateUtils;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.query.view.response.CaseProgressionDetailView;

public class CaseProgressionDetailToViewConverterTest {
    private static final UUID ID = UUID.randomUUID();
    private static final UUID CASEID = UUID.randomUUID();
    private static final String COURT_CENTRE = "liverpool";

    @Test
    public void getCaseProgressionDetailTest() {
        final CaseProgressionDetailToViewConverter caseProgressionDetailToVOConverter =
                        new CaseProgressionDetailToViewConverter();

        final CaseProgressionDetail caseProgressionDetail = new CaseProgressionDetail();
        final LocalDate now = LocalDate.now();
        caseProgressionDetail.setCaseId(CASEID);
        caseProgressionDetail.setFromCourtCentre(COURT_CENTRE);
        caseProgressionDetail.setSendingCommittalDate(now);
        caseProgressionDetail.setSentenceHearingDate(now);
        caseProgressionDetail.setStatus(CaseStatusEnum.READY_FOR_REVIEW);

        CaseProgressionDetailView caseProgressionDetailVO =
                        caseProgressionDetailToVOConverter.convert(caseProgressionDetail);
        assertTrue(caseProgressionDetailVO.getCaseId().equals(CASEID.toString()));
        assertTrue(caseProgressionDetailVO.getFromCourtCentre().equals(COURT_CENTRE));
        assertTrue(caseProgressionDetailVO.getSendingCommittalDate().equals(now));
        assertTrue(caseProgressionDetailVO.getSentenceHearingDate().equals(now));
        assertTrue(caseProgressionDetailVO.getStatus()
                        .equals(CaseStatusEnum.READY_FOR_REVIEW.toString()));

        caseProgressionDetail.setSendingCommittalDate(now);
        caseProgressionDetailVO = caseProgressionDetailToVOConverter.convert(caseProgressionDetail);
        assertTrue(caseProgressionDetailVO.getSentenceReviewDeadlineDate().equals(LocalDateUtils
                        .addWorkingDays(caseProgressionDetail.getSendingCommittalDate(), 7)));
    }

}
