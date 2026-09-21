package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.DefendantRequestEntity;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DefendantRequestRepository extends JpaEntityRepository<DefendantRequestEntity, UUID> {

    public DefendantRequestRepository() {
        super(DefendantRequestEntity.class);
    }

    @Override
    protected UUID idOf(final DefendantRequestEntity entity) {
        return entity.getDefendantId();
    }
}
