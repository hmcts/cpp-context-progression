package uk.gov.moj.cpp.progression.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.entity.Defendant;

import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;
/**
 * @deprecated
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
@Repository
public interface DefendantRepository extends EntityRepository<Defendant, UUID> {

    Defendant findByDefendantId(UUID defendantId);
}
