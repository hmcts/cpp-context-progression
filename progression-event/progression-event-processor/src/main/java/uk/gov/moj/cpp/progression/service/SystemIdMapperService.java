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

    protected static final String NOTIFICATION_SOURCE_TYPE = "PROGRESSION_NOTIFICATION_ID";
    protected static final String MATERIAL_SOURCE_TYPE = "PROGRESSION_MATERIAL_ID";
    protected static final String CASE_TARGET_TYPE = "CASE_ID";
    protected static final String APPLICATION_TARGET_TYPE = "APPLICATION_ID";
    protected static final String MATERIAL_TARGET_TYPE = "MATERIAL_ID";
    protected static final String DOCUMENT_TARGET_TYPE = "DOCUMENT_ID";

    @Inject
    private SystemUserProvider systemUserProvider;

    @Inject
    private SystemIdMapperClient systemIdMapperClient;

    public Optional<SystemIdMapping> getCppCaseIdForNotificationId(final String notificationId) {

        return systemIdMapperClient.findBy(notificationId, NOTIFICATION_SOURCE_TYPE, CASE_TARGET_TYPE, getSystemUserId());
    }

    public Optional<SystemIdMapping> getCppApplicationIdForNotificationId(final String notificationId) {

        return systemIdMapperClient.findBy(notificationId, NOTIFICATION_SOURCE_TYPE, APPLICATION_TARGET_TYPE, getSystemUserId());
    }

    public Optional<SystemIdMapping> getCppMaterialIdForNotificationId(final String notificationId) {
        return systemIdMapperClient.findBy(notificationId, NOTIFICATION_SOURCE_TYPE, MATERIAL_TARGET_TYPE, getSystemUserId());
    }

    public Optional<SystemIdMapping> getDocumentIdForMaterialId(final String materialId) {
        return systemIdMapperClient.findBy(materialId, MATERIAL_SOURCE_TYPE, DOCUMENT_TARGET_TYPE, getSystemUserId());
    }

    @SuppressWarnings("squid:S3655")
    public void mapNotificationIdToCaseId(final UUID caseId, final UUID notificationId) {

        final SystemIdMap systemIdMap = new SystemIdMap(notificationId.toString(), NOTIFICATION_SOURCE_TYPE, caseId, CASE_TARGET_TYPE);

        final AdditionResponse response = systemIdMapperClient.add(systemIdMap, getSystemUserId());

        if (!response.isSuccess()) {
            throw new IllegalStateException(format("Failed to map case Id: %s to notification id %s", caseId, notificationId));
        }
    }

    public void mapNotificationIdToApplicationId(final UUID applicationId, final UUID notificationId) {

        final SystemIdMap systemIdMap = new SystemIdMap(notificationId.toString(), NOTIFICATION_SOURCE_TYPE, applicationId, APPLICATION_TARGET_TYPE);

        final AdditionResponse response = systemIdMapperClient.add(systemIdMap, getSystemUserId());

        if (!response.isSuccess()) {
            throw new IllegalStateException(format("Failed to map case Id: %s to notification id %s", applicationId, notificationId));
        }
    }

    public void mapNotificationIdToMaterialId(final UUID materialId, final UUID notificationId) {

        final SystemIdMap systemIdMap = new SystemIdMap(notificationId.toString(), NOTIFICATION_SOURCE_TYPE, materialId, MATERIAL_TARGET_TYPE);

        final AdditionResponse response = systemIdMapperClient.add(systemIdMap, getSystemUserId());

        if (!response.isSuccess()) {
            throw new IllegalStateException(format("Failed to map material Id: %s to notification id %s", materialId, notificationId));
        }
    }

    public void mapMaterialIdToDocumentId(final UUID documentId, final UUID materialId) {

        final SystemIdMap systemIdMap = new SystemIdMap(materialId.toString(), MATERIAL_SOURCE_TYPE, documentId, DOCUMENT_TARGET_TYPE);

        final AdditionResponse response = systemIdMapperClient.add(systemIdMap, getSystemUserId());

        if (!response.isSuccess()) {
            throw new IllegalStateException(format("Failed to map material Id: %s to document id %s", materialId, documentId));
        }
    }

    private UUID getSystemUserId() {
        return systemUserProvider.getContextSystemUserId().orElseThrow(
                () -> new ContextSystemUserIdException("System user id not available for progression context"));
    }
}




