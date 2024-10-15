package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.DefendantLAAAssociationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.DefendantLAAKey;

import java.util.List;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface DefendantLAAAssociationRepository extends EntityRepository<DefendantLAAAssociationEntity, DefendantLAAKey> {

     @Query(value = "from DefendantLAAAssociationEntity entity where entity.id.laaContractNumber in (:laaContractNumber) and entity.isAssociatedByLAA is false")
     public abstract List<DefendantLAAAssociationEntity> findByLAAContractNUmber(@QueryParam("laaContractNumber") String laaContractNumber);


}
