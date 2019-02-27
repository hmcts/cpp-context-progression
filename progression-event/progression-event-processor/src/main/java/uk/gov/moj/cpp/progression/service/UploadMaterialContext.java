package uk.gov.moj.cpp.progression.service;

import uk.gov.justice.core.courts.NotificationDocumentState;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

public class UploadMaterialContext {
    private  Sender sender;
    private  JsonEnvelope originatingEnvelope;
    private  UUID userId;
    private  UUID hearingId;
    private  UUID materialId;
    private  UUID fileId;
    private  NotificationDocumentState nowsNotificationDocumentState;
    private  UUID caseId;
    private  boolean isRemotePrintingRequired;

    public Sender getSender() {
        return sender;
    }

    public JsonEnvelope getOriginatingEnvelope() {
        return originatingEnvelope;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public UUID getFileId() {
        return fileId;
    }

    public NotificationDocumentState getNowsNotificationDocumentState() {
        return nowsNotificationDocumentState;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public boolean isRemotePrintingRequired() {
        return isRemotePrintingRequired;
    }

    public void setSender(final Sender sender) {
        this.sender = sender;
    }

    public void setOriginatingEnvelope(final JsonEnvelope originatingEnvelope) {
        this.originatingEnvelope = originatingEnvelope;
    }

    public void setUserId(final UUID userId) {
        this.userId = userId;
    }

    public void setHearingId(final UUID hearingId) {
        this.hearingId = hearingId;
    }

    public void setMaterialId(final UUID materialId) {
        this.materialId = materialId;
    }

    public void setFileId(final UUID fileId) {
        this.fileId = fileId;
    }

    public void setNowsNotificationDocumentState(final NotificationDocumentState nowsNotificationDocumentState) {
        this.nowsNotificationDocumentState = nowsNotificationDocumentState;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public void setRemotePrintingRequired(final boolean remotePrintingRequired) {
        isRemotePrintingRequired = remotePrintingRequired;
    }
}
