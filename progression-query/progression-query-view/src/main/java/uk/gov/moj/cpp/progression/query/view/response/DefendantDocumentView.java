package uk.gov.moj.cpp.progression.query.view.response;

import java.time.LocalDateTime;
import java.util.UUID;

public class DefendantDocumentView {
    private UUID fileId;
    private String fileName;
    private LocalDateTime lastModified;

    public DefendantDocumentView(UUID fileId, String fileName, LocalDateTime lastModified) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.lastModified = lastModified;
    }

    public UUID getFileId() {
        return fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }
}
