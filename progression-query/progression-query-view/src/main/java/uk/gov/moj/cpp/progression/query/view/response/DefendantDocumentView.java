package uk.gov.moj.cpp.progression.query.view.response;

import java.time.LocalDateTime;
import java.util.UUID;
/**
 * 
 * @deprecated
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
public class DefendantDocumentView {
    private final UUID fileId;
    private final String fileName;
    private final LocalDateTime lastModified;

    public DefendantDocumentView(final UUID fileId, final String fileName, final LocalDateTime lastModified) {
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
