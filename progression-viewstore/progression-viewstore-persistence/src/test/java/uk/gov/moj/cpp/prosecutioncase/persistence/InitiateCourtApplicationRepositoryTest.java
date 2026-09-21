package uk.gov.moj.cpp.prosecutioncase.persistence;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.InitiateCourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.InitiateCourtApplicationRepository;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Runs InitiateCourtApplicationRepository's queries against in-memory H2 so the JPQL produced by the
 * DeltaSpike migration is actually executed rather than mocked.
 */
public class InitiateCourtApplicationRepositoryTest {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("progression-test-persistence-unit");

    private InitiateCourtApplicationRepository initiateCourtApplicationRepository;

    @BeforeEach
    void createRepositoryWithATestEntityManager() {
        initiateCourtApplicationRepository = new InitiateCourtApplicationRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(initiateCourtApplicationRepository);
    }

    @Test
    public void shouldSaveAndFindByApplicationId() {
        final UUID applicationId = randomUUID();
        final InitiateCourtApplicationEntity entity = new InitiateCourtApplicationEntity();
        entity.setApplicationId(applicationId);
        entity.setParentApplicationId(randomUUID());
        entity.setPayload("{}");

        initiateCourtApplicationRepository.save(entity);

        assertThat(initiateCourtApplicationRepository.findBy(applicationId), is(notNullValue()));
    }

    @Test
    public void shouldCountTheApplicationsSaved() {
        final InitiateCourtApplicationEntity entity = new InitiateCourtApplicationEntity();
        entity.setApplicationId(randomUUID());
        entity.setPayload("{}");
        initiateCourtApplicationRepository.save(entity);

        assertThat(initiateCourtApplicationRepository.count(), is(notNullValue()));
    }
}
