package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseNoteEntity;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface CaseNoteRepository extends EntityRepository<CaseNoteEntity, UUID> {

    List<CaseNoteEntity> findByCaseIdOrderByCreatedDateTimeDesc(UUID caseId);

}
