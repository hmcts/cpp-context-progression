package uk.gov.moj.cpp.progression.service;

import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;
import java.util.UUID;

public class UploadMaterialContextBuilder {
    private Sender sender;
    private JsonEnvelope originatingEnvelope;
    private UUID userId;
    private UUID hearingId;
    private UUID materialId;
    private UUID fileId;
    private UUID caseId;
    private UUID applicationId;
    private boolean firstClassLetter;
    private boolean secondClassLetter;
    private boolean isNotificationApi;
    private boolean isCps;
    private List<EmailChannel> emailNotifications;

    public UploadMaterialContextBuilder setSender(final Sender sender) {
        this.sender = sender;
        return this;
    }

    public UploadMaterialContextBuilder setOriginatingEnvelope(final JsonEnvelope originatingEnvelope) {
        this.originatingEnvelope = originatingEnvelope;
        return this;
    }

    public UploadMaterialContextBuilder setUserId(final UUID userId) {
        this.userId = userId;
        return this;
    }

    public UploadMaterialContextBuilder setHearingId(final UUID hearingId) {
        this.hearingId = hearingId;
        return this;
    }

    public UploadMaterialContextBuilder setMaterialId(final UUID materialId) {
        this.materialId = materialId;
        return this;
    }

    public UploadMaterialContextBuilder setFileId(final UUID fileId) {
        this.fileId = fileId;
        return this;
    }

    public UploadMaterialContextBuilder setCaseId(final UUID caseId) {
        this.caseId = caseId;
        return this;
    }

    public UploadMaterialContextBuilder setApplicationId(final UUID applicationId) {
        this.applicationId = applicationId;
        return this;
    }

    public UploadMaterialContextBuilder setFirstClassLetter(final boolean firstClassLetter) {
        this.firstClassLetter = firstClassLetter;
        return this;
    }

    public UploadMaterialContextBuilder setSecondClassLetter(final boolean secondClassLetter) {
        this.secondClassLetter = secondClassLetter;
        return this;
    }

    public UploadMaterialContextBuilder setEmailNotifications(final List<EmailChannel> emailNotifications) {
        this.emailNotifications = emailNotifications;
        return this;
    }

    public UploadMaterialContextBuilder setIsNotificationApi(final boolean isNotificationApi) {
        this.isNotificationApi = isNotificationApi;
        return this;
    }

    public UploadMaterialContextBuilder setIsCps(final boolean isCps) {
        this.isCps = isCps;
        return this;
    }

    public UploadMaterialContext build() {
        final UploadMaterialContext uploadMaterialContext = new UploadMaterialContext();
        uploadMaterialContext.setSender(sender);
        uploadMaterialContext.setOriginatingEnvelope(originatingEnvelope);
        uploadMaterialContext.setUserId(userId);
        uploadMaterialContext.setHearingId(hearingId);
        uploadMaterialContext.setFileId(fileId);
        uploadMaterialContext.setMaterialId(materialId);
        uploadMaterialContext.setCaseId(caseId);
        uploadMaterialContext.setApplicationId(applicationId);
        uploadMaterialContext.setFirstClassLetter(firstClassLetter);
        uploadMaterialContext.setSecondClassLetter(secondClassLetter);
        uploadMaterialContext.setEmailNotifications(emailNotifications);
        uploadMaterialContext.setIsNotificationApi(isNotificationApi);
        uploadMaterialContext.setIsCps(isCps);
        return uploadMaterialContext;
    }
}