package uk.gov.moj.cpp.progression.persistence;

import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.entity.DefendantBailDocument;
import uk.gov.moj.cpp.progression.persistence.repository.CaseProgressionDetailRepository;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * @deprecated This is deprecated for Release 2.4
 */
@Deprecated
public class CaseProgressionDetailRepositoryTest {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("progression-test-persistence-unit");

    private static final String COURT_CENTER = "Liverpool";
    private static final UUID CASE_ID_ONE = UUID.randomUUID();
    private static final UUID CASE_ID_TWO = UUID.randomUUID();
    private static final UUID DEF_ID = UUID.randomUUID();
    public static final ZonedDateTime CASE_STATUS_UPDATED_DATE_TIME = ZonedDateTime.now(ZoneOffset.UTC).plusDays(7);
    public static final String CASE_URN_ONE = "URNONE";
    public static final String CASE_URN_TWO = "URNTWO";
    public static final UUID MATERIAL_ID = UUID.randomUUID();
    private static LocalDate now;
    private final List<CaseProgressionDetail> caseProgressionDetails = new ArrayList<>();
    private CaseProgressionDetailRepository repository;

    @BeforeEach
    void createRepositoriesWithATestEntityManager() {
        repository = new CaseProgressionDetailRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(repository);
    }

    @BeforeEach
    public void setup() {
        now = LocalDate.now();
        final CaseProgressionDetail caseProgressionDetailOne =
                createCaseProgressionDetail(CASE_ID_ONE, CaseStatusEnum.INCOMPLETE, CASE_URN_ONE);
        caseProgressionDetails.add(caseProgressionDetailOne);

        final Defendant defendant =
                new Defendant(DEF_ID, caseProgressionDetailOne, false, null);
        final DefendantBailDocument defendantBailDocument = new DefendantBailDocument();
        defendantBailDocument.setDocumentId(MATERIAL_ID);
        defendantBailDocument.setId(UUID.randomUUID());
        defendantBailDocument.setActive(Boolean.TRUE);
        defendant.addDefendantBailDocument(defendantBailDocument);
        caseProgressionDetailOne.getDefendants().add(defendant);
        repository.save(caseProgressionDetailOne);

        final CaseProgressionDetail caseProgressionDetailTwo = createCaseProgressionDetail(
                CASE_ID_TWO, CaseStatusEnum.READY_FOR_REVIEW, CASE_URN_TWO);
        caseProgressionDetails.add(caseProgressionDetailTwo);
        repository.save(caseProgressionDetailTwo);

    }

    private CaseProgressionDetail createCaseProgressionDetail(final UUID caseId,
                                                              final CaseStatusEnum status, final String caseUrn) {
        final CaseProgressionDetail caseProgressionDetail = new CaseProgressionDetail();
        caseProgressionDetail.setCaseId(caseId);
        caseProgressionDetail.setCaseUrn(caseUrn);
        caseProgressionDetail.setCourtCentreId(COURT_CENTER);
        caseProgressionDetail.setFromCourtCentre(COURT_CENTER);
        caseProgressionDetail.setCaseStatusUpdatedDateTime(CASE_STATUS_UPDATED_DATE_TIME);
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

    @Test
    public void shouldFindByCaseId() throws Exception {
        final CaseProgressionDetail result = repository.findByCaseId(CASE_ID_ONE);
        assertThat(result.getCaseId(), equalTo(CASE_ID_ONE));
        assertThat(result.getCaseUrn(), equalTo(CASE_URN_ONE));
        assertThat(result.getCourtCentreId(), equalTo(COURT_CENTER));
        assertThat(result.getFromCourtCentre(), equalTo(COURT_CENTER));
        assertThat(result.getCaseStatusUpdatedDateTime(),
                equalTo(CASE_STATUS_UPDATED_DATE_TIME));
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
    public void shouldfindCaseByMaterialIdWhenMaterialIsDocument() {

        final CaseProgressionDetail caseProgressionDetail = repository.findByMaterialId(MATERIAL_ID);
        assertThat(caseProgressionDetail.getCaseId(), equalTo(CASE_ID_ONE));
    }

    @Test
    public void shouldFindDefendantByProgressionId() throws Exception {
        final CaseProgressionDetail results = repository.findBy(CASE_ID_ONE);
        assertThat(results.getDefendants().size(), equalTo(1));
    }

    @Test
    public void shouldFindTheCasesInAnyOfTheGivenStatuses() {
        final List<CaseProgressionDetail> found =
                repository.findByStatus(List.of(CaseStatusEnum.INCOMPLETE, CaseStatusEnum.READY_FOR_REVIEW));

        assertThat(found.stream().map(CaseProgressionDetail::getCaseId).collect(toSet()),
                equalTo(Set.of(CASE_ID_ONE, CASE_ID_TWO)));
    }

    @Test
    public void shouldFindOnlyTheCasesInTheStatusAsked() {
        final List<CaseProgressionDetail> found = repository.findByStatus(List.of(CaseStatusEnum.INCOMPLETE));

        assertThat(found.stream().map(CaseProgressionDetail::getCaseId).toList(), equalTo(List.of(CASE_ID_ONE)));
    }

    @Test
    public void shouldNarrowAStatusSearchToASingleCase() {
        final List<CaseProgressionDetail> found = repository.findByStatusAndCaseID(
                List.of(CaseStatusEnum.INCOMPLETE, CaseStatusEnum.READY_FOR_REVIEW), CASE_ID_TWO);

        assertThat(found.stream().map(CaseProgressionDetail::getCaseId).toList(), equalTo(List.of(CASE_ID_TWO)));
    }

    @Test
    public void shouldReturnNothingWhenTheCaseIsNotInAnyOfThoseStatuses() {
        assertThat(repository.findByStatusAndCaseID(List.of(CaseStatusEnum.CLOSED), CASE_ID_ONE).size(),
                equalTo(0));
    }

    @Test
    public void shouldFindTheDefendantsOnACase() {
        final List<Defendant> defendants = repository.findCaseDefendants(CASE_ID_ONE);

        assertThat(defendants.size(), equalTo(1));
        assertThat(defendants.get(0).getDefendantId(), equalTo(DEF_ID));
    }

    @Test
    public void shouldReturnNoDefendantsForACaseThatHasNone() {
        assertThat(repository.findCaseDefendants(CASE_ID_TWO).size(), equalTo(0));
    }

}
