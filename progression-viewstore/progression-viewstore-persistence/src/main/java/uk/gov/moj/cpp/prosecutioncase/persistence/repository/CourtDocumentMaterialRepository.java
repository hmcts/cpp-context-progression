package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentMaterialEntity;

import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface CourtDocumentMaterialRepository extends EntityRepository<CourtDocumentMaterialEntity, UUID> {

    CourtDocumentMaterialEntity findOptionalByCourtDocumentId(final UUID courtDocumentId);

}
