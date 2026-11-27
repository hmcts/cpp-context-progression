package uk.gov.moj.cpp;

import java.lang.reflect.Field;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class HibernateTestEntityManagerProvider implements BeforeAllCallback,
        BeforeEachCallback, AfterEachCallback, AfterAllCallback {
    private final String persistenceUnit;
    private EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;

    public HibernateTestEntityManagerProvider(final String persistenceUnit) {
        this.persistenceUnit = persistenceUnit;
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        entityManagerFactory = Persistence.createEntityManagerFactory(persistenceUnit);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if (entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().rollback();
        }
        entityManager.close();
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public void injectEntityManagerInto(final Object repository) {
        injectEntityManagerInto(repository, "entityManager");
    }

    public void injectEntityManagerInto(final Object repository, String fieldName) {
        Class<?> type = repository.getClass();
        while (type != null && type != Object.class) {
            for (final Field field : type.getDeclaredFields()) {
                if (EntityManager.class.isAssignableFrom(field.getType())) {
                    try {
                        field.setAccessible(true);
                        field.set(repository, entityManager);
                        return;
                    } catch (final IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
            type = type.getSuperclass();
        }
        throw new IllegalStateException("No EntityManager field found on" + repository.getClass().getName());
    }
}
