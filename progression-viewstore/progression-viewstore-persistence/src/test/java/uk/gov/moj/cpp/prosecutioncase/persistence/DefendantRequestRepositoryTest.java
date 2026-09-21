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
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.DefendantRequestEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.DefendantRequestRepository;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Runs DefendantRequestRepository's queries against in-memory H2 so the JPQL produced by the
 * DeltaSpike migration is actually executed rather than mocked.
 */
public class DefendantRequestRepositoryTest {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("progression-test-persistence-unit");

    private DefendantRequestRepository defendantRequestRepository;

    @BeforeEach
    void createRepositoryWithATestEntityManager() {
        defendantRequestRepository = new DefendantRequestRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(defendantRequestRepository);
    }

    @Test
    public void shouldSaveAndFindByDefendantId() {
        final UUID defendantId = randomUUID();
        final DefendantRequestEntity entity = new DefendantRequestEntity();
        entity.setDefendantId(defendantId);
        entity.setProsecutionCaseId(randomUUID());
        entity.setPayload("{}");

        defendantRequestRepository.save(entity);

        final DefendantRequestEntity found = defendantRequestRepository.findBy(defendantId);
        assertThat(found, is(notNullValue()));
        assertThat(found.getPayload(), is("{}"));
    }

    @Test
    public void shouldReturnAnEmptyOptionalForAnUnknownDefendant() {
        assertThat(defendantRequestRepository.findOptionalBy(randomUUID()).isPresent(), is(false));
    }
}
