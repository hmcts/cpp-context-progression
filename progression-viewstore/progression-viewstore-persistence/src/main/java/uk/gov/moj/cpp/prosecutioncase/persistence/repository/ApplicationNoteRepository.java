package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ApplicationNoteEntity;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface ApplicationNoteRepository extends EntityRepository<ApplicationNoteEntity, UUID> {

    List<ApplicationNoteEntity> findByApplicationIdOrderByCreatedDateTimeDesc(UUID applicationId);

}
