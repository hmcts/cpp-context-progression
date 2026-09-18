package uk.gov.moj.cpp.prosecutioncase.persistence;

import static org.hamcrest.MatcherAssert.assertThat;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PetCaseDefendantOffence;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.PetCaseDefendantOffenceRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class PetCaseDefendantOffenceRepositoryTest {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("progression-test-persistence-unit");

    private PetCaseDefendantOffenceRepository repository;

    @BeforeEach
    void createRepositoriesWithATestEntityManager() {
        repository = new PetCaseDefendantOffenceRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(repository);
    }

    @Test
    public void shouldSaveAndReadPetCaseDefendantOffence() {
        UUID key = randomUUID();
        boolean isYouth = false;

        final PetCaseDefendantOffence petCaseDefendantOffence = new PetCaseDefendantOffence(key, key, randomUUID(), isYouth,randomUUID());
        repository.save(petCaseDefendantOffence);

        final PetCaseDefendantOffence persistedPet = repository.findBy(key);
        assertThat(persistedPet, is(notNullValue()));
        assertThat(persistedPet.getId(), is(petCaseDefendantOffence.getId()));
        assertThat(persistedPet.getCaseId(), is(petCaseDefendantOffence.getCaseId()));
    }

    @Test
    public void shouldFindEveryOffenceOnAPet() {
        final UUID petId = randomUUID();
        repository.saveAndFlush(new PetCaseDefendantOffence(randomUUID(), randomUUID(), petId, false, randomUUID()));
        repository.saveAndFlush(new PetCaseDefendantOffence(randomUUID(), randomUUID(), petId, true, randomUUID()));
        repository.saveAndFlush(new PetCaseDefendantOffence(randomUUID(), randomUUID(), randomUUID(), false, randomUUID()));

        final List<PetCaseDefendantOffence> found = repository.findByPetId(petId);

        assertThat(found, hasSize(2));
        assertThat(found.stream().map(PetCaseDefendantOffence::getPetId).distinct().toList(), contains(petId));
    }

    @Test
    public void shouldReturnNothingWhenNoOffenceIsOnThatPet() {
        assertThat(repository.findByPetId(randomUUID()), is(empty()));
    }

    @Test
    public void shouldFindEveryPetOffenceOnACase() {
        final UUID caseId = randomUUID();
        repository.saveAndFlush(new PetCaseDefendantOffence(randomUUID(), caseId, randomUUID(), false, randomUUID()));
        repository.saveAndFlush(new PetCaseDefendantOffence(randomUUID(), caseId, randomUUID(), false, randomUUID()));
        repository.saveAndFlush(new PetCaseDefendantOffence(randomUUID(), randomUUID(), randomUUID(), false, randomUUID()));

        final List<PetCaseDefendantOffence> found = repository.findByCaseId(caseId);

        assertThat(found, hasSize(2));
        assertThat(found.stream().map(PetCaseDefendantOffence::getCaseId).distinct().toList(), contains(caseId));
    }

    @Test
    public void shouldReturnNothingWhenTheCaseHasNoPetOffence() {
        assertThat(repository.findByCaseId(randomUUID()), is(empty()));
    }

    @Test
    public void shouldSaveAndReadPetCaseDefendantOffenceForYouth() {
        UUID key = randomUUID();
        boolean isYouth = true;

        final PetCaseDefendantOffence petCaseDefendantOffence = new PetCaseDefendantOffence(key, randomUUID(), randomUUID(), isYouth, randomUUID(), ZonedDateTime.now());
        repository.save(petCaseDefendantOffence);

        final PetCaseDefendantOffence persistedPet = repository.findBy(key);
        assertThat(persistedPet, is(notNullValue()));
        assertThat(persistedPet.getId(), is(petCaseDefendantOffence.getId()));
        assertThat(persistedPet.getCaseId(), is(petCaseDefendantOffence.getCaseId()));
        assertThat(persistedPet.getIsYouth(), is(true));
    }

}
