package uk.gov.moj.cpp.prosecutioncase.persistence;

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
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * DB integration tests for {@link ProsecutionCaseEntity} class
 */


@RunWith(CdiTestRunner.class)
public class ProsecutionCaseRepositoryTest {

    private static final String PAYLOAD = "{\"defendants\":[],\"id\":\"bd947fa9-1eac-4edc-9452-382cd57fac2f\",\"initiationCode\":\"J\",\"originatingOrganisation\":\"G01FT01AB\",\"statementOfFacts\":\"You did it\",\"statementOfFactsWelsh\":\"You did it in Welsh\"}";
    private static final UUID CASE_ID_ONE = UUID.randomUUID();

    @Inject
    private ProsecutionCaseRepository repository;

    @Test
    public void shouldFindOptionalBy() throws Exception {
        //given
        repository.save(getProsecutionCase());

        final ProsecutionCaseEntity actual = repository.findByCaseId(CASE_ID_ONE);
        assertNotNull("Should not be null", actual);
        assertEquals(CASE_ID_ONE, actual.getCaseId());
    }


    private ProsecutionCaseEntity getProsecutionCase() {
        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(CASE_ID_ONE);
        prosecutionCaseEntity.setPayload(PAYLOAD);

        return prosecutionCaseEntity;
    }


}
