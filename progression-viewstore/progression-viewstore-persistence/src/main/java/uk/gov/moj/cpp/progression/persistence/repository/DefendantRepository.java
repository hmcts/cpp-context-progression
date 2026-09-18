package uk.gov.moj.cpp.progression.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.entity.Defendant;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * @deprecated
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
@ApplicationScoped
public class DefendantRepository extends JpaEntityRepository<Defendant, UUID> {

    public DefendantRepository() {
        super(Defendant.class);
    }

    @Override
    protected UUID idOf(final Defendant entity) {
        return entity.getDefendantId();
    }

    /**
     * Throws NoResultException when absent, matching the DeltaSpike derived finder's default.
     */
    public Defendant findByDefendantId(final UUID defendantId) {
        return entityManager.createQuery(
                        "select d from Defendant d where d.defendantId = :defendantId", Defendant.class)
                .setParameter("defendantId", defendantId)
                .getSingleResult();
    }
}
