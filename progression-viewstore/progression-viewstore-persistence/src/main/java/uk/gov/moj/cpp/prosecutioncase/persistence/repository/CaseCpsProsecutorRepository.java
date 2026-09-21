package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseCpsProsecutorEntity;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CaseCpsProsecutorRepository extends JpaEntityRepository<CaseCpsProsecutorEntity, UUID> {

    public CaseCpsProsecutorRepository() {
        super(CaseCpsProsecutorEntity.class);
    }

    @Override
    protected UUID idOf(final CaseCpsProsecutorEntity entity) {
        return entity.getCaseId();
    }
}
