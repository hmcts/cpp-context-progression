package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.COTRDetailsEntity;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface COTRDetailsRepository extends EntityRepository<COTRDetailsEntity, UUID> {

    List<COTRDetailsEntity> findByHearingId(UUID hearingId);
    List<COTRDetailsEntity> findByProsecutionCaseId(UUID prosecutionCaseId);
}
