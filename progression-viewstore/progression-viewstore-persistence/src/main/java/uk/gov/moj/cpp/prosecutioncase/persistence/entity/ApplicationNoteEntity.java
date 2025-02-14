package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "application_note")
@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
public class ApplicationNoteEntity implements Serializable {

    private static final long serialVersionUID = 2441781778236204986L;

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID id;

    @Column(name = "application_id", unique = false, nullable = false)
    private UUID applicationId;

    @Column(name = "note", nullable = false, length = 1000)
    private String note;

    @Column(name = "is_pinned", nullable = false)
    private Boolean isPinned;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "created_date_time", nullable = false)
    private ZonedDateTime createdDateTime;

    public ApplicationNoteEntity(final UUID id, final UUID applicationId, final String note, final Boolean isPinned, final String firstName, final String lastName, final ZonedDateTime createdDateTime) {
        this.id = id;
        this.applicationId = applicationId;
        this.note = note;
        this.isPinned = isPinned;
        this.firstName = firstName;
        this.lastName = lastName;
        this.createdDateTime = createdDateTime;
    }

    public ApplicationNoteEntity() {
    }

    public UUID getId() {
        return id;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public String getNote() {
        return note;
    }

    public Boolean getPinned() {
        return isPinned;
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

    public void setApplicationId(final UUID applicationId) {
        this.applicationId = applicationId;
    }

    public void setNote(final String note) {
        this.note = note;
    }

    public void setPinned(final Boolean pinned) {
        isPinned = pinned;
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
}
