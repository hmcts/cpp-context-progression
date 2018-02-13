package uk.gov.moj.cpp.progression.persistence;

import com.google.common.collect.Sets;
import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.progression.persistence.repository.CaseProgressionDetailRepository;
import uk.gov.moj.cpp.progression.persistence.repository.DefendantRepository;
import uk.gov.moj.cpp.progression.persistence.repository.OffenceRepository;

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * DB integration tests for {@link OffenceRepository} class
 */
@RunWith(CdiTestRunner.class)
public class OffenceRepositoryTest  {

    private static final UUID OFFENCE_ID_ONE = randomUUID();
    private static final UUID OFFENCE_ID_TWO = randomUUID();
    private static final UUID OFFENCE_ID_THREE = randomUUID();
    private static final String COURT_CENTER = "Liverpool";
    private static final String CASEURN = "CASEURN";
    private static final UUID CASE_ID_ONE = UUID.randomUUID();
    private static final UUID DEF_ID = UUID.randomUUID();

    @Inject
    private OffenceRepository offenceRepository;

    @Inject
    private CaseProgressionDetailRepository caseRepository;

    @Inject
    private DefendantRepository defendantRepository;

    @Test
    public void shouldFindOptionalBy() throws Exception {
        //given
        caseRepository.save(getCaseWithDefendantOffences());

        OffenceDetail actual = offenceRepository.findBy(OFFENCE_ID_ONE);
        assertNotNull("Should not be null", actual);
        assertEquals(OFFENCE_ID_ONE, actual.getId());
    }

    @Test
    public void shouldReturnOffenceInOrder() throws Exception {
        //given
        caseRepository.save(getCaseWithDefendantOffences());
        Defendant defendant=defendantRepository.findByDefendantId(DEF_ID);
        List<OffenceDetail> offenceDetails = offenceRepository.findByDefendantOrderByOrderIndex(defendant);
        assertEquals(OFFENCE_ID_ONE, offenceDetails.get(0).getId());
        assertEquals(OFFENCE_ID_TWO, offenceDetails.get(1).getId());
        assertEquals(OFFENCE_ID_THREE, offenceDetails.get(2).getId());
    }

    private CaseProgressionDetail getCaseWithDefendantOffences() {
        CaseProgressionDetail caseDetail = new CaseProgressionDetail();
        caseDetail.setCaseId(CASE_ID_ONE);
        caseDetail.setCaseUrn(CASEURN);
        caseDetail.addDefendant(new Defendant(DEF_ID,caseDetail,false, Sets.newHashSet(getOffenceDetail(OFFENCE_ID_ONE,1),getOffenceDetail(OFFENCE_ID_TWO,2),getOffenceDetail(OFFENCE_ID_THREE,3))));
        return caseDetail;
    }

    private OffenceDetail getOffenceDetail(UUID uuid,int orderIndex) {
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
                .setOffencePlea(null)
                .setReason("")
                .setSequenceNumber(1)
                .setStartDate(LocalDate.now())
                .setWording("")
                .withOrderIndex(orderIndex)
                .build();
    }

}
