package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PrisonCourtRegisterEntity;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface PrisonCourtRegisterRepository extends EntityRepository<PrisonCourtRegisterEntity, UUID> {
    List<PrisonCourtRegisterEntity> findByCourtCentreId(final UUID courtCentreId);
}
