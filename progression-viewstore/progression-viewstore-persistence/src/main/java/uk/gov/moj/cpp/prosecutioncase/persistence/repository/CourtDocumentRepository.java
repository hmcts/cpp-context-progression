package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * The DeltaSpike queries referenced raw column names in HQL - prosecution_case_id, defendant_id,
 * application_id - which the legacy parser tolerated. Hibernate 7 resolves property paths only, so
 * these are written as the mapped properties (prosecutionCaseId, defendantId, applicationId). Same
 * columns, same results; the alternative is a runtime parse failure that no unit test would catch.
 */
@ApplicationScoped
public class CourtDocumentRepository extends JpaEntityRepository<CourtDocumentEntity, UUID> {

    private static final String CASE_IDS = "caseIds";

    private static final String SELECT_INDEXED_DOCUMENT =
            "select cdi.courtDocument FROM CourtDocumentIndexEntity cdi";
    private static final String SELECT_DOCUMENT_FETCHING_INDICES =
            "select cde FROM CourtDocumentEntity cde join fetch cde.indices cdi";
    private static final String ORDER_BY_INDEX_SEQ_NUM = " ORDER BY cdi.courtDocument.seqNum ASC";

    public CourtDocumentRepository() {
        super(CourtDocumentEntity.class);
    }

    @Override
    protected UUID idOf(final CourtDocumentEntity entity) {
        return entity.getCourtDocumentId();
    }

    public List<CourtDocumentEntity> findByProsecutionCaseId(final UUID caseId) {
        return entityManager.createQuery(
                        SELECT_INDEXED_DOCUMENT
                                + " where cdi.prosecutionCaseId = :caseId"
                                + ORDER_BY_INDEX_SEQ_NUM,
                        CourtDocumentEntity.class)
                .setParameter("caseId", caseId)
                .getResultList();
    }

    public List<CourtDocumentEntity> findByDefendantId(final UUID defendantId) {
        return entityManager.createQuery(
                        SELECT_DOCUMENT_FETCHING_INDICES
                                + " where cdi.defendantId = :defendantId ORDER BY cde.seqNum ASC",
                        CourtDocumentEntity.class)
                .setParameter("defendantId", defendantId)
                .getResultList();
    }

    public List<CourtDocumentEntity> findByApplicationId(final UUID applicationId) {
        return entityManager.createQuery(
                        SELECT_INDEXED_DOCUMENT
                                + " where cdi.applicationId = :applicationId"
                                + ORDER_BY_INDEX_SEQ_NUM,
                        CourtDocumentEntity.class)
                .setParameter("applicationId", applicationId)
                .getResultList();
    }

    public List<CourtDocumentEntity> findCourtDocumentForNow(final UUID hearingId,
                                                             final String documentCategory,
                                                             final UUID defendantId) {
        return entityManager.createQuery(
                        SELECT_INDEXED_DOCUMENT
                                + " where cdi.hearingId = :hearingId"
                                + " and cdi.documentCategory = :documentCategory"
                                + " and cdi.defendantId = :defendantId"
                                + ORDER_BY_INDEX_SEQ_NUM,
                        CourtDocumentEntity.class)
                .setParameter("hearingId", hearingId)
                .setParameter("documentCategory", documentCategory)
                .setParameter("defendantId", defendantId)
                .getResultList();
    }

    public List<CourtDocumentEntity> findByProsecutionCaseIdAndDefendantId(final List<UUID> caseIds,
                                                                          final List<UUID> defendantIds) {
        return entityManager.createQuery(
                        SELECT_INDEXED_DOCUMENT
                                + " where cdi.prosecutionCaseId in (:caseIds)"
                                + " and cdi.defendantId in (:defendantIds)"
                                + ORDER_BY_INDEX_SEQ_NUM,
                        CourtDocumentEntity.class)
                .setParameter(CASE_IDS, caseIds)
                .setParameter("defendantIds", defendantIds)
                .getResultList();
    }

    public List<CourtDocumentEntity> findByProsecutionCaseIds(final List<UUID> caseIds) {
        return entityManager.createQuery(
                        SELECT_DOCUMENT_FETCHING_INDICES
                                + " where cdi.prosecutionCaseId in (:caseIds) ORDER BY cde.seqNum ASC",
                        CourtDocumentEntity.class)
                .setParameter(CASE_IDS, caseIds)
                .getResultList();
    }

    public List<CourtDocumentEntity> findByProsecutionCaseIdsAndDefendantIsNull(final List<UUID> caseIds) {
        return entityManager.createQuery(
                        SELECT_DOCUMENT_FETCHING_INDICES
                                + " where cdi.prosecutionCaseId in (:caseIds) and cdi.defendantId is null"
                                + " ORDER BY cde.seqNum ASC",
                        CourtDocumentEntity.class)
                .setParameter(CASE_IDS, caseIds)
                .getResultList();
    }

    public List<CourtDocumentEntity> findByApplicationIds(final List<UUID> applicationIds) {
        return entityManager.createQuery(
                        SELECT_DOCUMENT_FETCHING_INDICES
                                + " where cdi.applicationId in (:applicationIds) ORDER BY cde.seqNum ASC",
                        CourtDocumentEntity.class)
                .setParameter("applicationIds", applicationIds)
                .getResultList();
    }

    public List<CourtDocumentEntity> findByCourtDocumentIdsAndAreNotRemoved(final List<UUID> courtDocumentIds) {
        return entityManager.createQuery(
                        "select cde FROM CourtDocumentEntity cde"
                                + " where cde.courtDocumentId in (:ids) AND cde.isRemoved is false",
                        CourtDocumentEntity.class)
                .setParameter("ids", courtDocumentIds)
                .getResultList();
    }
}
