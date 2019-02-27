package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingResultLineEntity;

import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface HearingResultLineEntityRepository extends EntityRepository<HearingResultLineEntity, UUID> {

}
