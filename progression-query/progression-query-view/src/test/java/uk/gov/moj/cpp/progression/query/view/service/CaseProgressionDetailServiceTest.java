package uk.gov.moj.cpp.progression.query.view.service;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.repository.CaseProgressionDetailRepository;
import uk.gov.moj.cpp.progression.persistence.repository.DefendantRepository;
import uk.gov.moj.cpp.progression.query.view.AbstractProgressionQueryBaseTest;
import uk.gov.moj.cpp.progression.query.view.response.CaseProgressionDetailView;
import uk.gov.moj.cpp.progression.query.view.response.DefendantView;
import uk.gov.moj.cpp.progression.query.view.response.DefendantsView;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.persistence.NoResultException;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the CaseProgressionDetailTest class.
 * @deprecated
 *
 */
@Deprecated
@ExtendWith(MockitoExtension.class)
public class CaseProgressionDetailServiceTest extends AbstractProgressionQueryBaseTest {

    private static final UUID CASEID = UUID.randomUUID();


    private static final String COURT_CENTRE = "liverpool";

    private CaseProgressionDetail caseProgressionDetail1;

    private CaseProgressionDetail caseProgressionDetail2;

    private CaseProgressionDetail caseProgressionDetail3;
    @Mock
    private DefendantRepository defendantRepository;
    @Mock
    private CaseProgressionDetailRepository caseProgressionDetailRepository;
    @InjectMocks
    private CaseProgressionDetailService caseProgressionDetailService;


    @Test
    public void getCaseProgressionDetailTestIsEmpty() {

        when(this.caseProgressionDetailRepository.findByCaseId(CASEID))
                .thenThrow(new NoResultException());

        this.caseProgressionDetailService.getCaseProgressionDetail(CASEID);
    }

    @Test
    public void getCaseProgressionDetailTest() throws Exception {
        final UUID defendantId = UUID.randomUUID();

        final CaseProgressionDetail caseProgressionDetail = getCaseProgressionDetail(CASEID, defendantId);

        caseProgressionDetail.setCaseId(CASEID);
        caseProgressionDetail.setFromCourtCentre(COURT_CENTRE);
        caseProgressionDetail.setSendingCommittalDate(LocalDate.now());
        caseProgressionDetail.setStatus(CaseStatusEnum.READY_FOR_REVIEW);

        when(this.caseProgressionDetailRepository.findByCaseId(CASEID))
                .thenReturn(caseProgressionDetail);

        assertTrue(this.caseProgressionDetailService.getCaseProgressionDetail(CASEID).getCaseId()
                .equals(CASEID.toString()));
        assertTrue(this.caseProgressionDetailService.getCaseProgressionDetail(CASEID)
                .getFromCourtCentre().equals(COURT_CENTRE));
        assertTrue(this.caseProgressionDetailService.getCaseProgressionDetail(CASEID)
                .getSendingCommittalDate().equals(LocalDate.now()));
        assertTrue(this.caseProgressionDetailService.getCaseProgressionDetail(CASEID).getStatus()
                .equals(CaseStatusEnum.READY_FOR_REVIEW.getDescription()));
    }

    @Test
    public void shouldReturnAllCases() throws Exception {

        caseProgressionDetail1 = getCaseProgressionDetail(UUID.randomUUID(), UUID.randomUUID());

        caseProgressionDetail2 = getCaseProgressionDetail(UUID.randomUUID(), UUID.randomUUID());

        caseProgressionDetail3 = getCaseProgressionDetail(UUID.randomUUID(), UUID.randomUUID());

        when(caseProgressionDetailRepository.findOpenStatus()).thenReturn(Arrays.asList(
                caseProgressionDetail1, caseProgressionDetail2, caseProgressionDetail3));

        final List<CaseProgressionDetailView> cases =
                caseProgressionDetailService.getCases(Optional.ofNullable(null));

        verify(caseProgressionDetailRepository, times(1)).findOpenStatus();
        assertThat(cases, hasSize(3));
    }

    @Test
    public void shouldReturnCasesForStatus() throws Exception {

        caseProgressionDetail1 = getCaseProgressionDetail(UUID.randomUUID(), UUID.randomUUID());

        caseProgressionDetail2 = getCaseProgressionDetail(UUID.randomUUID(), UUID.randomUUID());

        caseProgressionDetail3 = getCaseProgressionDetail(UUID.randomUUID(), UUID.randomUUID());


        final String status = "COMPLETED,READY_FOR_REVIEW";
        final List<CaseStatusEnum> statusList =
                Arrays.asList(CaseStatusEnum.READY_FOR_REVIEW, CaseStatusEnum.READY_FOR_REVIEW);

        when(caseProgressionDetailRepository.findByStatus(any())).thenReturn(Arrays.asList(
                caseProgressionDetail1, caseProgressionDetail2, caseProgressionDetail3));

        final List<CaseProgressionDetailView> cases =
                caseProgressionDetailService.getCases(Optional.ofNullable(status));

        verify(caseProgressionDetailRepository, times(1)).findByStatus(any());
        assertThat(cases, hasSize(3));
    }

    @Test
    public void shouldReturnCasesForStatusAndCaseId() throws Exception {
        final UUID defendantId = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();

        final String status = "INCOMPLETE,READY_FOR_REVIEW";
        final List<CaseStatusEnum> statusList =
                Arrays.asList(CaseStatusEnum.INCOMPLETE, CaseStatusEnum.READY_FOR_REVIEW);

        when(caseProgressionDetailRepository.findByStatusAndCaseID(statusList,CASEID)).thenReturn(Arrays.asList(
                getCaseProgressionDetail(caseId, defendantId)));

        final List<CaseProgressionDetailView> cases =
                caseProgressionDetailService.getCases(Optional.ofNullable(status),Optional.ofNullable(CASEID));

        verify(caseProgressionDetailRepository, times(1)).findByStatusAndCaseID(statusList,CASEID);
        assertThat(cases, hasSize(1));
    }

}
