package uk.gov.moj.cpp.progression.persist;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;
import uk.gov.moj.cpp.progression.persist.entity.NotificationInfo;

import java.util.UUID;

@Repository
public interface NotificationInfoRepository extends EntityRepository<NotificationInfo, UUID> {
}

