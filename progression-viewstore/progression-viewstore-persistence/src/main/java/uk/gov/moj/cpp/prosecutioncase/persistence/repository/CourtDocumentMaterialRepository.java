package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentMaterialEntity;

import java.util.UUID;

@Repository
public interface CourtDocumentMaterialRepository extends EntityRepository<CourtDocumentMaterialEntity, UUID> {

}
