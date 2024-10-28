package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import org.apache.deltaspike.data.api.Query;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PrisonCourtRegisterEntity;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface PrisonCourtRegisterRepository extends EntityRepository<PrisonCourtRegisterEntity, UUID> {
    List<PrisonCourtRegisterEntity> findByCourtCentreId(final UUID courtCentreId);
    PrisonCourtRegisterEntity findById(final UUID id);

    @Query(isNative = true, value= "select * FROM prison_court_register p WHERE p.court_centre_id = ?1 and cast(p.payload as jsonb)->>'hearingId' = ?2 and p.file_id is null")
    PrisonCourtRegisterEntity findByCourtCentreIdAndHearingId( final UUID courtCentreId,  final String hearingId);
}
