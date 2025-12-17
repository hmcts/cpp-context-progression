package uk.gov.moj.cpp.progression.eventprocessorstore.persistence.entity;

import java.io.Serial;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "notification_info")
public class NotificationInfo implements Serializable {

    private static final long serialVersionUID = 1122355349786811530L;

    @Id
    @Column(name = "notification_id", nullable = false)
    private UUID notificationId;

    @Column(name = "notification_type")
    private String notificationType;

    @Column(name = "process_name")
    private String processName;

    @Column(name = "payload")
    private String payload;

    @Column(name = "processed_timestamp")
    private ZonedDateTime processedTimestamp;

    @Column(name = "status")
    private String status;

    public NotificationInfo() {
        // for JPA
    }

    private NotificationInfo(Builder builder) {
        setNotificationId(builder.notificationId);
        setNotificationType(builder.notificationType);
        setProcessName(builder.processName);
        setPayload(builder.payload);
        setProcessedTimestamp(builder.processedTimestamp);
        setStatus(builder.status);
    }

    // Getters and Setters

    public UUID getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(UUID notificationId) {
        this.notificationId = notificationId;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public ZonedDateTime getProcessedTimestamp() {
        return processedTimestamp;
    }

    public void setProcessedTimestamp(ZonedDateTime processedTimestamp) {
        this.processedTimestamp = processedTimestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public static final class Builder {
        private UUID notificationId;
        private String notificationType;
        private String processName;
        private String payload;
        private ZonedDateTime processedTimestamp;
        private String status;

        private Builder() {
        }

        public static Builder builder() {
            return new Builder();
        }

        public Builder withNotificationId(UUID val) {
            notificationId = val;
            return this;
        }

        public Builder withNotificationType(String val) {
            notificationType = val;
            return this;
        }

        public Builder withProcessName(String val) {
            processName = val;
            return this;
        }

        public Builder withPayload(String val) {
            payload = val;
            return this;
        }

        public Builder withProcessedTimestamp(ZonedDateTime val) {
            processedTimestamp = val;
            return this;
        }

        public Builder withStatus(String val) {
            status = val;
            return this;
        }

        public NotificationInfo build() {
            return new NotificationInfo(this);
        }
    }
}
