package uk.gov.moj.cpp.progression.listener.material;

import java.io.Serializable;
/**
 * 
 * @deprecated
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
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

    public void setAlfrescoAssetId(final String alfrescoAssetId) {
        this.alfrescoAssetId = alfrescoAssetId;
    }

    public void setMimeType(final String mimeType) {
        this.mimeType = mimeType;
    }

    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    public void setExternalLink(final String externalLink) {
        this.externalLink = externalLink;
    }
}
