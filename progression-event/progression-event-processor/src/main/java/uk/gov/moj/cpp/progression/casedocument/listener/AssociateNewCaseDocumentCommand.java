package uk.gov.moj.cpp.progression.casedocument.listener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import uk.gov.moj.cpp.progression.domain.Classification;

@JsonInclude(value = Include.NON_NULL)
public class AssociateNewCaseDocumentCommand implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private final String id;

    private final String caseId;
    private final String aggregateId;
    private final String materialId;
    private final String policeName;
    private final String policeMaterialId;
    private final String documentType;
    private final List<Classification> documentClassifications = new ArrayList<>();
    private final String externalFileURL;

    @SuppressWarnings("squid:S00107")
    public AssociateNewCaseDocumentCommand( 
                    final String caseId,
                    final String materialId, 
                    final String documentType) {
        this.id = UUID.randomUUID().toString();
        this.caseId = caseId;
        this.aggregateId = "";
        this.materialId = materialId;
        this.policeName = "";
        this.policeMaterialId = "";
        this.documentType = documentType;
        this.externalFileURL = "";
    }

    public String getId() {
        return id;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getMaterialId() {
        return materialId;
    }

    public String getPoliceName() {
        return policeName;
    }

    public String getPoliceMaterialId() {
        return policeMaterialId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public List<Classification> getDocumentClassifications() {
        return documentClassifications;
    }

    public String getExternalFileURL() {
        return externalFileURL;
    }
}
