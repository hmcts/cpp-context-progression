package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.COTRProsecutionFurtherInfoEntity;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class COTRProsecutionFurtherInfoRepository extends JpaEntityRepository<COTRProsecutionFurtherInfoEntity, UUID> {

    public COTRProsecutionFurtherInfoRepository() {
        super(COTRProsecutionFurtherInfoEntity.class);
    }

    @Override
    protected UUID idOf(final COTRProsecutionFurtherInfoEntity entity) {
        return entity.getId();
    }

    public List<COTRProsecutionFurtherInfoEntity> findByCotrId(final UUID cotrId) {
        return entityManager.createQuery(
                        "select e from COTRProsecutionFurtherInfoEntity e where e.cotrId = :cotrId", COTRProsecutionFurtherInfoEntity.class)
                .setParameter("cotrId", cotrId)
                .getResultList();
    }
}
