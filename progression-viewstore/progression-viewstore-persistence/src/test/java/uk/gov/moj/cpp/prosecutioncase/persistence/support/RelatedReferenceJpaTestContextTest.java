package uk.gov.moj.cpp.prosecutioncase.persistence.support;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.RelatedReference;

import java.util.UUID;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.Test;

class RelatedReferenceJpaTestContextTest {

    @Test
    void shouldCommitToTheDatabaseAndReadOutsideTheFirstLevelCache() {
        try (final RelatedReferenceJpaTestContext context = new RelatedReferenceJpaTestContext()) {
            final RelatedReference reference = reference(randomUUID());
            context.inTransaction(entityManager -> entityManager.persist(reference));

            context.entityManager().clear();

            final RelatedReference persisted = context.entityManager().find(RelatedReference.class, reference.getId());
            assertNotNull(persisted);
            assertEquals(reference.getProsecutionCaseId(), persisted.getProsecutionCaseId());
            assertEquals(reference.getReference(), persisted.getReference());
            assertEquals(1L, ((Number) context.entityManager()
                    .createNativeQuery("select count(*) from related_reference")
                    .getSingleResult()).longValue());
        }
    }

    @Test
    void shouldRollBackFlushedWritesWhenTheActionFails() {
        try (final RelatedReferenceJpaTestContext context = new RelatedReferenceJpaTestContext()) {
            final RelatedReference reference = reference(randomUUID());

            assertThrows(IllegalStateException.class, () -> context.inTransaction(entityManager -> {
                entityManager.persist(reference);
                entityManager.flush();
                throw new IllegalStateException("deliberate resource-local fixture failure");
            }));
            context.entityManager().clear();

            assertFalse(context.entityManager().getTransaction().isActive());
            assertNull(context.entityManager().find(RelatedReference.class, reference.getId()));
            assertEquals(0L, ((Number) context.entityManager()
                    .createNativeQuery("select count(*) from related_reference")
                    .getSingleResult()).longValue());
        }
    }

    @Test
    void shouldKeepFixturesIsolatedAndCloseTheirEntityManagers() {
        final EntityManager firstEntityManager;
        final EntityManager secondEntityManager;
        try (final RelatedReferenceJpaTestContext first = new RelatedReferenceJpaTestContext();
             final RelatedReferenceJpaTestContext second = new RelatedReferenceJpaTestContext()) {
            firstEntityManager = first.entityManager();
            secondEntityManager = second.entityManager();
            final RelatedReference reference = reference(randomUUID());
            first.inTransaction(entityManager -> entityManager.persist(reference));

            assertNull(second.entityManager().find(RelatedReference.class, reference.getId()));
        }

        assertFalse(firstEntityManager.isOpen());
        assertFalse(secondEntityManager.isOpen());
    }

    private RelatedReference reference(final UUID id) {
        final RelatedReference reference = new RelatedReference();
        reference.setId(id);
        reference.setProsecutionCaseId(randomUUID());
        reference.setReference("SYNTHETIC-RELATED-REFERENCE");
        return reference;
    }
}
