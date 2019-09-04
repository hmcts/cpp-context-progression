package uk.gov.moj.cpp.defence.association.persistence.repository;

import uk.gov.moj.cpp.defence.association.persistence.entity.DefenceAssociation;

import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface DefenceAssociationRepository extends EntityRepository<DefenceAssociation, UUID> {
    DefenceAssociation findByDefendantId(UUID defendantId);
}
