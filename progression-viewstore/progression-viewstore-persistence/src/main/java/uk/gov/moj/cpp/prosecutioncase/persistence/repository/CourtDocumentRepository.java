package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface CourtDocumentRepository extends EntityRepository<CourtDocumentEntity, UUID> {

    @Query("select courtDocument FROM CourtDocumentIndexEntity cdi where prosecution_case_id=:caseId ORDER BY cdi.courtDocument.seqNum ASC")
    List<CourtDocumentEntity> findByProsecutionCaseId(@QueryParam("caseId") final UUID caseId);

    @Query("select courtDocument FROM CourtDocumentIndexEntity cdi where defendant_id=:defendantId ORDER BY cdi.courtDocument.seqNum ASC")
    List<CourtDocumentEntity> findByDefendantId(@QueryParam("defendantId") final UUID defendantId);

    @Query("select courtDocument FROM CourtDocumentIndexEntity cdi where application_id=:applicationId ORDER BY cdi.courtDocument.seqNum ASC")
    List<CourtDocumentEntity> findByApplicationId(@QueryParam("applicationId") final UUID applicationId);

    @Query("select courtDocument FROM CourtDocumentIndexEntity cdi where cdi.hearingId=:hearingId and cdi.documentCategory=:documentCategory and cdi.defendantId=:defendantId ORDER BY cdi.courtDocument.seqNum ASC")
    List<CourtDocumentEntity> findCourtDocumentForNow(@QueryParam("hearingId") final UUID hearingId, @QueryParam("documentCategory") final String documentCategory, @QueryParam("defendantId") final UUID defendantId);

    @Query("select courtDocument FROM CourtDocumentIndexEntity cdi where prosecution_case_id=:caseId and defendant_id=:defendantId ORDER BY cdi.courtDocument.seqNum ASC")
    List<CourtDocumentEntity> findByProsecutionCaseIdAndDefendantId(@QueryParam("caseId") final UUID caseId,
                                                                    @QueryParam("defendantId") final UUID defendantId);
}
