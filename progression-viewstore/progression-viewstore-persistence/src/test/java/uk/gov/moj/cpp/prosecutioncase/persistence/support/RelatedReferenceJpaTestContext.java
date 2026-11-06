package uk.gov.moj.cpp.prosecutioncase.persistence.support;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.RelatedReference;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Consumer;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceProviderResolverHolder;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

/**
 * An isolated, resource-local JPA fixture for the RelatedReference pilot.
 *
 * Uses the existing test classpath's Hibernate provider and H2 driver. It does not
 * start CDI, JTA or WildFly, and cannot establish container transaction behaviour.
 * Programmatic persistence-unit metadata avoids shadowing the application's
 * META-INF/persistence.xml or changing existing DeltaSpike container tests.
 */
public final class RelatedReferenceJpaTestContext implements AutoCloseable {

    private static final String HIBERNATE_PROVIDER = "org.hibernate.jpa.HibernatePersistenceProvider";

    private final EntityManagerFactory entityManagerFactory;
    private final EntityManager entityManager;

    public RelatedReferenceJpaTestContext() {
        final PersistenceProvider provider = PersistenceProviderResolverHolder
                .getPersistenceProviderResolver()
                .getPersistenceProviders()
                .stream()
                .filter(candidate -> HIBERNATE_PROVIDER.equals(candidate.getClass().getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "The managed Hibernate provider must be present on the test classpath"));

        entityManagerFactory = provider.createContainerEntityManagerFactory(
                new RelatedReferenceTestPersistenceUnit(), Collections.emptyMap());
        if (entityManagerFactory == null) {
            throw new IllegalStateException("Hibernate did not accept the test persistence unit");
        }
        try {
            entityManager = entityManagerFactory.createEntityManager();
        } catch (final RuntimeException | Error failure) {
            entityManagerFactory.close();
            throw failure;
        }
    }

    public EntityManager entityManager() {
        return entityManager;
    }

    public void inTransaction(final Consumer<EntityManager> action) {
        final EntityTransaction transaction = entityManager.getTransaction();
        if (transaction.isActive()) {
            throw new IllegalStateException("This resource-local fixture does not support nested transactions");
        }
        transaction.begin();
        try {
            action.accept(entityManager);
            transaction.commit();
        } catch (final RuntimeException | Error failure) {
            if (transaction.isActive()) {
                try {
                    transaction.rollback();
                } catch (final RuntimeException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
            }
            throw failure;
        }
    }

    @Override
    public void close() {
        try {
            if (entityManager.isOpen()) {
                try {
                    if (entityManager.getTransaction().isActive()) {
                        entityManager.getTransaction().rollback();
                    }
                } finally {
                    entityManager.close();
                }
            }
        } finally {
            if (entityManagerFactory.isOpen()) {
                entityManagerFactory.close();
            }
        }
    }

    private static final class RelatedReferenceTestPersistenceUnit implements PersistenceUnitInfo {

        private final Properties properties = new Properties();

        private RelatedReferenceTestPersistenceUnit() {
            properties.setProperty("javax.persistence.jdbc.driver", "org.h2.Driver");
            properties.setProperty("javax.persistence.jdbc.url",
                    "jdbc:h2:mem:related_reference_" + UUID.randomUUID().toString().replace("-", ""));
            properties.setProperty("javax.persistence.jdbc.user", "sa");
            properties.setProperty("javax.persistence.jdbc.password", "");
            properties.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
            properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
            properties.setProperty("hibernate.show_sql", "false");
            properties.setProperty("hibernate.archive.autodetection", "none");
        }

        @Override
        public String getPersistenceUnitName() {
            return "progression-related-reference-resource-local-test";
        }

        @Override
        public String getPersistenceProviderClassName() {
            return HIBERNATE_PROVIDER;
        }

        @Override
        public PersistenceUnitTransactionType getTransactionType() {
            return PersistenceUnitTransactionType.RESOURCE_LOCAL;
        }

        @Override
        public DataSource getJtaDataSource() {
            return null;
        }

        @Override
        public DataSource getNonJtaDataSource() {
            return null;
        }

        @Override
        public List<String> getMappingFileNames() {
            return Collections.emptyList();
        }

        @Override
        public List<URL> getJarFileUrls() {
            return Collections.emptyList();
        }

        @Override
        public URL getPersistenceUnitRootUrl() {
            return RelatedReferenceJpaTestContext.class.getProtectionDomain().getCodeSource().getLocation();
        }

        @Override
        public List<String> getManagedClassNames() {
            return Collections.singletonList(RelatedReference.class.getName());
        }

        @Override
        public boolean excludeUnlistedClasses() {
            return true;
        }

        @Override
        public SharedCacheMode getSharedCacheMode() {
            return SharedCacheMode.NONE;
        }

        @Override
        public ValidationMode getValidationMode() {
            return ValidationMode.NONE;
        }

        @Override
        public Properties getProperties() {
            return properties;
        }

        @Override
        public String getPersistenceXMLSchemaVersion() {
            return "2.1";
        }

        @Override
        public ClassLoader getClassLoader() {
            return RelatedReference.class.getClassLoader();
        }

        @Override
        public void addTransformer(final ClassTransformer transformer) {
            // This isolated fixture does not perform container bytecode enhancement.
        }

        @Override
        public ClassLoader getNewTempClassLoader() {
            return new ClassLoader(getClassLoader()) {
                // Separate loader identity; this fixture has no bytecode transformer.
            };
        }
    }
}
