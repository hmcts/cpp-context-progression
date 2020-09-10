package uk.gov.moj.cpp.progression.nows;

import java.util.Map;
import java.util.UUID;

public class Notification {
    private UUID notificationId;
    private UUID templateId;
    private String sendToAddress;
    private String replyToAddress;
    private String materialUrl;

    private Map<String, String> personalisation;

    public UUID getTemplateId() {
        return templateId;
    }

    public void setTemplateId(UUID templateId) {
        this.templateId = templateId;
    }

    public String getSendToAddress() {
        return sendToAddress;
    }

    public void setSendToAddress(String sendToAddress) {
        this.sendToAddress = sendToAddress;
    }

    public String getReplyToAddress() {
        return replyToAddress;
    }

    public void setReplyToAddress(String replyToAddress) {
        this.replyToAddress = replyToAddress;
    }

    public Map<String, String> getPersonalisation() {
        return personalisation;
    }

    public void setPersonalisation(Map<String, String> personalisation) {
        this.personalisation = personalisation;
    }

    public String getMaterialUrl() {
        return materialUrl;
    }

    public void setMaterialUrl(String materialUrl) {
        this.materialUrl = materialUrl;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(UUID notificationId) {
        this.notificationId = notificationId;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "notificationId=" + notificationId +
                ", templateId=" + templateId +
                ", sendToAddress='" + sendToAddress + '\'' +
                ", replyToAddress='" + replyToAddress + '\'' +
                ", personalisation=" + personalisation +
                '}';
    }
}
