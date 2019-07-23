package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;

@Repository
public interface CourtDocumentRepository extends EntityRepository<CourtDocumentEntity, UUID> {

    @Query("select courtDocument FROM CourtDocumentIndexEntity where prosecution_case_id=:caseId")
    List<CourtDocumentEntity> findByProsecutionCaseId(@QueryParam("caseId") final UUID caseId);

    @Query("select courtDocument FROM CourtDocumentIndexEntity where defendant_id=:defendantId")
    List<CourtDocumentEntity> findByDefendantId(@QueryParam("defendantId") final UUID defendantId);

    @Query("select courtDocument FROM CourtDocumentIndexEntity where application_id=:applicationId")
    List<CourtDocumentEntity> findByApplicationId(@QueryParam("applicationId") final UUID applicationId);

    @Query("select courtDocument FROM CourtDocumentIndexEntity cdi where cdi.hearingId=:hearingId and cdi.documentCategory=:documentCategory and cdi.defendantId=:defendantId")
    List<CourtDocumentEntity> findCourtDocumentForNow(@QueryParam("hearingId") final UUID hearingId, @QueryParam("documentCategory") final String documentCategory, @QueryParam("defendantId") final UUID defendantId);
}
