package uk.gov.moj.cpp.progression.listener.material;

import java.io.Serializable;

public class FileDetails implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private String alfrescoAssetId;
    private String mimeType;
    private String fileName;
    private String externalLink;

    public String getAlfrescoAssetId() {
        return alfrescoAssetId;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getFileName() {
        return fileName;
    }

    public String getExternalLink() {
        return externalLink;
    }

    public void setAlfrescoAssetId(String alfrescoAssetId) {
        this.alfrescoAssetId = alfrescoAssetId;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setExternalLink(String externalLink) {
        this.externalLink = externalLink;
    }
}
