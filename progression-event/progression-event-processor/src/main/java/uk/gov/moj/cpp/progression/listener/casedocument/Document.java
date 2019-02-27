package uk.gov.moj.cpp.progression.listener.casedocument;

import java.io.Serializable;
/**
 * 
 * @deprecated
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
public class Document implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private String externalLink;
    private String fileReference;
    private String mimeType;
    
    public Document(final String externalLink) {
        this.externalLink = externalLink;
    }
    
    public Document(final String fileReference, final String mimeType) {
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
