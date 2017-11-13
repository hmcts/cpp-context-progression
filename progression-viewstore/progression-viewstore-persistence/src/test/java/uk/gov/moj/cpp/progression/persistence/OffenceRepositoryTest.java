package uk.gov.moj.cpp.progression.persistence;

import com.google.common.collect.Sets;
import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.progression.persistence.repository.CaseProgressionDetailRepository;
import uk.gov.moj.cpp.progression.persistence.repository.OffenceRepository;

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * DB integration tests for {@link OffenceRepository} class
 */
@RunWith(CdiTestRunner.class)
public class OffenceRepositoryTest  {

    private static final UUID VALID_OFFENCE_DETAIL_ID = randomUUID();
    private static final String COURT_CENTER = "Liverpool";
    private static final String CASEURN = "CASEURN";
    private static final UUID CASE_ID_ONE = UUID.randomUUID();
    private static final UUID DEF_ID = UUID.randomUUID();

    @Inject
    private OffenceRepository offenceRepository;

    @Inject
    private CaseProgressionDetailRepository caseRepository;

    @Test
    public void shouldFindOptionalBy() throws Exception {
        //given
        caseRepository.save(getCaseWithDefendantOffences());

        OffenceDetail actual = offenceRepository.findBy(VALID_OFFENCE_DETAIL_ID);
        assertNotNull("Should not be null", actual);
        assertEquals(VALID_OFFENCE_DETAIL_ID, actual.getId());
    }

    private CaseProgressionDetail getCaseWithDefendantOffences() {
        CaseProgressionDetail caseDetail = new CaseProgressionDetail();
        caseDetail.setId(CASE_ID_ONE);
        caseDetail.setCaseId(CASE_ID_ONE);
        caseDetail.setCaseUrn(CASEURN);
        caseDetail.addDefendant(new Defendant(DEF_ID, DEF_ID,caseDetail,false, Sets.newHashSet(getOffenceDetail(VALID_OFFENCE_DETAIL_ID))));
        return caseDetail;
    }

    private OffenceDetail getOffenceDetail(UUID uuid) {
        return new OffenceDetail.OffenceDetailBuilder()
                .setId(uuid)
                .setPoliceOffenceId("")
                .setArrestDate(LocalDate.now())
                .setCategory("")
                .setChargeDate(LocalDate.now())
                .setSequenceNumber(123)
                .setCode("")
                .setDescription("")
                .setCpr(null)
                .setPlea("")
                .setReason("")
                .setSequenceNumber(1)
                .setStartDate(LocalDate.now())
                .setWording("")
                .build();
    }

}
