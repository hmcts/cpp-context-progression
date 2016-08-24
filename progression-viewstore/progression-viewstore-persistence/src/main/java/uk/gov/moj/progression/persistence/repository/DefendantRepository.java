package uk.gov.moj.progression.persistence.repository;

import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

import uk.gov.moj.cpp.progression.persistence.entity.Defendant;

@Repository
public interface DefendantRepository extends EntityRepository<Defendant, UUID> {

    Defendant findByDefendantId(UUID defendantId);
}
