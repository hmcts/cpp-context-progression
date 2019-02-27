package uk.gov.moj.cpp.progression.persistence.repository;


import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.entity.OffenceDetail;

/**
 * Repository for {@link OffenceDetail}
 * @deprecated
 */
@SuppressWarnings("squid:S1133")
@Deprecated
@Repository
public interface OffenceRepository extends EntityRepository<OffenceDetail, UUID> {

    List<OffenceDetail> findByDefendantOrderByOrderIndex(Defendant defendant);
}
