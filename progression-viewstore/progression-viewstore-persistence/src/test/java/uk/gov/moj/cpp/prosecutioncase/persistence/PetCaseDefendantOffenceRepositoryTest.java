package uk.gov.moj.cpp.prosecutioncase.persistence;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PetCaseDefendantOffence;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.PetCaseDefendantOffenceRepository;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class PetCaseDefendantOffenceRepositoryTest {

    @Inject
    private PetCaseDefendantOffenceRepository repository;

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
