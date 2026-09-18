package uk.gov.moj.cpp.progression.persistence.repository;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The base class every repository gained when DeltaSpike Data was dropped for the Java 25 stack.
 *
 * It is exercised through a real subclass against in-memory H2 rather than mocked, because what
 * matters is the behaviour DeltaSpike used to provide - particularly that saving a new entity leaves
 * the caller's own instance managed - and a mocked EntityManager would assert nothing about that.
 *
 * This test lives in the repository package so it can reach the protected entityManager() accessor
 * that subclasses use.
 */
public class JpaEntityRepositoryTest {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("progression-test-persistence-unit");

    private CaseProgressionDetailRepository caseProgressionDetailRepository;

    @BeforeEach
    void createRepositoryWithATestEntityManager() {
        caseProgressionDetailRepository = new CaseProgressionDetailRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(caseProgressionDetailRepository);
    }

    private CaseProgressionDetail caseWith(final UUID caseId, final String caseUrn) {
        final CaseProgressionDetail caseProgressionDetail = new CaseProgressionDetail();
        caseProgressionDetail.setCaseId(caseId);
        caseProgressionDetail.setCaseUrn(caseUrn);
        return caseProgressionDetail;
    }

    private long rowsInTheDatabaseFor(final UUID caseId) {
        return ((Number) caseProgressionDetailRepository.entityManager()
                .createNativeQuery("select count(*) from CaseProgressionDetail where caseid = ?1")
                .setParameter(1, caseId)
                .getSingleResult()).longValue();
    }

    /**
     * The reason save() persists rather than merges a new entity. DeltaSpike left the instance passed
     * in managed; merge would hand back a managed copy and leave the caller holding a detached one,
     * so any code that saves an entity and carries on using its own reference would silently stop
     * seeing its own changes.
     */
    @Test
    public void shouldLeaveTheCallersOwnInstanceManagedWhenSavingANewEntity() {
        final UUID caseId = randomUUID();
        final CaseProgressionDetail caseProgressionDetail = caseWith(caseId, "URN-NEW");

        final CaseProgressionDetail saved = caseProgressionDetailRepository.save(caseProgressionDetail);

        assertThat(saved, is(sameInstance(caseProgressionDetail)));
        assertThat(caseProgressionDetailRepository.entityManager().contains(caseProgressionDetail), is(true));
    }

    @Test
    public void shouldUpdateRatherThanDuplicateWhenSavingAnEntityThatAlreadyExists() {
        final UUID caseId = randomUUID();
        caseProgressionDetailRepository.saveAndFlush(caseWith(caseId, "URN-FIRST"));

        final CaseProgressionDetail changed = caseWith(caseId, "URN-SECOND");
        caseProgressionDetailRepository.saveAndFlush(changed);

        assertThat(rowsInTheDatabaseFor(caseId), is(1L));
        assertThat(caseProgressionDetailRepository.findBy(caseId).getCaseUrn(), is("URN-SECOND"));
    }

    @Test
    public void shouldWriteTheEntityToTheDatabaseWhenSavingAndFlushing() {
        final UUID caseId = randomUUID();
        final CaseProgressionDetail caseProgressionDetail = caseWith(caseId, "URN-FLUSHED");

        final CaseProgressionDetail saved = caseProgressionDetailRepository.saveAndFlush(caseProgressionDetail);

        assertThat(saved, is(sameInstance(caseProgressionDetail)));
        assertThat(rowsInTheDatabaseFor(caseId), is(1L));
    }

    /**
     * Asserted with a native query on purpose: a JPQL or find() call would flush the persistence
     * context itself and pass whether or not flush() did anything.
     */
    @Test
    public void shouldPushPendingWritesToTheDatabaseWhenFlushed() {
        final UUID caseId = randomUUID();
        caseProgressionDetailRepository.save(caseWith(caseId, "URN-PENDING"));

        caseProgressionDetailRepository.flush();

        assertThat(rowsInTheDatabaseFor(caseId), is(1L));
    }

    @Test
    public void shouldExposeTheEntityManagerToSubclasses() {
        assertThat(caseProgressionDetailRepository.entityManager(), is(notNullValue()));
        assertThat(caseProgressionDetailRepository.entityManager().isOpen(), is(true));
    }

    @Test
    public void shouldFindASavedEntityById() {
        final UUID caseId = randomUUID();
        caseProgressionDetailRepository.saveAndFlush(caseWith(caseId, "URN-FIND"));

        final CaseProgressionDetail found = caseProgressionDetailRepository.findBy(caseId);

        assertThat(found, is(notNullValue()));
        assertThat(found.getCaseId(), is(caseId));
        assertThat(found.getCaseUrn(), is("URN-FIND"));
    }

    @Test
    public void shouldReturnNullWhenNoEntityHasThatId() {
        assertThat(caseProgressionDetailRepository.findBy(randomUUID()), is(nullValue()));
    }

    @Test
    public void shouldReturnTheEntityAsAnOptionalWhenItExists() {
        final UUID caseId = randomUUID();
        caseProgressionDetailRepository.saveAndFlush(caseWith(caseId, "URN-OPTIONAL"));

        final Optional<CaseProgressionDetail> found = caseProgressionDetailRepository.findOptionalBy(caseId);

        assertThat(found.isPresent(), is(true));
        assertThat(found.get().getCaseUrn(), is("URN-OPTIONAL"));
    }

    @Test
    public void shouldReturnAnEmptyOptionalWhenNoEntityHasThatId() {
        assertThat(caseProgressionDetailRepository.findOptionalBy(randomUUID()).isPresent(), is(false));
    }

    @Test
    public void shouldCountAndListWhatHasBeenSaved() {
        final UUID caseId = randomUUID();
        final Long countBefore = caseProgressionDetailRepository.count();

        caseProgressionDetailRepository.saveAndFlush(caseWith(caseId, "URN-COUNTED"));

        assertThat(caseProgressionDetailRepository.count(), is(countBefore + 1));
        assertThat(caseProgressionDetailRepository.findAll().stream()
                        .map(CaseProgressionDetail::getCaseId)
                        .filter(caseId::equals)
                        .toList(),
                contains(caseId));
    }

    @Test
    public void shouldDeleteAManagedEntity() {
        final UUID caseId = randomUUID();
        final CaseProgressionDetail caseProgressionDetail =
                caseProgressionDetailRepository.saveAndFlush(caseWith(caseId, "URN-REMOVED"));

        caseProgressionDetailRepository.removeAndFlush(caseProgressionDetail);

        assertThat(rowsInTheDatabaseFor(caseId), is(0L));
    }

    /**
     * DeltaSpike's attachAndRemove took a detached instance, re-attached it and deleted it. An
     * instance built afresh with the same id is detached, so this is the case that would fail if
     * remove() were given the argument directly rather than merging it first.
     */
    @Test
    public void shouldDeleteAnEntityThatIsNotAttachedToThePersistenceContext() {
        final UUID caseId = randomUUID();
        caseProgressionDetailRepository.saveAndFlush(caseWith(caseId, "URN-DETACHED"));
        caseProgressionDetailRepository.entityManager().clear();

        caseProgressionDetailRepository.attachAndRemove(caseWith(caseId, "URN-DETACHED"));
        caseProgressionDetailRepository.flush();

        assertThat(rowsInTheDatabaseFor(caseId), is(0L));
    }
}
