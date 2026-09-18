package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.domain.event.link.LinkType;
import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseLinkSplitMergeEntity;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CaseLinkSplitMergeRepository extends JpaEntityRepository<CaseLinkSplitMergeEntity, UUID> {

    private static final String CASE_ID = "caseId";

    public CaseLinkSplitMergeRepository() {
        super(CaseLinkSplitMergeEntity.class);
    }

    @Override
    protected UUID idOf(final CaseLinkSplitMergeEntity entity) {
        return entity.getId();
    }

    public List<CaseLinkSplitMergeEntity> findByLinkGroupId(final UUID linkGroupId) {
        return entityManager.createQuery(
                        "select e from CaseLinkSplitMergeEntity e where e.linkGroupId = :linkGroupId",
                        CaseLinkSplitMergeEntity.class)
                .setParameter("linkGroupId", linkGroupId)
                .getResultList();
    }

    public List<CaseLinkSplitMergeEntity> findByCaseId(final UUID caseId) {
        return entityManager.createQuery(
                        "select e from CaseLinkSplitMergeEntity e where e.caseId = :caseId",
                        CaseLinkSplitMergeEntity.class)
                .setParameter(CASE_ID, caseId)
                .getResultList();
    }

    public List<CaseLinkSplitMergeEntity> findByCaseIdAndLinkedCaseIdAndType(final UUID caseId,
                                                                             final UUID linkedCaseId,
                                                                             final LinkType type) {
        return entityManager.createQuery(
                        "select e from CaseLinkSplitMergeEntity e"
                                + " where e.caseId = :caseId and e.linkedCaseId = :linkedCaseId and e.type = :type",
                        CaseLinkSplitMergeEntity.class)
                .setParameter(CASE_ID, caseId)
                .setParameter("linkedCaseId", linkedCaseId)
                .setParameter("type", type)
                .getResultList();
    }

    public List<CaseLinkSplitMergeEntity> findPreviousMergesByReference(final UUID caseId, final String reference) {
        return entityManager.createQuery(
                        "select entity from CaseLinkSplitMergeEntity entity"
                                + " where entity.caseId != :caseId and entity.reference = :reference"
                                + " and entity.type = 'MERGE'",
                        CaseLinkSplitMergeEntity.class)
                .setParameter(CASE_ID, caseId)
                .setParameter("reference", reference)
                .getResultList();
    }
}
