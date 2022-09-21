package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.COTRDefendantEntity;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface COTRDefendantRepository extends EntityRepository<COTRDefendantEntity, UUID> {

    List<COTRDefendantEntity> findByCotrId(UUID cotrId);
    List<COTRDefendantEntity> findByCotrIdAndDefendantId(UUID cotrId, UUID defendantId);

}
