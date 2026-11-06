package uk.gov.moj.cpp.progression.readiness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ejb.Stateless;
import jakarta.jms.Message;
import jakarta.json.JsonObject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hibernate.Version;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.junit.jupiter.api.Test;

/** Compiles isolated API/processor probes; does not start CDI, JTA, a database or MI contracts. */
class TargetPlatformReadinessTest {
    @Test
    void runsOnTheRequiredJavaFeature() {
        assertEquals(25, Runtime.version().feature(), "Run this isolated probe on the Java 25 agent");
    }

    @Test
    void compilesJakartaApisAndExecutesGeneratedLombokMembers() throws ReflectiveOperationException {
        final List<Class<?>> jakartaTypes = List.of(ApplicationScoped.class, Stateless.class,
                Message.class, JsonObject.class, EntityManager.class, HttpServletRequest.class,
                Transactional.class, Response.class);
        assertEquals(8, jakartaTypes.size());
        assertTrue(jakartaTypes.stream().allMatch(type -> type.getName().startsWith("jakarta.")));
        assertEquals("generated", new GeneratedProbe("generated").getValue());
        assertNotNull(EntityManagerFactory.class.getMethod("getSchemaManager"));
        assertNotNull(Class.forName("uk.gov.justice.services.persistence.EntityManagerProducer"));
    }

    @Test
    void loadsTheRequiredHibernateProviderMajorWithoutOverridingManagement() {
        final PersistenceProvider provider = new HibernatePersistenceProvider();
        assertNotNull(provider);
        assertEquals("7", Version.getVersionString().split("\\.")[0],
                () -> "Readiness requires Hibernate 7; resolved provider is " + Version.getVersionString());
    }

    @Getter
    @AllArgsConstructor
    private static class GeneratedProbe {
        private final String value;
    }
}
