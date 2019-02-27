package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentIndexEntity;

import java.util.UUID;

@Repository
public interface CourtDocumentIndexRepository extends EntityRepository<CourtDocumentIndexEntity, UUID> {

}
