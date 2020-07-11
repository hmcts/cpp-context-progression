package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.progression.domain.constant.RegisterStatus;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.InformantRegisterEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface InformantRegisterRepository extends EntityRepository<InformantRegisterEntity, UUID> {
    @Query("select informantRegister from InformantRegisterEntity informantRegister " +
            "where informantRegister.generatedDate = :registerDate " +
            "and informantRegister.registerTime IN " +
            "(select max(ir.registerTime) from InformantRegisterEntity ir " +
            "where ir.generatedDate = :registerDate group by ir.hearingId)")
    List<InformantRegisterEntity> findByRegisterDate(@QueryParam("registerDate") final LocalDate registerDate);

    List<InformantRegisterEntity> findByFileId(final UUID materialId);

    List<InformantRegisterEntity> findByStatus(final RegisterStatus status);

    @Query("select informantRegister from InformantRegisterEntity informantRegister " +
            "where informantRegister.prosecutionAuthorityId = :prosecutionAuthorityId" +
            " and informantRegister.status = 'RECORDED' and informantRegister.registerTime IN " +
            "(select max(ir.registerTime) from InformantRegisterEntity ir " +
            "where ir.prosecutionAuthorityId = :prosecutionAuthorityId " +
            "and ir.status = 'RECORDED' group by ir.hearingId)")
    List<InformantRegisterEntity> findByProsecutionAuthorityIdAndStatusRecorded(@QueryParam("prosecutionAuthorityId") final UUID prosecutionAuthorityId);

    @Query("select informantRegister FROM InformantRegisterEntity informantRegister where prosecutionAuthorityId=:prosecutionAuthorityId and status='GENERATED'")
    List<InformantRegisterEntity> findByProsecutionAuthorityIdAndStatusGenerated(@QueryParam("prosecutionAuthorityId") final UUID prosecutionAuthorityId);

    @Query("select informantRegister from InformantRegisterEntity informantRegister " +
            "where informantRegister.prosecutionAuthorityCode = :prosecutionAuthorityCode" +
            " and informantRegister.generatedDate = :registerDate and informantRegister.registerTime IN " +
            "(select max(ir.registerTime) from InformantRegisterEntity ir " +
            "where ir.prosecutionAuthorityCode = :prosecutionAuthorityCode " +
            "and ir.generatedDate = :registerDate group by ir.hearingId)")
    List<InformantRegisterEntity> findByRegisterDateAndProsecutionAuthorityCode(@QueryParam("registerDate") final LocalDate registerDate, @QueryParam("prosecutionAuthorityCode") final String prosecutionAuthorityCode);
}
