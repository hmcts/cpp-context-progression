
package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import java.util.UUID;

import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.SqlResultSetMapping;

@Entity
@SqlResultSetMapping(
        name = "MaterialIdMappingResult",
        classes = @ConstructorResult(
                targetClass = MaterialIdMapping.class,
                columns = {
                        @ColumnResult(name = "material_id", type = UUID.class),
                        @ColumnResult(name = "court_document_id", type = UUID.class),
                        @ColumnResult(name = "case_id", type = UUID.class),
                        @ColumnResult(name = "caseurn", type = String.class)
                }
        )
)
public class MaterialIdMapping {

    @Id
    private UUID materialId;

    private UUID courtDocumentId;
    private UUID caseId;
    private String caseUrn;

    public MaterialIdMapping() {
        // JPA requires a no-arg constructor
    }

    public MaterialIdMapping(
            final UUID materialId,
            final UUID courtDocumentId,
            final UUID caseId,
            final String caseUrn) {
        this.materialId = materialId;
        this.courtDocumentId = courtDocumentId;
        this.caseId = caseId;
        this.caseUrn = caseUrn;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public UUID getCourtDocumentId() {
        return courtDocumentId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getCaseUrn() {
        return caseUrn;
    }
}