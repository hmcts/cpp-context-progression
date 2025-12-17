package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "court_application")
public class CourtApplicationEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID applicationId;

    @Column(name = "parent_application_id", nullable = true)
    private UUID parentApplicationId;

    @Column(name = "payload")
    private String payload;

    @Column(name = "assigned_user_id")
    private UUID assignedUserId;

    public UUID getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(final UUID applicationId) {
        this.applicationId = applicationId;
    }

    public UUID getParentApplicationId() {
        return parentApplicationId;
    }

    public void setParentApplicationId(final UUID parentApplicationId) {
        this.parentApplicationId = parentApplicationId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(final String payload) {
        this.payload = payload;
    }

    public UUID getAssignedUserId() {
        return assignedUserId;
    }

    public void setAssignedUserId(UUID assignedUserId) {
        this.assignedUserId = assignedUserId;
    }
}
