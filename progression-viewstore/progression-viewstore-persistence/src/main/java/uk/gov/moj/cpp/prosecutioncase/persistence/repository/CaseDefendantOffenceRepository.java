package uk.gov.moj.cpp.prosecutioncase.persistence.repository;


import uk.gov.justice.core.courts.FormType;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantOffence;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface CaseDefendantOffenceRepository extends EntityRepository<CaseDefendantOffence, UUID> {
    List<CaseDefendantOffence> findById(final UUID id);
    List<CaseDefendantOffence> findByCaseId(final UUID caseId);
    List<CaseDefendantOffence> findByCaseIdAndFormType(final UUID caseId, final FormType formType);
    List<CaseDefendantOffence> findByCourtFormId(final UUID courtFormId);
}
