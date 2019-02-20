package uk.gov.moj.cpp.progression.service;

import uk.gov.justice.core.courts.NotificationDocumentState;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

public class UploadMaterialContextBuilder {
    private Sender sender;
    private JsonEnvelope originatingEnvelope;
    private UUID userId;
    private UUID hearingId;
    private UUID materialId;
    private UUID fileId;
    private NotificationDocumentState nowsNotificationDocumentState;
    private UUID caseId;
    private boolean isRemotePrintingRequired;

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

    public UploadMaterialContextBuilder setNowsNotificationDocumentState(final NotificationDocumentState nowsNotificationDocumentState) {
        this.nowsNotificationDocumentState = nowsNotificationDocumentState;
        return this;
    }

    public UploadMaterialContextBuilder setCaseId(final UUID caseId) {
        this.caseId = caseId;
        return this;
    }

    public UploadMaterialContextBuilder setIsRemotePrintingRequired(final boolean isRemotePrintingRequired) {
        this.isRemotePrintingRequired = isRemotePrintingRequired;
        return this;
    }

    public UploadMaterialContext createUploadMaterialContext() {
        final UploadMaterialContext uploadMaterialContext = new UploadMaterialContext();
        uploadMaterialContext.setSender(sender);
        uploadMaterialContext.setOriginatingEnvelope(originatingEnvelope);
        uploadMaterialContext.setUserId(userId);
        uploadMaterialContext.setHearingId(hearingId);
        uploadMaterialContext.setFileId(fileId);
        uploadMaterialContext.setMaterialId(materialId);
        uploadMaterialContext.setNowsNotificationDocumentState(nowsNotificationDocumentState);
        uploadMaterialContext.setCaseId(caseId);
        uploadMaterialContext.setRemotePrintingRequired(isRemotePrintingRequired);
        return uploadMaterialContext;
    }
}