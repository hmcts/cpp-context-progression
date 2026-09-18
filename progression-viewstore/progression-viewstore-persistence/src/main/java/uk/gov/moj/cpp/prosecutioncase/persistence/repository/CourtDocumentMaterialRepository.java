package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentMaterialEntity;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CourtDocumentMaterialRepository extends JpaEntityRepository<CourtDocumentMaterialEntity, UUID> {

    public CourtDocumentMaterialRepository() {
        super(CourtDocumentMaterialEntity.class);
    }

    @Override
    protected UUID idOf(final CourtDocumentMaterialEntity entity) {
        return entity.getMaterialId();
    }

    /**
     * The DeltaSpike findOptionalBy prefix meant SingleResultType.OPTIONAL, so this returns null
     * when there is no match rather than throwing. The declared return type is the entity, not
     * Optional, so the contract is preserved as-is.
     */
    public CourtDocumentMaterialEntity findOptionalByCourtDocumentId(final UUID courtDocumentId) {
        return entityManager.createQuery(
                        "select e from CourtDocumentMaterialEntity e where e.courtDocumentId = :courtDocumentId",
                        CourtDocumentMaterialEntity.class)
                .setParameter("courtDocumentId", courtDocumentId)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }
}
