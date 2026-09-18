package uk.gov.moj.cpp.progression.persistence.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Replaces DeltaSpike Data's {@code EntityRepository} for the Java 25 stack.
 *
 * DeltaSpike is not merely unmanaged on service-parent-pom 25.104.2 - it cannot work at all, because
 * its own {@code @Query} annotation is compiled against {@code javax.persistence.LockModeType}, which
 * does not exist on a Jakarta classpath. Every repository therefore becomes a concrete
 * {@code @ApplicationScoped} class extending this base.
 *
 * The built-ins below reproduce DeltaSpike's contracts rather than the naive JPA equivalents, because
 * callers and tests depend on the difference - see {@link #save(Object)}.
 *
 * No method here is {@code final}: a normal-scoped CDI bean must stay proxyable, and a non-private
 * final method fails the deploy with WELD-001480.
 */
public abstract class JpaEntityRepository<E, ID> {

    @PersistenceContext(unitName = "progression-persistence-unit")
    protected EntityManager entityManager;

    private final Class<E> entityType;

    protected JpaEntityRepository(final Class<E> entityType) {
        this.entityType = entityType;
    }

    /**
     * DeltaSpike persisted a new entity and left the instance passed in MANAGED; a plain
     * {@code merge} returns a managed copy and leaves the argument DETACHED. Code that saves an
     * entity and then keeps using the same reference relies on the former, so the new-entity case
     * persists rather than merges.
     */
    public E save(final E entity) {
        final ID id = idOf(entity);
        if (id != null && entityManager.find(entityType, id) != null) {
            return entityManager.merge(entity);
        }
        entityManager.persist(entity);
        return entity;
    }

    public E saveAndFlush(final E entity) {
        final E saved = save(entity);
        entityManager.flush();
        return saved;
    }

    public E findBy(final ID id) {
        return entityManager.find(entityType, id);
    }

    public Optional<E> findOptionalBy(final ID id) {
        return Optional.ofNullable(entityManager.find(entityType, id));
    }

    public List<E> findAll() {
        return entityManager.createQuery("select e from " + entityType.getSimpleName() + " e", entityType)
                .getResultList();
    }

    public Long count() {
        return entityManager.createQuery("select count(e) from " + entityType.getSimpleName() + " e", Long.class)
                .getSingleResult();
    }

    public void remove(final E entity) {
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
    }

    public void removeAndFlush(final E entity) {
        remove(entity);
        entityManager.flush();
    }

    /**
     * DeltaSpike's attachAndRemove re-attached a detached instance before deleting it, which is what
     * {@link #remove(Object)} already does here.
     */
    public void attachAndRemove(final E entity) {
        remove(entity);
    }

    public void flush() {
        entityManager.flush();
    }

    protected EntityManager entityManager() {
        return entityManager;
    }

    /**
     * The id of an entity, used only to decide persist-vs-merge in {@link #save(Object)}. Viewstore
     * entities carry assigned UUIDs, so this is never null in practice and the find() check is what
     * actually distinguishes new from existing.
     */
    protected abstract ID idOf(E entity);
}
