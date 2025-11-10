package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CivilFeeEntity;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface CivilFeeRepository extends EntityRepository<CivilFeeEntity, UUID> {

    @Query("from CivilFeeEntity c where c.feeId in (:feeIds)")
    List<CivilFeeEntity> findByFeeIds(@QueryParam("feeIds") List<UUID> feeIds);

}
