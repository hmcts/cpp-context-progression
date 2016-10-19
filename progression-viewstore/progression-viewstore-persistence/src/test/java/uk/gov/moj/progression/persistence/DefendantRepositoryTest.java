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
import uk.gov.moj.progression.persistence.repository.DefendantRepository;

@RunWith(CdiTestRunner.class)
public class DefendantRepositoryTest {

    private static final String COURT_CENTER = "Liverpool";
    private static final UUID ID_ONE = UUID.randomUUID();
    private static final UUID CASE_ID_ONE = UUID.randomUUID();
    private static final UUID DEF_PRG_ID = UUID.randomUUID();
    private static final UUID DEF_ID = UUID.randomUUID();
    private List<CaseProgressionDetail> caseProgressionDetails = new ArrayList<>();

    @Inject
    private CaseProgressionDetailRepository repository;

    @Inject
    private DefendantRepository defendantRepository;

    private static LocalDate now;

    @Before
    public void setup() {
        now = LocalDate.now();
        CaseProgressionDetail caseProgressionDetailOne =
                        createCaseProgressionDetail(ID_ONE, CASE_ID_ONE, CaseStatusEnum.INCOMPLETE);
        caseProgressionDetails.add(caseProgressionDetailOne);
        Defendant defendant = new Defendant(DEF_PRG_ID, DEF_ID, caseProgressionDetailOne, false);
        caseProgressionDetailOne.getDefendants().add(defendant);
        repository.save(caseProgressionDetailOne);

    }

    @Test
    public void shouldFindDefendantByProgressionId() throws Exception {
        CaseProgressionDetail results = repository.findBy(ID_ONE);
        assertThat(results.getDefendants().iterator().next().getDefendantId(), equalTo(DEF_ID));
    }

    @Test
    public void shouldFindDefendantById() throws Exception {
        Defendant results = defendantRepository.findByDefendantId(DEF_ID);
        assertThat(results.getDefendantId(), equalTo(DEF_ID));
    }

    private CaseProgressionDetail createCaseProgressionDetail(UUID id, UUID caseId,
                    CaseStatusEnum status) {
        CaseProgressionDetail caseProgressionDetail = new CaseProgressionDetail();
        caseProgressionDetail.setCaseId(caseId);
        caseProgressionDetail.setId(id);
        caseProgressionDetail.setCourtCentreId(COURT_CENTER);
        caseProgressionDetail.setDirectionIssuedOn(now);
        caseProgressionDetail.setVersion(0l);
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

}
