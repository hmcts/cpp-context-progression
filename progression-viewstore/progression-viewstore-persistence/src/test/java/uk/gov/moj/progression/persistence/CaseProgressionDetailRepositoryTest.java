package uk.gov.moj.progression.persistence;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.domain.constant.TimeLineDateType;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.TimeLineDate;
import uk.gov.moj.progression.persistence.repository.CaseProgressionDetailRepository;

/**
 * 
 * @author jchondig
 *
 */
@RunWith(CdiTestRunner.class)
public class CaseProgressionDetailRepositoryTest {

    private static final String ISSUE = "issue one";
    private static final String COURT_CENTER = "Liverpool";
    private static final UUID ID_ONE = UUID.randomUUID();
    private static final UUID ID_TWO = UUID.randomUUID();
    private static final UUID CASE_ID_ONE = UUID.randomUUID();
    private static final UUID CASE_ID_TWO = UUID.randomUUID();
    private List<CaseProgressionDetail> caseProgressionDetails = new ArrayList<>();

    @Inject
    private CaseProgressionDetailRepository repository;

    private static LocalDate now;

    @Before
    public void setup() {
        now = LocalDate.now();
        CaseProgressionDetail caseProgressionDetailOne = createCaseProgressionDetail(ID_ONE, CASE_ID_ONE,
                CaseStatusEnum.INCOMPLETE);
        caseProgressionDetails.add(caseProgressionDetailOne);
        repository.save(caseProgressionDetailOne);

        CaseProgressionDetail caseProgressionDetailTwo = createCaseProgressionDetail(ID_TWO, CASE_ID_TWO,
                CaseStatusEnum.READY_FOR_REVIEW);
        caseProgressionDetails.add(caseProgressionDetailTwo);
        repository.save(caseProgressionDetailTwo);

    }

    private CaseProgressionDetail createCaseProgressionDetail(UUID id, UUID caseId, CaseStatusEnum status) {
        CaseProgressionDetail caseProgressionDetail = new CaseProgressionDetail();
        caseProgressionDetail.setCaseId(caseId);
        caseProgressionDetail.setId(id);
        caseProgressionDetail.setDateOfSending(now);
        caseProgressionDetail.setCourtCentreId(COURT_CENTER);
        caseProgressionDetail.setPtpHearingVacatedDate(now);
        caseProgressionDetail.setDefenceIssue(ISSUE);
        caseProgressionDetail.setDirectionIssuedOn(now);
        caseProgressionDetail.setVersion(0l);
        caseProgressionDetail.setFromCourtCentre(COURT_CENTER);
        caseProgressionDetail.setIsAllStatementsIdentified(true);
        caseProgressionDetail.setIsAllStatementsServed(true);
        caseProgressionDetail.setIsPSROrdered(true);
        caseProgressionDetail.setReadyForSentenceHearingDate(now.plusDays(7));
        caseProgressionDetail.setSendingCommittalDate(now);
        caseProgressionDetail.setSentenceHearingDate(now);
        caseProgressionDetail.setSfrIssue(ISSUE);
        caseProgressionDetail.setTrialEstimateDefence(7L);
        caseProgressionDetail.setTrialEstimateProsecution(6L);
        caseProgressionDetail.setStatus(status);
        return caseProgressionDetail;
    }

    @After
    public void teardown() {
        caseProgressionDetails.forEach(
                caseProgressionDetail -> repository.attachAndRemove(repository.findBy(caseProgressionDetail.getId())));
    }

    private List<TimeLineDate> getTimeLine() {
        final TimeLineDate timeLineDate = new TimeLineDate(TimeLineDateType.cmiSubmissionDeadline, now, now, 2);
        return Arrays.asList(timeLineDate);
    }

    @Test
    public void shouldFindByCaseProgressionId() throws Exception {
        CaseProgressionDetail result = repository.findByCaseId(CASE_ID_ONE);
        result.setTimeLine(getTimeLine());
        assertThat(result.getCaseId(), equalTo(CASE_ID_ONE));
        assertThat(result.getTimeLine().get(0).getDaysFromStartDate(), equalTo(2l));
        assertThat(result.getTimeLine().get(0).getDaysToDeadline(), equalTo(2l));
        assertThat(result.getTimeLine().get(0).getDeadLineDate(), equalTo(now.plusDays(2)));
        assertThat(result.getTimeLine().get(0).getStartDate(), equalTo(now));
        assertThat(result.getTimeLine().get(0).getType(), equalTo(TimeLineDateType.cmiSubmissionDeadline));
        assertThat(result.getDateOfSending(), equalTo(now));
        assertThat(result.getCourtCentreId(), equalTo(COURT_CENTER));
        assertThat(result.getPtpHearingVacatedDate(), equalTo(now));
        assertThat(result.getDefenceIssue(), equalTo(ISSUE));
        assertThat(result.getDirectionIssuedOn(), equalTo(now));
        assertThat(result.getVersion(), equalTo(0l));
        assertThat(result.getFromCourtCentre(), equalTo(COURT_CENTER));
        assertThat(result.getIsAllStatementsIdentified(), equalTo(true));
        assertThat(result.getIsAllStatementsServed(), equalTo(true));
        assertThat(result.getIsPSROrdered(), equalTo(true));
        assertThat(result.getReadyForSentenceHearingDate(), equalTo(now.plusDays(7)));
        assertThat(result.getSendingCommittalDate(), equalTo(now));
        assertThat(result.getSentenceHearingDate(), equalTo(now));
        assertThat(result.getSfrIssue(), equalTo(ISSUE));
        assertThat(result.getTrialEstimateDefence(), equalTo(7L));
        assertThat(result.getTrialEstimateProsecution(), equalTo(6L));
        assertThat(result.getStatus(), equalTo(CaseStatusEnum.INCOMPLETE));

    }

    @Test
    public void shouldFindAll() throws Exception {
        List<CaseProgressionDetail> results = repository.findAll();
        assertThat(results.size(), equalTo(2));
        CaseProgressionDetail result = results.get(0);
        assertThat(result.getCourtCentreId(), equalTo(COURT_CENTER));
    }

    @Test
    public void shouldFindByStatus() throws Exception {
        List<CaseProgressionDetail> results = repository.findByStatus(Arrays.asList(CaseStatusEnum.INCOMPLETE));
        assertThat(results.size(), equalTo(1));
        CaseProgressionDetail result = results.get(0);
        assertThat(result.getStatus(), equalTo(CaseStatusEnum.INCOMPLETE));
    }

    @Test
    public void shouldFindOpenStatus() throws Exception {
        List<CaseProgressionDetail> results = repository.findOpenStatus();
        assertThat(results.size(), equalTo(2));
        CaseProgressionDetail result = results.get(0);
        assertThat(result.getCourtCentreId(), equalTo(COURT_CENTER));
    }
}