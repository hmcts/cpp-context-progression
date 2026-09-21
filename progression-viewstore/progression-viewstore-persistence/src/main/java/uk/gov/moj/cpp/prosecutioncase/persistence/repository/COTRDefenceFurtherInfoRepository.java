package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.COTRDefenceFurtherInfoEntity;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class COTRDefenceFurtherInfoRepository extends JpaEntityRepository<COTRDefenceFurtherInfoEntity, UUID> {

    public COTRDefenceFurtherInfoRepository() {
        super(COTRDefenceFurtherInfoEntity.class);
    }

    @Override
    protected UUID idOf(final COTRDefenceFurtherInfoEntity entity) {
        return entity.getId();
    }

    public List<COTRDefenceFurtherInfoEntity> findByCotrDefendantId(final UUID cotrDefendantId) {
        return entityManager.createQuery(
                        "select e from COTRDefenceFurtherInfoEntity e where e.cotrDefendantId = :cotrDefendantId", COTRDefenceFurtherInfoEntity.class)
                .setParameter("cotrDefendantId", cotrDefendantId)
                .getResultList();
    }
}
