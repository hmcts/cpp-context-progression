package uk.gov.moj.cpp.progression.persistence.entity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "Defendant_Document")
public class DefendantDocument {

    @Column(name = "case_id", nullable = false)
    UUID caseId;
    @Column(name = "file_id", nullable = false)
    UUID fileId;
    @Column(name = "file_name", nullable = false)
    String fileName;
    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID id;
    @Column(name = "defendant_id", nullable = false)
    private UUID defendantId;
    @Column(name = "last_modified", nullable = false)
    private LocalDateTime lastModified;

    public DefendantDocument(UUID caseId, UUID defendantId, UUID fileId,
                             String fileName, LocalDateTime lastModified) {
        super();
        this.id = UUID.randomUUID();
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.fileId = fileId;
        this.fileName = fileName;
        this.lastModified = lastModified;
    }

    public DefendantDocument() {
        super();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public void setDefendantId(UUID defendantId) {
        this.defendantId = defendantId;
    }

    public UUID getFileId() {
        return fileId;
    }

    public void setFileId(UUID fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o){
            return true;
        }

        if (!(o instanceof DefendantDocument)){
            return false;
        }

        DefendantDocument that = (DefendantDocument) o;
        boolean checkIdCaseIdAndDefendantId = Objects.equals(getId(), that.getId()) &&
                Objects.equals(getCaseId(), that.getCaseId()) &&
                Objects.equals(getDefendantId(), that.getDefendantId());
        boolean checkfileIdFileNameAndLastModified = Objects.equals(getFileId(), that.getFileId()) &&
                Objects.equals(getFileName(), that.getFileName()) &&
                Objects.equals(getLastModified(), that.getLastModified());
        return checkIdCaseIdAndDefendantId && checkfileIdFileNameAndLastModified;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getCaseId(), getDefendantId(), getFileId(), getFileName(), getLastModified());
    }

    @Override
    public String toString() {
        return "DefendantDocument{" +
                "id=" + id +
                ", caseId=" + caseId +
                ", defendantId=" + defendantId +
                ", fileId=" + fileId +
                ", fileName=" + fileName +
                ", lastModified=" + lastModified +
                '}';
    }
}
