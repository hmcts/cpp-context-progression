package uk.gov.moj.cpp.progression.service;

import static java.lang.String.format;

import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.moj.cpp.progression.exception.ContextSystemUserIdException;
import uk.gov.moj.cpp.systemidmapper.client.AdditionResponse;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMap;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapperClient;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapping;

import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Created by satishkumar on 12/11/2018.
 */
@ApplicationScoped
public class SystemIdMapperService {

    protected static final String SOURCE_TYPE = "PROGRESSION_NOTIFICATION_ID";

    protected static final String CASE_TARGET_TYPE = "CASE_ID";

    protected static final String APPLICATION_TARGET_TYPE = "APPLICATION_ID";

    @Inject
    private SystemUserProvider systemUserProvider;

    @Inject
    private SystemIdMapperClient systemIdMapperClient;

    public Optional<SystemIdMapping> getCppCaseIdForNotificationId(final String notificationId) {

        return systemIdMapperClient.findBy(notificationId, SOURCE_TYPE, CASE_TARGET_TYPE, getSystemUserId());
    }

    public Optional<SystemIdMapping> getCppApplicationIdForNotificationId(final String notificationId) {

        return systemIdMapperClient.findBy(notificationId, SOURCE_TYPE, APPLICATION_TARGET_TYPE, getSystemUserId());
    }


    @SuppressWarnings("squid:S3655")
    public void mapNotificationIdToCaseId(final UUID caseId, final UUID notificationId) {

        final SystemIdMap systemIdMap = new SystemIdMap(notificationId.toString(), SOURCE_TYPE, caseId, CASE_TARGET_TYPE);

        final AdditionResponse response = systemIdMapperClient.add(systemIdMap, getSystemUserId());

        if (!response.isSuccess()) {
            throw new IllegalStateException(format("Failed to map case Id: %s to notification id %s", caseId, notificationId));
        }
    }

    public void mapNotificationIdToApplicationId(final UUID applicationId, final UUID notificationId) {

        final SystemIdMap systemIdMap = new SystemIdMap(notificationId.toString(), SOURCE_TYPE, applicationId, APPLICATION_TARGET_TYPE);

        final AdditionResponse response = systemIdMapperClient.add(systemIdMap, getSystemUserId());

        if (!response.isSuccess()) {
            throw new IllegalStateException(format("Failed to map case Id: %s to notification id %s", applicationId, notificationId));
        }
    }

    private UUID getSystemUserId() {
        return systemUserProvider.getContextSystemUserId().orElseThrow(
                () -> new ContextSystemUserIdException("System user id not available for progression context"));
    }
}




