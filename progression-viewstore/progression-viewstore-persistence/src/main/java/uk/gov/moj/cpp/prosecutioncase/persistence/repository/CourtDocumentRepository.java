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
    public abstract List<CourtDocumentEntity> findByDefendantId(@QueryParam("defendantId") final UUID defendantId);
}
