package uk.gov.moj.cpp.progression.domain.event;

import uk.gov.justice.domain.annotation.Event;

import java.io.Serializable;
import java.util.UUID;

@Event("progression.events.new-case-document-received")
public class NewCaseDocumentReceivedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID cppCaseId;

    private final String fileId;

    private final String fileMimeType;

    private final String fileName;


    public NewCaseDocumentReceivedEvent(UUID cppCaseId, String fileId, String fileMimeType, String fileName) {
        this.cppCaseId = cppCaseId;
        this.fileId = fileId;
        this.fileMimeType = fileMimeType;
        this.fileName = fileName;
    }

    public UUID getCppCaseId() {
        return cppCaseId;
    }

    public String getFileId() {
        return fileId;
    }

    public String getFileMimeType() {
        return fileMimeType;
    }

    public String getFileName() {
        return fileName;
    }
}
