package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CpsSendNotificationEntity;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CpsSendNotificationRepository extends JpaEntityRepository<CpsSendNotificationEntity, UUID> {

    public CpsSendNotificationRepository() {
        super(CpsSendNotificationEntity.class);
    }

    @Override
    protected UUID idOf(final CpsSendNotificationEntity entity) {
        return entity.getCourtDocumentId();
    }
}
