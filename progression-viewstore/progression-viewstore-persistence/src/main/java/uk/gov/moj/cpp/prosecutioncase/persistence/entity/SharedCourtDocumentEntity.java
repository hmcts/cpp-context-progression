package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "shared_court_document")
public class SharedCourtDocumentEntity implements Serializable {

    private static final long serialVersionUID = 8137449412665L;

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "court_document_id", nullable = false)
    private UUID courtDocumentId;

    @Column(name = "hearing_id")
    private UUID hearingId;

    @Column(name = "user_group_id")
    private UUID userGroupId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "case_id")
    private UUID caseId;

    @Column(name = "application_id")
    private UUID applicationId;

    @Column(name = "defendant_id")
    private UUID defendantId;

    @Column(name = "seq_num")
    private Integer seqNum;


    public SharedCourtDocumentEntity() {}

    public SharedCourtDocumentEntity(final UUID id, final UUID courtDocumentId, final UUID hearingId, final UUID userGroupId, final UUID userId, final UUID caseId, final UUID applicationId, final UUID defendantId, final Integer seqNum) {
        this.id = id;
        this.courtDocumentId = courtDocumentId;
        this.hearingId = hearingId;
        this.userGroupId = userGroupId;
        this.userId = userId;
        this.caseId = caseId;
        this.applicationId = applicationId;
        this.defendantId = defendantId;
        this.seqNum = seqNum;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCourtDocumentId() {
        return courtDocumentId;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public UUID getUserGroupId() {
        return userGroupId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public void setCourtDocumentId(final UUID courtDocumentId) {
        this.courtDocumentId = courtDocumentId;
    }

    public void setHearingId(final UUID hearingId) {
        this.hearingId = hearingId;
    }

    public void setUserGroupId(final UUID userGroupId) {
        this.userGroupId = userGroupId;
    }

    public void setUserId(final UUID userId) {
        this.userId = userId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public void setDefendantId(final UUID defendantId) {
        this.defendantId = defendantId;
    }

    public Integer getSeqNum() { return seqNum; }

    public void setSeqNum(final Integer seqNum) { this.seqNum = seqNum; }

    public UUID getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(UUID applicationId) {
        this.applicationId = applicationId;
    }
}
