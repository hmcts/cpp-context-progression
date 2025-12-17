package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "shared_all_court_documents")
public class SharedAllCourtDocumentsEntity implements Serializable {

    private static final long serialVersionUID = 8137449412665L;

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    @Column(name = "defendant_id", nullable = false)
    private UUID defendantId;

    @Column(name = "application_hearing_id", nullable = false)
    private UUID applicationHearingId;

    @Column(name = "user_group_id")
    private UUID userGroupId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "shared_by_user")
    private UUID sharedByUser;

    @Column(name = "date_shared")
    private ZonedDateTime dateShared;

    public SharedAllCourtDocumentsEntity() {
    }

    public SharedAllCourtDocumentsEntity(final UUID id, final UUID caseId, final UUID defendantId, final UUID applicationHearingId, final UUID userGroupId, final UUID userId, final UUID sharedByUser, final ZonedDateTime dateShared) {
        this.id = id;
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.applicationHearingId = applicationHearingId;
        this.userGroupId = userGroupId;
        this.userId = userId;
        this.sharedByUser = sharedByUser;
        this.dateShared = dateShared;

    }

    public UUID getId() {
        return id;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public UUID getApplicationHearingId() {
        return applicationHearingId;
    }

    public UUID getUserGroupId() {
        return userGroupId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getSharedByUser() {
        return sharedByUser;
    }

    public ZonedDateTime getDateShared() {
        return dateShared;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public void setDefendantId(final UUID defendantId) {
        this.defendantId = defendantId;
    }

    public void setApplicationHearingId(final UUID hearingId) {
        this.applicationHearingId = hearingId;
    }

    public void setUserGroupId(final UUID userGroupId) {
        this.userGroupId = userGroupId;
    }

    public void setUserId(final UUID userId) {
        this.userId = userId;
    }

    public void setSharedByUser(final UUID sharedByUser) {
        this.sharedByUser = sharedByUser;
    }

    public void setDateShared(final ZonedDateTime dateShared) {
        this.dateShared = dateShared;
    }
}
