package uk.gov.moj.cpp.prosecutioncase.persistence.repository;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.NowDocumentRequestEntity;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface NowDocumentRequestRepository extends EntityRepository<NowDocumentRequestEntity, UUID> {

    List<NowDocumentRequestEntity> findByRequestId(final UUID requestId);

    List<NowDocumentRequestEntity> findByHearingId(final UUID hearingId);
}
