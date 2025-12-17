package uk.gov.moj.cpp.progression.persistence.repository;


import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.entity.OffenceDetail;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

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
