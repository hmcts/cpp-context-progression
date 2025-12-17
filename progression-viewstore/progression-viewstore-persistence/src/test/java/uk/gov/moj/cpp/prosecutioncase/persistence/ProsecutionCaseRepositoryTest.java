package uk.gov.moj.cpp.prosecutioncase.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

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
    public void shouldFindOptionalBy() {
        //given
        repository.save(getProsecutionCase());

        final ProsecutionCaseEntity actual = repository.findByCaseId(CASE_ID_ONE);
        assertNotNull(actual, "Should not be null");
        assertEquals(CASE_ID_ONE, actual.getCaseId());
    }


    private ProsecutionCaseEntity getProsecutionCase() {
        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(CASE_ID_ONE);
        prosecutionCaseEntity.setPayload(PAYLOAD);

        return prosecutionCaseEntity;
    }


}
