package uk.gov.moj.cpp.progression.persistence;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

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
public class CaseProgressionDetailRepositoryTest {

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
    @Inject
    private CaseProgressionDetailRepository repository;


    @Before
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


    @After
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

}
