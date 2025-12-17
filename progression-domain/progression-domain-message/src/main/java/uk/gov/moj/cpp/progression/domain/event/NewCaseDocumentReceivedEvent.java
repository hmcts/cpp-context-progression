package uk.gov.moj.cpp.progression.domain.event;

import java.io.Serializable;
import java.util.UUID;

import uk.gov.justice.domain.annotation.Event;
/**
 * 
 * @deprecated This is deprecated for Release 2.4
 *
 */
@Deprecated
@Event("progression.events.new-case-document-received")
public class NewCaseDocumentReceivedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID cppCaseId;

    private final String fileId;

    private final String fileMimeType;

    private final String fileName;


    public NewCaseDocumentReceivedEvent(final UUID cppCaseId, final String fileId, final String fileMimeType, final String fileName) {
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
