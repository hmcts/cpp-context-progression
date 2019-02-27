package uk.gov.moj.cpp.progression.listener.material;


import java.io.Serializable;
import java.util.UUID;
/**
 * 
 * @deprecated
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
public class MaterialAdded  implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID materialId;

    private FileDetails fileDetails;
    
    private String materialAddedDate;

    public UUID getMaterialId() {
        return materialId;
    }

    public FileDetails getFileDetails() {
        return fileDetails;
    }

    public void setMaterialId(final UUID materialId) {
        this.materialId = materialId;
    }

    public void setFileDetails(final FileDetails fileDetails) {
        this.fileDetails = fileDetails;
    }

    public String getMaterialAddedDate() {
        return materialAddedDate;
    }

    public void setMaterialAddedDate(final String materialAddedDate) {
        this.materialAddedDate = materialAddedDate;
    }

}
