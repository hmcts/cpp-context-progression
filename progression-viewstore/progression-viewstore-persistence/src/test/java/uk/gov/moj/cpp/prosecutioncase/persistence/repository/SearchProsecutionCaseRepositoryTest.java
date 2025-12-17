package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.SearchProsecutionCaseEntity;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * DB integration tests for {@link SearchProsecutionCaseEntity} class
 */


@RunWith(CdiTestRunner.class)
public class SearchProsecutionCaseRepositoryTest {

    private final String searchTarget = "TEST12345 | John S Smith | 1977-01-01";

    private String searchCriteria;
    private UUID defedantId;
    private String caseId;

    @Inject
    private SearchProsecutionCaseRepository repository;

    @Before
    public void setUp() {
        defedantId = UUID.fromString("e1d32d9d-29ec-4934-a932-22a50f223966");
        searchCriteria = "%JOHN% %smith% %1977-01-01%".toLowerCase();
        caseId = "5002d600-af66-11e8-b568-0800200c9a66";
    }

    @Test
    public void shouldFindCaseByInputCriteria() {
        //given
        repository.save(searchProsecutionCase());

        //when
        final List<SearchProsecutionCaseEntity> actual = repository.findBySearchCriteria(searchCriteria); //"%John%"

        //then
        assertNotNull(actual, "Should not be null");
        assertEquals(1, actual.size());
        assertEquals(defedantId, actual.get(0).getDefendantId());
        assertEquals(searchTarget, actual.get(0).getSearchTarget());
        assertEquals("5002d600-af66-11e8-b568-0800200c9a66", actual.get(0).getCaseId());
        assertEquals("PAR-100", actual.get(0).getReference());
        assertEquals("John", actual.get(0).getDefendantFirstName());
        assertEquals("S", actual.get(0).getDefendantMiddleName());
        assertEquals("Smith", actual.get(0).getDefendantLastName());
        assertEquals("01-01-1977", actual.get(0).getDefendantDob());
        assertEquals("TFL", actual.get(0).getProsecutor());
        assertEquals("SJP Referral", actual.get(0).getStatus());
    }


    @Test
    public void shouldFindFirstByreference() {
        //given
        repository.save(searchProsecutionCase());

        //when
        final List<SearchProsecutionCaseEntity> actual = repository.findByCaseUrn("PAR-100"); //"%John%"

        //then
        assertNotNull(actual, "Should not be null");
        assertEquals(1, actual.size());
        assertEquals(defedantId, actual.get(0).getDefendantId());
        assertEquals(searchTarget, actual.get(0).getSearchTarget());
        assertEquals("5002d600-af66-11e8-b568-0800200c9a66", actual.get(0).getCaseId());
        assertEquals("PAR-100", actual.get(0).getReference());
        assertEquals("John", actual.get(0).getDefendantFirstName());
        assertEquals("S", actual.get(0).getDefendantMiddleName());
        assertEquals("Smith", actual.get(0).getDefendantLastName());
        assertEquals("01-01-1977", actual.get(0).getDefendantDob());
        assertEquals("TFL", actual.get(0).getProsecutor());
        assertEquals("SJP Referral", actual.get(0).getStatus());
    }

    @Test
    public void shouldFindByDefedantId() {
        //given
        repository.save(searchProsecutionCase());

        //when
        final SearchProsecutionCaseEntity actual = repository.findBy(defedantId);

        //then
        assertNotNull(actual, "Should not be null");
        assertEquals(defedantId, actual.getDefendantId());
    }

    @Test
    public void shouldFindByCaseId() {
        //given
        final SearchProsecutionCaseEntity entity = searchProsecutionCase();
        repository.save(entity);

        //when
        final SearchProsecutionCaseEntity actual = repository.findByCaseId(entity.getCaseId()).stream().findFirst().get();

        //then
        assertNotNull(actual, "Should not be null");
        assertEquals(caseId, actual.getCaseId());
    }

    private SearchProsecutionCaseEntity searchProsecutionCase() {
        final SearchProsecutionCaseEntity searchProsecutionCaseEntity = new SearchProsecutionCaseEntity();
        searchProsecutionCaseEntity.setDefendantId(defedantId);
        searchProsecutionCaseEntity.setCaseId(caseId);
        searchProsecutionCaseEntity.setReference("PAR-100");
        searchProsecutionCaseEntity.setDefendantFirstName("John");
        searchProsecutionCaseEntity.setDefendantMiddleName("S");
        searchProsecutionCaseEntity.setDefendantLastName("Smith");
        searchProsecutionCaseEntity.setDefendantDob("01-01-1977");
        searchProsecutionCaseEntity.setProsecutor("TFL");
        searchProsecutionCaseEntity.setStatus("SJP Referral");
        searchProsecutionCaseEntity.setStandaloneApplication(false);
        searchProsecutionCaseEntity.setSearchTarget(searchTarget);
        return searchProsecutionCaseEntity;
    }
}
