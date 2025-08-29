package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentIndexEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Modifying;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;
import org.apache.deltaspike.data.api.SingleResultType;

@Repository
public interface CourtDocumentIndexRepository extends EntityRepository<CourtDocumentIndexEntity, UUID> {

    @Query(value = "from CourtDocumentIndexEntity cdie where cdie.prosecutionCaseId =:caseId and cdie.defendantId=:defendantId and cdie.courtDocument.courtDocumentId=:courtDocumentId",
            singleResult = SingleResultType.OPTIONAL)
    CourtDocumentIndexEntity findByCaseIdDefendantIdAndCaseDocumentId(@QueryParam("caseId") UUID caseId, @QueryParam("defendantId") UUID defendantId,
                                                                      @QueryParam("courtDocumentId") UUID courtDocumentId);

    @Query(value = "select di from CourtDocumentIndexEntity di, CourtDocumentMaterialEntity dm where dm.materialId =:materialId and dm.courtDocumentId=di.courtDocument.courtDocumentId",
            singleResult = SingleResultType.OPTIONAL)
    Optional<CourtDocumentIndexEntity> findByMaterialId(@QueryParam("materialId") UUID materialId);

    @Modifying
    @Query(value = "update CourtDocumentIndexEntity cdie set cdie.applicationId =:newApplicationId where  cdie.applicationId =:applicationId")
    void updateApplicationIdByApplicationId(@QueryParam("newApplicationId") UUID newApplicationId, @QueryParam("applicationId") UUID applicationId);

    List<CourtDocumentIndexEntity> findByProsecutionCaseIdAndDefendantId( UUID prosecutionCaseId, UUID defendantId);
}
