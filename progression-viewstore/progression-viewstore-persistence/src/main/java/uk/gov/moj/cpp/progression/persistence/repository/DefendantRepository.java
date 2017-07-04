package uk.gov.moj.cpp.progression.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.entity.Defendant;

import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface DefendantRepository extends EntityRepository<Defendant, UUID> {

    Defendant findByDefendantId(UUID defendantId);
}
