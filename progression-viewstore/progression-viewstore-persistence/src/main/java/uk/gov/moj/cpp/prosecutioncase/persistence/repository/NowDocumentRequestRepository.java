package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.NowDocumentRequestEntity;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class NowDocumentRequestRepository extends JpaEntityRepository<NowDocumentRequestEntity, UUID> {

    public NowDocumentRequestRepository() {
        super(NowDocumentRequestEntity.class);
    }

    @Override
    protected UUID idOf(final NowDocumentRequestEntity entity) {
        return entity.getMaterialId();
    }

    public List<NowDocumentRequestEntity> findByRequestId(final UUID requestId) {
        return entityManager.createQuery(
                        "select e from NowDocumentRequestEntity e where e.requestId = :requestId", NowDocumentRequestEntity.class)
                .setParameter("requestId", requestId)
                .getResultList();
    }

    public List<NowDocumentRequestEntity> findByHearingId(final UUID hearingId) {
        return entityManager.createQuery(
                        "select e from NowDocumentRequestEntity e where e.hearingId = :hearingId", NowDocumentRequestEntity.class)
                .setParameter("hearingId", hearingId)
                .getResultList();
    }
}
