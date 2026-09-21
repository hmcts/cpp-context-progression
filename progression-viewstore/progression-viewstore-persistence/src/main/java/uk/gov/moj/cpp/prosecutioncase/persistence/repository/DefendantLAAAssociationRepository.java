package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.DefendantLAAAssociationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.DefendantLAAKey;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DefendantLAAAssociationRepository
        extends JpaEntityRepository<DefendantLAAAssociationEntity, DefendantLAAKey> {

    public DefendantLAAAssociationRepository() {
        super(DefendantLAAAssociationEntity.class);
    }

    @Override
    protected DefendantLAAKey idOf(final DefendantLAAAssociationEntity entity) {
        return entity.getDefendantLAAKey();
    }

    public List<DefendantLAAAssociationEntity> findByLAAContractNUmber(final String laaContractNumber) {
        return entityManager.createQuery(
                        "select entity from DefendantLAAAssociationEntity entity"
                                + " where entity.id.laaContractNumber in (:laaContractNumber)"
                                + " and entity.isAssociatedByLAA is false",
                        DefendantLAAAssociationEntity.class)
                .setParameter("laaContractNumber", laaContractNumber)
                .getResultList();
    }
}
