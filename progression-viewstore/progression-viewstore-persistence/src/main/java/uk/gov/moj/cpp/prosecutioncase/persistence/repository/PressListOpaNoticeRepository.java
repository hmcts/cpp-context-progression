package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PressListOpaNotice;

import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Modifying;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface PressListOpaNoticeRepository extends EntityRepository<PressListOpaNotice, UUID> {
    @Modifying
    void deleteByDefendantId(UUID defendantId);
}
