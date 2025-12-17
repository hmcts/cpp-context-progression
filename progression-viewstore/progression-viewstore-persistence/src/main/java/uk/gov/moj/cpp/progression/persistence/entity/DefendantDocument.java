package uk.gov.moj.cpp.progression.persistence.entity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
/**
 * @deprecated
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
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

    public DefendantDocument(final UUID caseId, final UUID defendantId, final UUID fileId,
                             final String fileName, final LocalDateTime lastModified) {
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

    public void setId(final UUID id) {
        this.id = id;
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

    public UUID getFileId() {
        return fileId;
    }

    public void setFileId(final UUID fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(final LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o){
            return true;
        }

        if (!(o instanceof DefendantDocument)){
            return false;
        }

        final DefendantDocument that = (DefendantDocument) o;
        final boolean checkIdCaseIdAndDefendantId = Objects.equals(getId(), that.getId()) &&
                Objects.equals(getCaseId(), that.getCaseId()) &&
                Objects.equals(getDefendantId(), that.getDefendantId());
        final boolean checkfileIdFileNameAndLastModified = Objects.equals(getFileId(), that.getFileId()) &&
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
