package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.InitiateCourtApplicationEntity;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class InitiateCourtApplicationRepository extends JpaEntityRepository<InitiateCourtApplicationEntity, UUID> {

    public InitiateCourtApplicationRepository() {
        super(InitiateCourtApplicationEntity.class);
    }

    @Override
    protected UUID idOf(final InitiateCourtApplicationEntity entity) {
        return entity.getApplicationId();
    }
}
