package uk.gov.moj.cpp.progression.domain;

import java.util.Map;
import java.util.UUID;

public class Notification {

    private UUID notificationId;

    private UUID templateId;

    private String sendToAddress;

    private String replyToAddress;

    private String materialUrl;


    private Map<String, String> personalisation;

    public Notification(final UUID notificationId, final UUID templateId, final String sendToAddress, final String replyToAddress, final Map<String, String> personalisation, String materialUrl) {
        this.notificationId = notificationId;
        this.templateId = templateId;
        this.sendToAddress = sendToAddress;
        this.replyToAddress = replyToAddress;
        this.personalisation = personalisation;
        this.materialUrl=materialUrl;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public String getSendToAddress() {
        return sendToAddress;
    }

    public String getReplyToAddress() {
        return replyToAddress;
    }

    public Map<String, String> getPersonalisation() {
        return personalisation;
    }

    public String getMaterialUrl() {
        return materialUrl;
    }
}
