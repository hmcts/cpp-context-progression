package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.OnlinePlea;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OnlinePleaRepository extends JpaEntityRepository<OnlinePlea, UUID> {

    public OnlinePleaRepository() {
        super(OnlinePlea.class);
    }

    @Override
    protected UUID idOf(final OnlinePlea entity) {
        return entity.getCaseId();
    }
}
