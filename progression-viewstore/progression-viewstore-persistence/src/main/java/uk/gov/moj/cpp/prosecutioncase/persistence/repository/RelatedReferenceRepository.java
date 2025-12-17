package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.RelatedReference;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface RelatedReferenceRepository extends EntityRepository<RelatedReference, UUID> {

    List<RelatedReference> findByProsecutionCaseId(UUID caseId);

}
