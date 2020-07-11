package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "prison_court_register")
public class PrisonCourtRegisterEntity implements Serializable {
    private static final long serialVersionUID = 2774561198743459041L;

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "recorded_date")
    private LocalDate recordedDate;

    @Column(name = "court_centre_id")
    private UUID courtCentreId;

    @Column(name = "payload")
    private String payload;

    @Column(name = "file_id")
    private UUID fileId;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LocalDate getRecordedDate() {
        return recordedDate;
    }

    public void setRecordedDate(LocalDate recordedDate) {
        this.recordedDate = recordedDate;
    }

    public UUID getCourtCentreId() {
        return courtCentreId;
    }

    public void setCourtCentreId(UUID courtCentreId) {
        this.courtCentreId = courtCentreId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public UUID getFileId() {
        return fileId;
    }

    public void setFileId(UUID fileId) {
        this.fileId = fileId;
    }
}
