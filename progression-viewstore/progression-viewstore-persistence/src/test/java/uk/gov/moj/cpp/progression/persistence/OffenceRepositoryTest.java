package uk.gov.moj.cpp.progression.persistence;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.progression.persistence.repository.CaseProgressionDetailRepository;
import uk.gov.moj.cpp.progression.persistence.repository.DefendantRepository;
import uk.gov.moj.cpp.progression.persistence.repository.OffenceRepository;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import com.google.common.collect.Sets;
import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * DB integration tests for {@link OffenceRepository} class
 * @deprecated
 */

@Deprecated
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

        final OffenceDetail actual = offenceRepository.findBy(OFFENCE_ID_ONE);
        assertNotNull(actual, "Should not be null");
        assertEquals(OFFENCE_ID_ONE, actual.getId());
    }

    @Test
    public void shouldReturnOffenceInOrder() throws Exception {
        //given
        caseRepository.save(getCaseWithDefendantOffences());
        final Defendant defendant=defendantRepository.findByDefendantId(DEF_ID);
        final List<OffenceDetail> offenceDetails =
                        offenceRepository.findByDefendantOrderByOrderIndex(defendant);
        final List<UUID>  offenceIds = Arrays.asList(OFFENCE_ID_TWO,OFFENCE_ID_TWO,OFFENCE_ID_THREE);
        final long count = offenceDetails.stream().map(item -> offenceIds.contains(item)).count();
        assertTrue(count == 3);
    }

    private CaseProgressionDetail getCaseWithDefendantOffences() {
        final CaseProgressionDetail caseDetail = new CaseProgressionDetail();
        caseDetail.setCaseId(CASE_ID_ONE);
        caseDetail.setCaseUrn(CASEURN);
        caseDetail.addDefendant(new Defendant(DEF_ID,caseDetail,false, Sets.newHashSet(getOffenceDetail(OFFENCE_ID_ONE,1),getOffenceDetail(OFFENCE_ID_TWO,2),getOffenceDetail(OFFENCE_ID_THREE,3))));
        return caseDetail;
    }

    private OffenceDetail getOffenceDetail(final UUID uuid, final int orderIndex) {
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
