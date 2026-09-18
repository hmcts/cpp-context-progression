package uk.gov.moj.cpp.prosecutioncase.persistence;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.RelatedReference;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.RelatedReferenceRepository;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Runs RelatedReferenceRepository's queries against in-memory H2, so the JPQL the DeltaSpike migration
 * produced is executed rather than mocked.
 */
public class RelatedReferenceRepositoryTest {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("progression-test-persistence-unit");

    private RelatedReferenceRepository relatedReferenceRepository;

    @BeforeEach
    void createRepositoryWithATestEntityManager() {
        relatedReferenceRepository = new RelatedReferenceRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(relatedReferenceRepository);
    }

    private RelatedReference relatedReference(final UUID prosecutionCaseId, final String reference) {
        final RelatedReference relatedReference = new RelatedReference();
        relatedReference.setId(randomUUID());
        relatedReference.setProsecutionCaseId(prosecutionCaseId);
        relatedReference.setReference(reference);
        return relatedReference;
    }

    @Test
    public void shouldFindEveryReferenceForACase() {
        final UUID prosecutionCaseId = randomUUID();
        relatedReferenceRepository.save(relatedReference(prosecutionCaseId, "REF1"));
        relatedReferenceRepository.save(relatedReference(prosecutionCaseId, "REF2"));
        relatedReferenceRepository.save(relatedReference(randomUUID(), "OTHER"));

        final List<RelatedReference> found = relatedReferenceRepository.findByProsecutionCaseId(prosecutionCaseId);

        assertThat(found, hasSize(2));
    }

    @Test
    public void shouldReturnNothingForACaseWithNoReferences() {
        assertThat(relatedReferenceRepository.findByProsecutionCaseId(randomUUID()), is(empty()));
    }
}
