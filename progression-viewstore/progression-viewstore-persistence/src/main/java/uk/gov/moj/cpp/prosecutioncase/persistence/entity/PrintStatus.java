package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import static java.util.UUID.randomUUID;

import uk.gov.moj.cpp.progression.domain.constant.PrintStatusType;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "print_status")
public class PrintStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "case_id")
    private UUID caseId;

    @Column(name = "notification_id", nullable = false)
    private UUID notificationId;

    @Column(name = "material_id")
    private UUID materialId;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PrintStatusType status;

    @Column(name = "updated", nullable = false)
    private ZonedDateTime updated;

    public PrintStatus() {
        //for JPA
        this.id = randomUUID();
    }

    @SuppressWarnings("squid:S00107")
    public PrintStatus(
            final UUID caseId,
            final UUID notificationId,
            final UUID materialId,
            final String errorMessage,
            final Integer statusCode,
            final PrintStatusType status,
            final ZonedDateTime updated) {
        this.id = randomUUID();
        this.caseId = caseId;
        this.notificationId = notificationId;
        this.materialId = materialId;
        this.errorMessage = errorMessage;
        this.statusCode = statusCode;
        this.status = status;
        this.updated = updated;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(final UUID notificationId) {
        this.notificationId = notificationId;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public void setMaterialId(final UUID materialId) {
        this.materialId = materialId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(final Integer statusCode) {
        this.statusCode = statusCode;
    }

    public ZonedDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(ZonedDateTime updated) {
        this.updated = updated;
    }

    public PrintStatusType getStatus() {
        return status;
    }

    public void setStatus(PrintStatusType status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "ResultOrderPrintStatus{" +
                "id=" + id +
                ", caseId=" + caseId +
                ", notificationId=" + notificationId +
                ", materialId=" + materialId +
                ", errorMessage='" + errorMessage + '\'' +
                ", statusCode=" + statusCode +
                ", status=" + status +
                ", updated=" + updated +
                '}';
    }
}
