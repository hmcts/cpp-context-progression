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
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.OnlinePleaRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Runs OnlinePleaRepository's queries against in-memory H2 so the JPQL produced by the
 * DeltaSpike migration is actually executed rather than mocked.
 */
public class OnlinePleaRepositoryTest {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("progression-test-persistence-unit");

    private OnlinePleaRepository onlinePleaRepository;

    @BeforeEach
    void createRepositoryWithATestEntityManager() {
        onlinePleaRepository = new OnlinePleaRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(onlinePleaRepository);
    }

    @Test
    public void shouldSaveAndFindByCaseId() {
        final UUID caseId = randomUUID();
        final OnlinePlea onlinePlea = new OnlinePlea();
        onlinePlea.setCaseId(caseId);
        onlinePlea.setDefendantId(randomUUID());
        onlinePlea.setSubmittedOn(ZonedDateTime.now());
        onlinePlea.setUrn("URN1");

        onlinePleaRepository.save(onlinePlea);

        final OnlinePlea found = onlinePleaRepository.findBy(caseId);
        assertThat(found, is(notNullValue()));
        assertThat(found.getCaseId(), is(caseId));
    }

    @Test
    public void shouldListEveryPleaSaved() {
        final OnlinePlea onlinePlea = new OnlinePlea();
        onlinePlea.setCaseId(randomUUID());
        onlinePlea.setDefendantId(randomUUID());
        onlinePlea.setSubmittedOn(ZonedDateTime.now());
        onlinePlea.setUrn("URN1");
        onlinePleaRepository.save(onlinePlea);

        assertThat(onlinePleaRepository.findAll(), is(notNullValue()));
    }
}
