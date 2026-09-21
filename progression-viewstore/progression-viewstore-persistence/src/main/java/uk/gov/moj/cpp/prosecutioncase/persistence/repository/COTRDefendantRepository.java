package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.COTRDefendantEntity;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class COTRDefendantRepository extends JpaEntityRepository<COTRDefendantEntity, UUID> {

    public COTRDefendantRepository() {
        super(COTRDefendantEntity.class);
    }

    @Override
    protected UUID idOf(final COTRDefendantEntity entity) {
        return entity.getId();
    }

    public List<COTRDefendantEntity> findByCotrId(final UUID cotrId) {
        return entityManager.createQuery(
                        "select e from COTRDefendantEntity e where e.cotrId = :cotrId", COTRDefendantEntity.class)
                .setParameter("cotrId", cotrId)
                .getResultList();
    }

    public List<COTRDefendantEntity> findByCotrIdAndDefendantId(final UUID cotrId, final UUID defendantId) {
        return entityManager.createQuery(
                        "select e from COTRDefendantEntity e where e.cotrId = :cotrId and e.defendantId = :defendantId", COTRDefendantEntity.class)
                .setParameter("cotrId", cotrId)
                .setParameter("defendantId", defendantId)
                .getResultList();
    }
}
