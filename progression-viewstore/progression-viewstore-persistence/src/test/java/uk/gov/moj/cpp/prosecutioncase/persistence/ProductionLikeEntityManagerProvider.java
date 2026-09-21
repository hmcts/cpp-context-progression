package uk.gov.moj.cpp.prosecutioncase.persistence;

import static java.util.Map.of;

import java.lang.reflect.Field;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/**
 * An H2-backed EntityManager configured the way the deployed persistence unit is, specifically with
 * strict JPA query compliance OFF.
 *
 * {@code HibernateTestEntityManagerProvider} bootstraps its EntityManagerFactory with a hardcoded
 * {@code hibernate.jpa.compliance.query=true}, which beats whatever the test persistence.xml says.
 * That is a stricter bar than production, so most repository tests can happily use it. A few queries
 * legitimately rely on HQL extensions that strict mode rejects - aliased fetch joins in
 * CourtDocumentRepository, row-value constructors in CourtRegisterRequestRepository - and testing
 * those under strict compliance would prove something about a configuration we do not deploy.
 *
 * Keeping the queries verbatim and relaxing the test to match production is deliberate: the
 * compliant rewrite of an aliased fetch join fetches the whole collection rather than only the
 * matching rows, which is a different query.
 */
public final class ProductionLikeEntityManagerProvider implements AutoCloseable {

    private static final String PERSISTENCE_UNIT = "progression-test-persistence-unit";

    private final EntityManagerFactory entityManagerFactory;
    private final EntityManager entityManager;

    public ProductionLikeEntityManagerProvider(final String isolatedDatabaseName) {
        final Map<String, String> overrides = of(
                "hibernate.jpa.compliance.query", "false",
                "jakarta.persistence.jdbc.url",
                "jdbc:h2:mem:" + isolatedDatabaseName + ";DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE");
        this.entityManagerFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT, overrides);
        this.entityManager = entityManagerFactory.createEntityManager();
    }

    public EntityManager entityManager() {
        return entityManager;
    }

    /**
     * Assigns the EntityManager onto a repository's inherited field, the same thing the framework's
     * provider does, so the repository under test behaves exactly as it does in the container.
     */
    public void injectEntityManagerInto(final Object repository) {
        Class<?> type = repository.getClass();
        while (type != null) {
            try {
                final Field field = type.getDeclaredField("entityManager");
                field.setAccessible(true);
                field.set(repository, entityManager);
                return;
            } catch (final NoSuchFieldException e) {
                type = type.getSuperclass();
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException("Could not inject the EntityManager into " + repository.getClass(), e);
            }
        }
        throw new IllegalStateException("No entityManager field found on " + repository.getClass());
    }

    public void beginTransaction() {
        entityManager.getTransaction().begin();
    }

    public void rollbackTransaction() {
        if (entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().rollback();
        }
    }

    @Override
    public void close() {
        entityManager.close();
        entityManagerFactory.close();
    }
}
