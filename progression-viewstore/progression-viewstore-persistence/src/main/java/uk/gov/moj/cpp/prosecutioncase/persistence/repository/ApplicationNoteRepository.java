package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ApplicationNoteEntity;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ApplicationNoteRepository extends JpaEntityRepository<ApplicationNoteEntity, UUID> {

    public ApplicationNoteRepository() {
        super(ApplicationNoteEntity.class);
    }

    @Override
    protected UUID idOf(final ApplicationNoteEntity entity) {
        return entity.getId();
    }

    public List<ApplicationNoteEntity> findByApplicationIdOrderByCreatedDateTimeDesc(final UUID applicationId) {
        return entityManager.createQuery(
                        "select e from ApplicationNoteEntity e where e.applicationId = :applicationId"
                                + " order by e.createdDateTime desc",
                        ApplicationNoteEntity.class)
                .setParameter("applicationId", applicationId)
                .getResultList();
    }
}
