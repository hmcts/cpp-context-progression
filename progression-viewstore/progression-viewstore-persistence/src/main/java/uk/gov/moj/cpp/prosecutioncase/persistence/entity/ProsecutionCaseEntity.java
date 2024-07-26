package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "prosecution_case")
public class ProsecutionCaseEntity implements Serializable {

    private static final long serialVersionUID = 9730445115611L;

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID caseId;

    @Column(name = "payload")
    private String payload;

    @Column(name = "group_id")
    private UUID groupId;

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(final String payload) {
        this.payload = payload;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public void setGroupId(final UUID groupId) {
        this.groupId = groupId;
    }
}
