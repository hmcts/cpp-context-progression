package uk.gov.moj.cpp.progression.domain.event;

import java.io.Serializable;
import java.util.UUID;

import javax.json.JsonObject;

import uk.gov.justice.domain.annotation.Event;

@Event("progression.events.new-case-document-received")
public class NewCaseDocumentReceivedEvent implements Serializable {

    private JsonObject payload;

    private final UUID cppCaseId;

    private final String fileId;

    private final String fileMimeType;

    private final String fileName;
    
    private static final long serialVersionUID = 1L;

    public NewCaseDocumentReceivedEvent(final UUID id, final JsonObject payload) {
        this.payload = payload;
        this.cppCaseId = UUID.fromString(payload.getString("cppCaseId"));
        this.fileId = payload.getString("fileId");
        this.fileMimeType = payload.getString("fileMimeType");
        this.fileName = payload.getString("fileName");
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

    public JsonObject getPayload() {
        return payload;
    }

    public void setPayload(JsonObject payload) {
        this.payload = payload;
    }

    
}
