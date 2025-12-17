package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "case_note")
@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
public class CaseNoteEntity implements Serializable {

    private static final long serialVersionUID = 2441781778236204986L;

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID id;

    @Column(name = "case_id", unique = false, nullable = false)
    private UUID caseId;

    @Column(name = "note", nullable = false, length = 1000)
    private String note;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "created_date_time", nullable = false)
    private ZonedDateTime createdDateTime;

    @Column(name = "is_pinned", nullable = false)
    private Boolean isPinned;

    public CaseNoteEntity(final UUID id, final UUID caseId, final String note, final String firstName, final String lastName, final ZonedDateTime createdDateTime, final Boolean isPinned) {
        this.id = id;
        this.caseId = caseId;
        this.note = note;
        this.firstName = firstName;
        this.lastName = lastName;
        this.createdDateTime = createdDateTime;
        this.isPinned = isPinned;
    }

    public CaseNoteEntity() {
    }

    public UUID getId() {
        return id;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getNote() {
        return note;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public ZonedDateTime getCreatedDateTime() {
        return createdDateTime;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public void setNote(final String note) {
        this.note = note;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    public void setCreatedDateTime(final ZonedDateTime createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    public Boolean getPinned() {
        return isPinned;
    }

    public void setPinned(final Boolean pinned) {
        isPinned = pinned;
    }
}
