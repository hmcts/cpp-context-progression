package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingResultLineEntity;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class HearingResultLineEntityRepository extends JpaEntityRepository<HearingResultLineEntity, UUID> {

    public HearingResultLineEntityRepository() {
        super(HearingResultLineEntity.class);
    }

    @Override
    protected UUID idOf(final HearingResultLineEntity entity) {
        return entity.getId();
    }
}
