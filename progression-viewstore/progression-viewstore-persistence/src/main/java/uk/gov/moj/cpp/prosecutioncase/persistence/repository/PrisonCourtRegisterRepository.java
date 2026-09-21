package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PrisonCourtRegisterEntity;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PrisonCourtRegisterRepository extends JpaEntityRepository<PrisonCourtRegisterEntity, UUID> {

    public PrisonCourtRegisterRepository() {
        super(PrisonCourtRegisterEntity.class);
    }

    @Override
    protected UUID idOf(final PrisonCourtRegisterEntity entity) {
        return entity.getId();
    }

    public List<PrisonCourtRegisterEntity> findByCourtCentreId(final UUID courtCentreId) {
        return entityManager.createQuery(
                        "select e from PrisonCourtRegisterEntity e where e.courtCentreId = :courtCentreId",
                        PrisonCourtRegisterEntity.class)
                .setParameter("courtCentreId", courtCentreId)
                .getResultList();
    }

    /**
     * Derived single-result finder, so it throws NoResultException when absent.
     */
    public PrisonCourtRegisterEntity findById(final UUID id) {
        return entityManager.createQuery(
                        "select e from PrisonCourtRegisterEntity e where e.id = :id",
                        PrisonCourtRegisterEntity.class)
                .setParameter("id", id)
                .getSingleResult();
    }

    /**
     * Native query, kept verbatim: it reaches into the jsonb payload, which has no JPQL equivalent.
     * The DeltaSpike positional parameters ?1..?3 become JPA's 1-based positional parameters.
     */
    public PrisonCourtRegisterEntity findByCourtCentreIdAndHearingIdAndDefendantId(final UUID courtCentreId,
                                                                                  final String hearingId,
                                                                                  final String defendantId) {
        return (PrisonCourtRegisterEntity) entityManager.createNativeQuery(
                        "select * FROM prison_court_register p WHERE p.court_centre_id = ?1"
                                + " and cast(p.payload as jsonb)->>'hearingId' = ?2"
                                + " and (cast(p.payload as jsonb)->'defendant')->>'masterDefendantId' = ?3"
                                + " and p.file_id is null",
                        PrisonCourtRegisterEntity.class)
                .setParameter(1, courtCentreId)
                .setParameter(2, hearingId)
                .setParameter(3, defendantId)
                .getSingleResult();
    }
}
