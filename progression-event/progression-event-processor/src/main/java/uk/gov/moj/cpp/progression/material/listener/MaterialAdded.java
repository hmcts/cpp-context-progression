package uk.gov.moj.cpp.progression.material.listener;


import java.io.Serializable;
import java.util.UUID;

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

    public void setMaterialId(UUID materialId) {
        this.materialId = materialId;
    }

    public void setFileDetails(FileDetails fileDetails) {
        this.fileDetails = fileDetails;
    }

    public String getMaterialAddedDate() {
        return materialAddedDate;
    }

    public void setMaterialAddedDate(String materialAddedDate) {
        this.materialAddedDate = materialAddedDate;
    }

}
