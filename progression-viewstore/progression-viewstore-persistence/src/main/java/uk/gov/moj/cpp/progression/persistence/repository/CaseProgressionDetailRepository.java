package uk.gov.moj.cpp.progression.persistence.repository;

import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * @deprecated
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
@ApplicationScoped
public class CaseProgressionDetailRepository extends JpaEntityRepository<CaseProgressionDetail, UUID> {

    private static final String CASE_ID = "caseId";

    public CaseProgressionDetailRepository() {
        super(CaseProgressionDetail.class);
    }

    @Override
    protected UUID idOf(final CaseProgressionDetail entity) {
        return entity.getCaseId();
    }

    /**
     * The DeltaSpike derived finder defaulted to SingleResultType.JPA, so an absent row threw
     * NoResultException rather than returning null. Preserved.
     */
    public CaseProgressionDetail findByCaseId(final UUID caseId) {
        return entityManager.createQuery(
                        "select c from CaseProgressionDetail c where c.caseId = :caseId",
                        CaseProgressionDetail.class)
                .setParameter(CASE_ID, caseId)
                .getSingleResult();
    }

    public List<CaseProgressionDetail> findByStatus(final List<CaseStatusEnum> status) {
        return entityManager.createQuery(
                        "select c from CaseProgressionDetail c where c.status in (:status)",
                        CaseProgressionDetail.class)
                .setParameter("status", status)
                .getResultList();
    }

    public List<CaseProgressionDetail> findByStatusAndCaseID(final List<CaseStatusEnum> status, final UUID caseId) {
        return entityManager.createQuery(
                        "select c from CaseProgressionDetail c where c.status in (:status) and c.caseId = :caseId",
                        CaseProgressionDetail.class)
                .setParameter("status", status)
                .setParameter(CASE_ID, caseId)
                .getResultList();
    }

    public List<Defendant> findCaseDefendants(final UUID caseId) {
        return entityManager.createQuery(
                        "SELECT cd.defendants FROM CaseProgressionDetail cd where cd.caseId = :caseId",
                        Defendant.class)
                .setParameter(CASE_ID, caseId)
                .getResultList();
    }

    /**
     * Native query kept verbatim from the DeltaSpike @Query(isNative = true).
     */
    public CaseProgressionDetail findByMaterialId(final UUID materialId) {
        return (CaseProgressionDetail) entityManager.createNativeQuery(
                        "select * from CaseProgressionDetail caseprog where \n" +
                                "caseprog.caseid=(select def.caseid from Defendant def inner join defendant_bail_document baildoc on def.defendant_id=baildoc.defendant_id where caseprog.caseid=def.caseid and baildoc.document_id=:materialId) \n",
                        CaseProgressionDetail.class)
                .setParameter("materialId", materialId)
                .getSingleResult();
    }
}
