package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ResultListOpaNotice;

import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Modifying;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface ResultListOpaNoticeRepository extends EntityRepository<ResultListOpaNotice, UUID> {
    @Modifying
    void deleteByDefendantId(UUID defendantId);
}
