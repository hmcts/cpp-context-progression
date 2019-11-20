package uk.gov.moj.cpp.defence.association.persistence.repository;

import uk.gov.moj.cpp.defence.association.persistence.entity.DefenceAssociationDefendant;

import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface DefenceAssociationRepository extends EntityRepository<DefenceAssociationDefendant, UUID> {
    DefenceAssociationDefendant findByDefendantId(UUID defendantId);
}
