package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.domain.constant.RegisterStatus;
import uk.gov.moj.cpp.progression.persistence.repository.JpaEntityRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtRegisterRequestEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Several queries here use HQL's row-value constructor for a greatest-per-group lookup. Hibernate 7
 * rejects that under strict JPA compliance, so the persistence unit sets
 * hibernate.jpa.compliance.query=false - see persistence.xml. The queries are kept verbatim rather
 * than rewritten, because they are the ones that decide which court register is the current one.
 */
@ApplicationScoped
public class CourtRegisterRequestRepository extends JpaEntityRepository<CourtRegisterRequestEntity, UUID> {

    private static final String COURT_CENTRE_ID = "courtCentreId";
    private static final String REGISTER_DATE = "registerDate";

    /**
     * Two spellings of the same clause, kept apart because the queries below concatenate a trailing
     * space in some cases and not others; collapsing them would change the generated JPQL.
     */
    private static final String SELECT_COURT_REGISTER =
            "select courtRegister FROM CourtRegisterRequestEntity courtRegister";
    private static final String SELECT_COURT_REGISTER_SPACED =
            "select courtRegister from CourtRegisterRequestEntity courtRegister ";

    public CourtRegisterRequestRepository() {
        super(CourtRegisterRequestEntity.class);
    }

    @Override
    protected UUID idOf(final CourtRegisterRequestEntity entity) {
        return entity.getCourtRegisterRequestId();
    }

    public List<CourtRegisterRequestEntity> findByStatus(final RegisterStatus status) {
        return entityManager.createQuery(
                        SELECT_COURT_REGISTER
                                + " where courtRegister.status = :status",
                        CourtRegisterRequestEntity.class)
                .setParameter("status", status)
                .getResultList();
    }

    public List<CourtRegisterRequestEntity> findBySystemDocGeneratorId(final UUID systemDocGeneratorId) {
        return entityManager.createQuery(
                        SELECT_COURT_REGISTER
                                + " where courtRegister.systemDocGeneratorId = :systemDocGeneratorId",
                        CourtRegisterRequestEntity.class)
                .setParameter("systemDocGeneratorId", systemDocGeneratorId)
                .getResultList();
    }

    public List<CourtRegisterRequestEntity> findByCourtCenterIdAndStatusRecorded(final UUID courtCentreId) {
        return entityManager.createQuery(
                        SELECT_COURT_REGISTER_SPACED
                                + "where courtRegister.courtCentreId = :courtCentreId"
                                + " and courtRegister.status = 'RECORDED' and courtRegister.processedOn is null"
                                + " and (courtRegister.registerTime, courtRegister.hearingId) IN "
                                + "(select max(cr.registerTime), cr.hearingId from CourtRegisterRequestEntity cr "
                                + "where cr.courtCentreId = :courtCentreId "
                                + "and cr.status = 'RECORDED' and cr.processedOn is null"
                                + " group by cr.hearingId, cr.status)",
                        CourtRegisterRequestEntity.class)
                .setParameter(COURT_CENTRE_ID, courtCentreId)
                .getResultList();
    }

    public List<CourtRegisterRequestEntity> findByRequestDate(final LocalDate requestDate) {
        return entityManager.createQuery(
                        SELECT_COURT_REGISTER_SPACED
                                + "where courtRegister.generatedDate = :registerDate "
                                + "and courtRegister.registerTime IN "
                                + "(select max(cr.registerTime) from CourtRegisterRequestEntity cr "
                                + "where cr.generatedDate = :registerDate group by cr.hearingId)",
                        CourtRegisterRequestEntity.class)
                .setParameter(REGISTER_DATE, requestDate)
                .getResultList();
    }

    public List<CourtRegisterRequestEntity> findByRequestDateAndCourtHouse(final LocalDate requestDate,
                                                                          final String courtHouse) {
        return entityManager.createQuery(
                        SELECT_COURT_REGISTER_SPACED
                                + "where courtRegister.courtHouse = :courtHouse"
                                + " and courtRegister.generatedDate = :registerDate"
                                + " and courtRegister.registerTime IN "
                                + "(select max(cr.registerTime) from CourtRegisterRequestEntity cr "
                                + "where cr.courtHouse = :courtHouse "
                                + "and cr.generatedDate = :registerDate group by cr.hearingId)",
                        CourtRegisterRequestEntity.class)
                .setParameter("courtHouse", courtHouse)
                .setParameter(REGISTER_DATE, requestDate)
                .getResultList();
    }

    public List<CourtRegisterRequestEntity> findByCourtCenterIdAndStatusGenerated(final UUID courtCentreId) {
        return entityManager.createQuery(
                        SELECT_COURT_REGISTER
                                + " where courtRegister.courtCentreId = :courtCentreId"
                                + " and courtRegister.status = 'GENERATED'",
                        CourtRegisterRequestEntity.class)
                .setParameter(COURT_CENTRE_ID, courtCentreId)
                .getResultList();
    }

    public List<CourtRegisterRequestEntity> findByCourtCenterIdForRegisterDateAndStatusGenerated(
            final UUID courtCentreId, final LocalDate requestDate) {
        return entityManager.createQuery(
                        SELECT_COURT_REGISTER
                                + " where courtRegister.courtCentreId = :courtCentreId"
                                + " and courtRegister.registerDate = :registerDate"
                                + " and courtRegister.status = 'GENERATED'",
                        CourtRegisterRequestEntity.class)
                .setParameter(COURT_CENTRE_ID, courtCentreId)
                .setParameter(REGISTER_DATE, requestDate)
                .getResultList();
    }

    public List<CourtRegisterRequestEntity> findByHearingIdAndStatusRecorded(final UUID hearingId) {
        return entityManager.createQuery(
                        SELECT_COURT_REGISTER_SPACED
                                + " where courtRegister.hearingId = :hearingId"
                                + " and courtRegister.status = 'RECORDED'",
                        CourtRegisterRequestEntity.class)
                .setParameter("hearingId", hearingId)
                .getResultList();
    }

    public List<CourtRegisterRequestEntity> findByStatusRecorded() {
        return entityManager.createQuery(
                        SELECT_COURT_REGISTER_SPACED
                                + "where courtRegister.status = 'RECORDED' and courtRegister.processedOn is null"
                                + " and (courtRegister.registerTime, courtRegister.hearingId) IN "
                                + "(select max(cr.registerTime), cr.hearingId from CourtRegisterRequestEntity cr"
                                + " where cr.status = 'RECORDED' AND cr.processedOn is null"
                                + " group by cr.hearingId, cr.status)",
                        CourtRegisterRequestEntity.class)
                .getResultList();
    }
}
