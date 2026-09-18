package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseNoteEntity;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CaseNoteRepository extends JpaEntityRepository<CaseNoteEntity, UUID> {

    public CaseNoteRepository() {
        super(CaseNoteEntity.class);
    }

    @Override
    protected UUID idOf(final CaseNoteEntity entity) {
        return entity.getId();
    }

    public List<CaseNoteEntity> findByCaseIdOrderByCreatedDateTimeDesc(final UUID caseId) {
        return entityManager.createQuery(
                        "select e from CaseNoteEntity e where e.caseId = :caseId order by e.createdDateTime desc",
                        CaseNoteEntity.class)
                .setParameter("caseId", caseId)
                .getResultList();
    }
}
