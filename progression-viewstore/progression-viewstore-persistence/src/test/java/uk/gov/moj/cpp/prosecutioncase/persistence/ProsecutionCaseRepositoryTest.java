package uk.gov.moj.cpp.prosecutioncase.persistence;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * DB integration tests for {@link ProsecutionCaseEntity} class
 */

public class ProsecutionCaseRepositoryTest {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("progression-test-persistence-unit");

    private static final String PAYLOAD = "{\"defendants\":[],\"id\":\"bd947fa9-1eac-4edc-9452-382cd57fac2f\",\"initiationCode\":\"J\",\"originatingOrganisation\":\"G01FT01AB\",\"statementOfFacts\":\"You did it\",\"statementOfFactsWelsh\":\"You did it in Welsh\"}";
    private static final UUID CASE_ID_ONE = UUID.randomUUID();

    private ProsecutionCaseRepository repository;

    @BeforeEach
    void createRepositoriesWithATestEntityManager() {
        repository = new ProsecutionCaseRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(repository);
    }

    @Test
    public void shouldFindOptionalBy() {
        //given
        repository.save(getProsecutionCase());

        final ProsecutionCaseEntity actual = repository.findByCaseId(CASE_ID_ONE);
        assertNotNull(actual, "Should not be null");
        assertEquals(CASE_ID_ONE, actual.getCaseId());
    }

    /**
     * Unlike findByCaseId, this one returns null instead of throwing when nothing matches, which is
     * the whole reason it exists alongside it.
     */
    @Test
    public void shouldFindACaseByIdWithoutThrowingWhenItIsMissing() {
        repository.saveAndFlush(getProsecutionCase());

        assertEquals(CASE_ID_ONE, repository.findOptionalByCaseId(CASE_ID_ONE).getCaseId());
        assertNull(repository.findOptionalByCaseId(randomUUID()), "Should be null for an unknown case");
    }

    @Test
    public void shouldFindEveryCaseInAGroup() {
        final UUID groupId = randomUUID();
        repository.saveAndFlush(caseInGroup(randomUUID(), groupId));
        repository.saveAndFlush(caseInGroup(randomUUID(), groupId));
        repository.saveAndFlush(caseInGroup(randomUUID(), randomUUID()));

        final List<ProsecutionCaseEntity> found = repository.findByGroupId(groupId);

        assertEquals(2, found.size());
        assertEquals(List.of(groupId), found.stream().map(ProsecutionCaseEntity::getGroupId).distinct().toList());
    }

    @Test
    public void shouldReturnNothingWhenNoCaseIsInThatGroup() {
        assertEquals(0, repository.findByGroupId(randomUUID()).size());
    }

    @Test
    public void shouldFindOnlyTheCasesWhoseIdsWereAsked() {
        final UUID wantedCaseId = randomUUID();
        final UUID otherWantedCaseId = randomUUID();
        repository.saveAndFlush(caseInGroup(wantedCaseId, randomUUID()));
        repository.saveAndFlush(caseInGroup(otherWantedCaseId, randomUUID()));
        repository.saveAndFlush(caseInGroup(randomUUID(), randomUUID()));

        final List<ProsecutionCaseEntity> found =
                repository.findByProsecutionCaseIds(List.of(wantedCaseId, otherWantedCaseId));

        assertEquals(2, found.size());
        assertEquals(Set.of(wantedCaseId, otherWantedCaseId),
                found.stream().map(ProsecutionCaseEntity::getCaseId).collect(toSet()));
    }

    @Test
    public void shouldReturnNothingWhenNoneOfTheRequestedCaseIdsExist() {
        assertEquals(0, repository.findByProsecutionCaseIds(List.of(randomUUID())).size());
    }

    private ProsecutionCaseEntity caseInGroup(final UUID caseId, final UUID groupId) {
        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(caseId);
        prosecutionCaseEntity.setGroupId(groupId);
        prosecutionCaseEntity.setPayload(PAYLOAD);
        return prosecutionCaseEntity;
    }

    private ProsecutionCaseEntity getProsecutionCase() {
        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(CASE_ID_ONE);
        prosecutionCaseEntity.setPayload(PAYLOAD);

        return prosecutionCaseEntity;
    }

}
