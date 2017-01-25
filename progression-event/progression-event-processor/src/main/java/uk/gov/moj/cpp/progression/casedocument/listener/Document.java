package uk.gov.moj.cpp.progression.casedocument.listener;

import java.io.Serializable;

public class Document implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private String externalLink;
    private String fileReference;
    private String mimeType;
    
    public Document(String externalLink) {
        this.externalLink = externalLink;
    }
    
    public Document(String fileReference, String mimeType) {
        this.fileReference = fileReference;
        this.mimeType = mimeType;
    }
    
    public String getExternalLink() {
        return externalLink;
    }
    
    public String getFileReference() {
        return fileReference;
    }

    public Document setFileReference(final String fileReference) {
        this.fileReference = fileReference;
        return this;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Document setMimeType(final String mimeType) {
        this.mimeType = mimeType;
        return this;
    }
}
