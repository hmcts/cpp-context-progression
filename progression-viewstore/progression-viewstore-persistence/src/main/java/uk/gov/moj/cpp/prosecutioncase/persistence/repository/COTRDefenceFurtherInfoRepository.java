package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.COTRDefenceFurtherInfoEntity;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface COTRDefenceFurtherInfoRepository extends EntityRepository<COTRDefenceFurtherInfoEntity, UUID> {
    List<COTRDefenceFurtherInfoEntity> findByCotrDefendantId(UUID cotrDefendantId);
}
