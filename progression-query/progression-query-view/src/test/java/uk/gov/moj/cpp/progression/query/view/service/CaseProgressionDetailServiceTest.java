package uk.gov.moj.cpp.progression.query.view.service;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.persistence.NoResultException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.query.view.converter.CaseProgressionDetailToViewConverter;
import uk.gov.moj.progression.persistence.repository.CaseProgressionDetailRepository;

/**
 * Unit tests for the CaseProgressionDetailTest class.
 */
@RunWith(MockitoJUnitRunner.class)
public class CaseProgressionDetailServiceTest {

    private static final UUID ID = UUID.randomUUID();

    private static final UUID CASEID =  UUID.randomUUID();

    private static final String DEFENCEISSUE = "defence issue one";

    private static final String SFRISSUE = "streamlined forensics report one";

    private static final String COURT_CENTRE="liverpool";

    @Mock
    private CaseProgressionDetail caseProgressionDetail1;

    @Mock
    private CaseProgressionDetail caseProgressionDetail2;

    @Mock
    private CaseProgressionDetail caseProgressionDetail3;

    @Mock
    private CaseProgressionDetailRepository caseProgressionDetailRepository;

    @Spy
    private final CaseProgressionDetailToViewConverter caseProgressionDetailToVOConverter = new CaseProgressionDetailToViewConverter();

    @InjectMocks
    private CaseProgressionDetailService caseProgressionDetailService;


    @Test(expected = NoResultException.class)
    public void getCaseProgressionDetailTestIsEmpty() {

        when(this.caseProgressionDetailRepository.findByCaseId(CASEID))
        .thenThrow(new NoResultException());

        this.caseProgressionDetailService.getCaseProgressionDetail(CASEID);
    }

    @Test
    public void getCaseProgressionDetailTest() throws IOException {

        final CaseProgressionDetail caseProgressionDetail = new CaseProgressionDetail();
        caseProgressionDetail.setId( ID);
        caseProgressionDetail.setDateOfSending(LocalDate.now());
        caseProgressionDetail.setCaseId(CASEID);
        caseProgressionDetail.setDefenceIssue(DEFENCEISSUE);
        caseProgressionDetail.setSfrIssue(SFRISSUE);
        caseProgressionDetail.setIsAllStatementsServed(true);
        caseProgressionDetail.setIsAllStatementsIdentified(true);
        caseProgressionDetail.setTrialEstimateDefence(10l);
        caseProgressionDetail.setTrialEstimateProsecution(20l);
        caseProgressionDetail.setPtpHearingVacatedDate(LocalDate.now());
        caseProgressionDetail.setFromCourtCentre(COURT_CENTRE);
        caseProgressionDetail.setSendingCommittalDate(LocalDate.now());
        caseProgressionDetail.setIsPSROrdered(true);
        caseProgressionDetail.setStatus(CaseStatusEnum.READY_FOR_REVIEW);

        when(this.caseProgressionDetailRepository.findByCaseId(CASEID)).thenReturn(caseProgressionDetail);

        assertTrue(this.caseProgressionDetailService.getCaseProgressionDetail(CASEID).getCaseId()
                        .equals(CASEID));
        assertTrue(this.caseProgressionDetailService.getCaseProgressionDetail(CASEID).getId()
                        .equals(ID));
        assertTrue(this.caseProgressionDetailService.getCaseProgressionDetail(CASEID)
                        .getDateOfSending().equals(LocalDate.now()));
        assertTrue(this.caseProgressionDetailService.getCaseProgressionDetail(CASEID)
                        .getDefenceIssue().equals(DEFENCEISSUE));
        assertTrue(this.caseProgressionDetailService.getCaseProgressionDetail(CASEID).getSfrIssue()
                        .equals(SFRISSUE));

        assertThat(this.caseProgressionDetailService.getCaseProgressionDetail(CASEID).getTimeLine(),
                        hasSize(5));
        assertTrue(this.caseProgressionDetailService.getCaseProgressionDetail(CASEID)
                        .getDateOfSending().equals(LocalDate.now()));

        assertTrue(this.caseProgressionDetailService.getCaseProgressionDetail(CASEID)
                        .getIsAllStatementsServed().equals(true));
        assertTrue(this.caseProgressionDetailService.getCaseProgressionDetail(CASEID)
                        .getIsAllStatementsIdentified().equals(true));
        assertTrue(this.caseProgressionDetailService.getCaseProgressionDetail(CASEID)
                        .getTrialEstimateDefence() == 10l);
        assertTrue(this.caseProgressionDetailService.getCaseProgressionDetail(CASEID)
                        .getPtpHearingVacatedDate()
                        .equals(LocalDate.now()));
        assertTrue(this.caseProgressionDetailService.getCaseProgressionDetail(CASEID)
                        .getFromCourtCentre().equals(COURT_CENTRE));
        assertTrue(this.caseProgressionDetailService.getCaseProgressionDetail(CASEID)
                        .getSendingCommittalDate().equals(LocalDate.now()));
        assertTrue(this.caseProgressionDetailService.getCaseProgressionDetail(CASEID)
                        .getIsPSROrdered().equals(true));
        assertTrue(this.caseProgressionDetailService.getCaseProgressionDetail(CASEID).getStatus()
                        .equals(CaseStatusEnum.READY_FOR_REVIEW));
    }

    @Test
    public void shouldReturnAllCases() throws Exception {

        when(caseProgressionDetailRepository.findOpenStatus())
        .thenReturn(Arrays.asList(caseProgressionDetail1, caseProgressionDetail2,
                        caseProgressionDetail3));

        final List<CaseProgressionDetail> cases =
                        caseProgressionDetailService.getCases(Optional.ofNullable(null));

        verify(caseProgressionDetailRepository, times(1)).findOpenStatus();
        assertThat(cases, hasSize(3));
    }

    @Test
    public void shouldReturnCasesForStatus() throws Exception {

        final String status = "COMPLETED,ASSIGNED_FOR_REVIEW";
        final List<CaseStatusEnum> statusList =
                        Arrays.asList(CaseStatusEnum.COMPLETED, CaseStatusEnum.ASSIGNED_FOR_REVIEW);

        when(caseProgressionDetailRepository.findByStatus(statusList)).thenReturn(Arrays.asList(
                        caseProgressionDetail1, caseProgressionDetail2, caseProgressionDetail3));

        final List<CaseProgressionDetail> cases =
                        caseProgressionDetailService.getCases(Optional.ofNullable(status));

        verify(caseProgressionDetailRepository, times(1)).findByStatus(statusList);
        assertThat(cases, hasSize(3));
    }
}
