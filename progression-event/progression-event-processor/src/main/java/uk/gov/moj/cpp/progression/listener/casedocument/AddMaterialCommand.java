package uk.gov.moj.cpp.progression.listener.casedocument;

import java.io.Serializable;
/**
 * 
 * @deprecated
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
public class AddMaterialCommand implements Serializable {

    private static final long serialVersionUID = 45685980061249719L;

    private final String materialId;

    private final Document document;

    private final String fileName;

    public AddMaterialCommand(final String materialId, final Document document,
                    final String filename) {
        this.materialId = materialId;
        this.document = document;
        this.fileName = filename;
    }

    public String getMaterialId() {
        return materialId;
    }

    public Document getDocument() {
        return document;
    }

    public String getFileName() {
        return fileName;
    }
}
