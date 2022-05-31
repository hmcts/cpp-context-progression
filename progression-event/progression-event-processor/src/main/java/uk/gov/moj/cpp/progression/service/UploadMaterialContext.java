package uk.gov.moj.cpp.progression.service;

import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;
import java.util.UUID;

public class UploadMaterialContext {

    private  Sender sender;

    private  JsonEnvelope originatingEnvelope;

    private  UUID userId;

    private  UUID hearingId;

    private  UUID materialId;

    private  UUID fileId;

    private  UUID caseId;

    private  UUID applicationId;

    private  boolean firstClassLetter;

    private  boolean secondClassLetter;

    private  boolean isNotificationApi;

    private  boolean isCps;

    private List<EmailChannel> emailNotifications;

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

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public boolean isFirstClassLetter() {
        return firstClassLetter;
    }

    public boolean getIsNotificationApi() {
        return isNotificationApi;
    }

    public boolean isSecondClassLetter() {
        return secondClassLetter;
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

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public void setApplicationId(final UUID applicationId) {
        this.applicationId = applicationId;
    }

    public void setFirstClassLetter(boolean firstClassLetter) {
        this.firstClassLetter = firstClassLetter;
    }

    public void setSecondClassLetter(boolean secondClassLetter) {
        this.secondClassLetter = secondClassLetter;
    }

    public List<EmailChannel> getEmailNotifications() {
        return emailNotifications;
    }

    public void setEmailNotifications(final List<EmailChannel> emailNotifications) {
        this.emailNotifications = emailNotifications;
    }

    public void setIsNotificationApi(boolean isNotificationApi) {
        this.isNotificationApi = isNotificationApi;
    }

    public boolean getIsCps() {
        return isCps;
    }

    public void setIsCps(final boolean isCps) {
        this.isCps = isCps;
    }
}
