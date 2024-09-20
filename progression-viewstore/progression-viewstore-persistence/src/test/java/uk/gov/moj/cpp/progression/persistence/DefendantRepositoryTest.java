package uk.gov.moj.cpp.progression.persistence;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.repository.CaseProgressionDetailRepository;
import uk.gov.moj.cpp.progression.persistence.repository.DefendantRepository;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @deprecated This is deprecated for Release 2.4
 */
@Deprecated
@RunWith(CdiTestRunner.class)
public class DefendantRepositoryTest {

    private static final String COURT_CENTER = "Liverpool";
    private static final String ID_ONE = "CASEURN";
    private static final UUID CASE_ID_ONE = UUID.randomUUID();
    private static final UUID DEF_ID = UUID.randomUUID();
    private static LocalDate now;
    private static ZonedDateTime currentDateTime;
    private final List<CaseProgressionDetail> caseProgressionDetails = new ArrayList<>();
    @Inject
    private CaseProgressionDetailRepository repository;
    @Inject
    private DefendantRepository defendantRepository;

    @Before
    public void setup() {
        now = LocalDate.now();
        currentDateTime = ZonedDateTime.now();
        final CaseProgressionDetail caseProgressionDetailOne =
                createCaseProgressionDetail(ID_ONE, CASE_ID_ONE, CaseStatusEnum.INCOMPLETE);
        caseProgressionDetails.add(caseProgressionDetailOne);
        final Defendant defendant =
                new Defendant(DEF_ID, caseProgressionDetailOne, false, null);
        defendant.setSentenceHearingReviewDecisionDateTime(currentDateTime);
        caseProgressionDetailOne.getDefendants().add(defendant);
        repository.save(caseProgressionDetailOne);

    }

    @Test
    public void shouldFindDefendantByProgressionId() throws Exception {
        final CaseProgressionDetail results = repository.findBy(CASE_ID_ONE);
        assertThat(results.getDefendants().iterator().next().getDefendantId(), equalTo(DEF_ID));
    }

    @Test
    public void shouldFindDefendantById() throws Exception {
        final Defendant results = defendantRepository.findByDefendantId(DEF_ID);
        assertThat(results.getDefendantId(), equalTo(DEF_ID));
        assertThat(results.getSentenceHearingReviewDecisionDateTime(), equalTo(currentDateTime));
    }

    private CaseProgressionDetail createCaseProgressionDetail(final String caseUrn, final UUID caseId,
                                                              final CaseStatusEnum status) {
        final CaseProgressionDetail caseProgressionDetail = new CaseProgressionDetail();
        caseProgressionDetail.setCaseId(caseId);
        caseProgressionDetail.setCaseUrn(caseUrn);
        caseProgressionDetail.setCourtCentreId(COURT_CENTER);
        caseProgressionDetail.setFromCourtCentre(COURT_CENTER);
        caseProgressionDetail.setCaseStatusUpdatedDateTime(ZonedDateTime.now(ZoneOffset.UTC).plusDays(7));
        caseProgressionDetail.setSendingCommittalDate(now);
        caseProgressionDetail.setSentenceHearingDate(now);
        caseProgressionDetail.setStatus(status);
        return caseProgressionDetail;
    }

    @After
    public void teardown() {
        caseProgressionDetails.forEach(caseProgressionDetail -> repository
                .attachAndRemove(repository.findBy(caseProgressionDetail.getCaseId())));
    }

}
