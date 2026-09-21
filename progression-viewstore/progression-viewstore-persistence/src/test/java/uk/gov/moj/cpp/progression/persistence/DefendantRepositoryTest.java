package uk.gov.moj.cpp.progression.persistence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * @deprecated This is deprecated for Release 2.4
 */
@Deprecated
public class DefendantRepositoryTest {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("progression-test-persistence-unit");

    private static final String COURT_CENTER = "Liverpool";
    private static final String ID_ONE = "CASEURN";
    private static final UUID CASE_ID_ONE = UUID.randomUUID();
    private static final UUID DEF_ID = UUID.randomUUID();
    private static LocalDate now;
    private static ZonedDateTime currentDateTime;
    private final List<CaseProgressionDetail> caseProgressionDetails = new ArrayList<>();
    private CaseProgressionDetailRepository repository;
    private DefendantRepository defendantRepository;

    @BeforeEach
    void createRepositoriesWithATestEntityManager() {
        repository = new CaseProgressionDetailRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(repository);
        defendantRepository = new DefendantRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(defendantRepository);
    }

    @BeforeEach
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

    /**
     * Saves through the defendant repository itself rather than letting the case cascade, which is
     * the only way its own idOf is exercised - and idOf is what decides whether save() persists a
     * new row or merges an existing one.
     */
    @Test
    public void shouldSaveADefendantDirectlyAndReadItBack() {
        final UUID defendantId = UUID.randomUUID();
        final CaseProgressionDetail caseProgressionDetail =
                createCaseProgressionDetail("URN-DIRECT", UUID.randomUUID(), CaseStatusEnum.INCOMPLETE);
        repository.saveAndFlush(caseProgressionDetail);

        final Defendant defendant = new Defendant(defendantId, caseProgressionDetail, false, null);
        defendantRepository.saveAndFlush(defendant);

        final Defendant found = defendantRepository.findByDefendantId(defendantId);
        assertThat(found.getDefendantId(), equalTo(defendantId));
    }

    @Test
    public void shouldUpdateRatherThanDuplicateWhenSavingADefendantThatAlreadyExists() {
        final UUID defendantId = UUID.randomUUID();
        final CaseProgressionDetail caseProgressionDetail =
                createCaseProgressionDetail("URN-TWICE", UUID.randomUUID(), CaseStatusEnum.INCOMPLETE);
        repository.saveAndFlush(caseProgressionDetail);

        final Defendant defendant = new Defendant(defendantId, caseProgressionDetail, false, null);
        defendantRepository.saveAndFlush(defendant);
        final Long countAfterFirstSave = defendantRepository.count();

        final Defendant again = new Defendant(defendantId, caseProgressionDetail, true, null);
        defendantRepository.saveAndFlush(again);

        assertThat(defendantRepository.count(), equalTo(countAfterFirstSave));
        assertThat(defendantRepository.findByDefendantId(defendantId).getDefendantId(), equalTo(defendantId));
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
    @AfterEach
    public void teardown() {
        caseProgressionDetails.forEach(caseProgressionDetail -> repository
                .attachAndRemove(repository.findBy(caseProgressionDetail.getCaseId())));
    }

}
