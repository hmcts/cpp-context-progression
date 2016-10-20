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
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.progression.persistence.repository.CaseProgressionDetailRepository;

/**
 * 
 * @author jchondig
 *
 */
@RunWith(CdiTestRunner.class)
public class CaseProgressionDetailRepositoryTest {

    private static final String COURT_CENTER = "Liverpool";
    private static final UUID ID_ONE = UUID.randomUUID();
    private static final UUID ID_TWO = UUID.randomUUID();
    private static final UUID CASE_ID_ONE = UUID.randomUUID();
    private static final UUID CASE_ID_TWO = UUID.randomUUID();
    private static final UUID DEF_PRG_ID = UUID.randomUUID();
    private static final UUID DEF_ID = UUID.randomUUID();
    private final List<CaseProgressionDetail> caseProgressionDetails = new ArrayList<>();

    @Inject
    private CaseProgressionDetailRepository repository;

    private static LocalDate now;

    @Before
    public void setup() {
        now = LocalDate.now();
        final CaseProgressionDetail caseProgressionDetailOne =
                        createCaseProgressionDetail(ID_ONE, CASE_ID_ONE, CaseStatusEnum.INCOMPLETE);
        caseProgressionDetails.add(caseProgressionDetailOne);
        final Defendant defendant =
                        new Defendant(DEF_PRG_ID, DEF_ID, caseProgressionDetailOne, false);
        caseProgressionDetailOne.getDefendants().add(defendant);
        repository.save(caseProgressionDetailOne);

        final CaseProgressionDetail caseProgressionDetailTwo = createCaseProgressionDetail(ID_TWO,
                        CASE_ID_TWO, CaseStatusEnum.READY_FOR_REVIEW);
        caseProgressionDetails.add(caseProgressionDetailTwo);
        repository.save(caseProgressionDetailTwo);

    }

    private CaseProgressionDetail createCaseProgressionDetail(final UUID id, final UUID caseId,
                    final CaseStatusEnum status) {
        final CaseProgressionDetail caseProgressionDetail = new CaseProgressionDetail();
        caseProgressionDetail.setCaseId(caseId);
        caseProgressionDetail.setId(id);
        caseProgressionDetail.setCourtCentreId(COURT_CENTER);
        caseProgressionDetail.setDirectionIssuedOn(now);
        caseProgressionDetail.setFromCourtCentre(COURT_CENTER);
        caseProgressionDetail.setReadyForSentenceHearingDate(now.plusDays(7).atStartOfDay());
        caseProgressionDetail.setSendingCommittalDate(now);
        caseProgressionDetail.setSentenceHearingDate(now);
        caseProgressionDetail.setStatus(status);
        return caseProgressionDetail;
    }


    @After
    public void teardown() {
        caseProgressionDetails.forEach(caseProgressionDetail -> repository
                        .attachAndRemove(repository.findBy(caseProgressionDetail.getId())));
    }

    @Test
    public void shouldFindByCaseProgressionId() throws Exception {
        final CaseProgressionDetail result = repository.findByCaseId(CASE_ID_ONE);
        assertThat(result.getCaseId(), equalTo(CASE_ID_ONE));
        assertThat(result.getCourtCentreId(), equalTo(COURT_CENTER));
        assertThat(result.getDirectionIssuedOn(), equalTo(now));
        assertThat(result.getFromCourtCentre(), equalTo(COURT_CENTER));
        assertThat(result.getReadyForSentenceHearingDate(),
                        equalTo(now.plusDays(7).atStartOfDay()));
        assertThat(result.getSendingCommittalDate(), equalTo(now));
        assertThat(result.getSentenceHearingDate(), equalTo(now));
        assertThat(result.getStatus(), equalTo(CaseStatusEnum.INCOMPLETE));

    }

    @Test
    public void shouldFindAll() throws Exception {
        final List<CaseProgressionDetail> results = repository.findAll();
        assertThat(results.size(), equalTo(2));
        final CaseProgressionDetail result = results.get(0);
        assertThat(result.getCourtCentreId(), equalTo(COURT_CENTER));
    }

    @Test
    public void shouldFindByStatus() throws Exception {
        final List<CaseProgressionDetail> results =
                        repository.findByStatus(Arrays.asList(CaseStatusEnum.INCOMPLETE));
        assertThat(results.size(), equalTo(1));
        final CaseProgressionDetail result = results.get(0);
        assertThat(result.getStatus(), equalTo(CaseStatusEnum.INCOMPLETE));
    }

    @Test
    public void shouldFindOpenStatus() throws Exception {
        final List<CaseProgressionDetail> results = repository.findOpenStatus();
        assertThat(results.size(), equalTo(2));
        final CaseProgressionDetail result = results.get(0);
        assertThat(result.getCourtCentreId(), equalTo(COURT_CENTER));
    }


    @Test
    public void shouldFindDefendantByProgressionId() throws Exception {
        final CaseProgressionDetail results = repository.findBy(ID_ONE);
        assertThat(results.getDefendants().size(), equalTo(1));
    }

}
