package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentIndexEntity;

import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
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
}
