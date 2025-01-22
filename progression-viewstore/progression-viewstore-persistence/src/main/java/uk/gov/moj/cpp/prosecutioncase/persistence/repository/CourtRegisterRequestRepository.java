package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.domain.constant.RegisterStatus;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtRegisterRequestEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface CourtRegisterRequestRepository extends EntityRepository<CourtRegisterRequestEntity, UUID> {
    @Query("select courtRegister FROM CourtRegisterRequestEntity courtRegister where status=:status")
    List<CourtRegisterRequestEntity> findByStatus(@QueryParam("status") final RegisterStatus status);

    @Query("select courtRegister FROM CourtRegisterRequestEntity courtRegister where systemDocGeneratorId=:systemDocGeneratorId")
    List<CourtRegisterRequestEntity> findBySystemDocGeneratorId(@QueryParam("systemDocGeneratorId") final UUID systemDocGeneratorId);

    @Query("select courtRegister from CourtRegisterRequestEntity courtRegister " +
            "where courtRegister.courtCentreId = :courtCentreId" +
            " and courtRegister.status = 'RECORDED' and courtRegister.processedOn is null and (courtRegister.registerTime, courtRegister.hearingId) IN " +
            "(select max(cr.registerTime), cr.hearingId from CourtRegisterRequestEntity cr " +
            "where cr.courtCentreId = :courtCentreId " +
            "and cr.status = 'RECORDED' and cr.processedOn is null group by cr.hearingId, cr.status)")
    List<CourtRegisterRequestEntity> findByCourtCenterIdAndStatusRecorded(@QueryParam("courtCentreId") final UUID courtCentreId);

    @Query("select courtRegister from CourtRegisterRequestEntity courtRegister " +
            "where courtRegister.generatedDate = :registerDate " +
            "and courtRegister.registerTime IN " +
            "(select max(cr.registerTime) from CourtRegisterRequestEntity cr " +
            "where cr.generatedDate = :registerDate group by cr.hearingId)")
    List<CourtRegisterRequestEntity> findByRequestDate(@QueryParam("registerDate") final LocalDate requestDate);

    @Query("select courtRegister from CourtRegisterRequestEntity courtRegister " +
            "where courtRegister.courtHouse = :courtHouse" +
            " and courtRegister.generatedDate = :registerDate and courtRegister.registerTime IN " +
            "(select max(cr.registerTime) from CourtRegisterRequestEntity cr " +
            "where cr.courtHouse = :courtHouse " +
            "and cr.generatedDate = :registerDate group by cr.hearingId)")
    List<CourtRegisterRequestEntity> findByRequestDateAndCourtHouse(@QueryParam("registerDate") final LocalDate requestDate, @QueryParam("courtHouse") final String courtHouse);

    @Query("select courtRegister FROM CourtRegisterRequestEntity courtRegister where courtCentreId=:courtCentreId and status='GENERATED'")
    List<CourtRegisterRequestEntity> findByCourtCenterIdAndStatusGenerated(@QueryParam("courtCentreId") final UUID courtCentreId);

    @Query("select courtRegister FROM CourtRegisterRequestEntity courtRegister where courtCentreId=:courtCentreId and registerDate=:registerDate and status='GENERATED'")
    List<CourtRegisterRequestEntity> findByCourtCenterIdForRegisterDateAndStatusGenerated(@QueryParam("courtCentreId") final UUID courtCentreId, @QueryParam("registerDate") final LocalDate requestDate);

    @Query("select courtRegister from CourtRegisterRequestEntity courtRegister " +
            " where courtRegister.hearingId = :hearingId and courtRegister.status = 'RECORDED'")
    List<CourtRegisterRequestEntity> findByHearingIdAndStatusRecorded(@QueryParam("hearingId") UUID hearingId);

    @Query("select courtRegister from CourtRegisterRequestEntity courtRegister " +
            "where courtRegister.status = 'RECORDED' and courtRegister.processedOn is null and (courtRegister.registerTime, courtRegister.hearingId) IN " +
            "(select max(cr.registerTime), hearingId from CourtRegisterRequestEntity cr where cr.status = 'RECORDED' AND cr.processedOn is null group by cr.hearingId, cr.status)")
    List<CourtRegisterRequestEntity> findByStatusRecorded();

}
