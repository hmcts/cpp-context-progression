package uk.gov.moj.cpp.defence.association.persistence.repository;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;
import uk.gov.moj.cpp.defence.association.persistence.entity.DefenceAssociationDefendant;

import java.util.UUID;

@Repository
public interface DefenceAssociationRepository extends EntityRepository<DefenceAssociationDefendant, UUID> {

    DefenceAssociationDefendant findByDefendantId(UUID defendantId);

}
