package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.COTRDetailsEntity;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class COTRDetailsRepository extends JpaEntityRepository<COTRDetailsEntity, UUID> {

    public COTRDetailsRepository() {
        super(COTRDetailsEntity.class);
    }

    @Override
    protected UUID idOf(final COTRDetailsEntity entity) {
        return entity.getId();
    }

    public List<COTRDetailsEntity> findByHearingId(final UUID hearingId) {
        return entityManager.createQuery(
                        "select e from COTRDetailsEntity e where e.hearingId = :hearingId", COTRDetailsEntity.class)
                .setParameter("hearingId", hearingId)
                .getResultList();
    }

    public List<COTRDetailsEntity> findByProsecutionCaseId(final UUID prosecutionCaseId) {
        return entityManager.createQuery(
                        "select e from COTRDetailsEntity e where e.prosecutionCaseId = :prosecutionCaseId", COTRDetailsEntity.class)
                .setParameter("prosecutionCaseId", prosecutionCaseId)
                .getResultList();
    }
}
