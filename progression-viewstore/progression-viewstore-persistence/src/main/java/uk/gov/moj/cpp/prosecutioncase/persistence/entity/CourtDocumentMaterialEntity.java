package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

@SuppressWarnings("squid:S2384")
@Entity
@Table(name = "court_document_material")
public class CourtDocumentMaterialEntity implements Serializable {

    private static final long serialVersionUID = 8137449412665L;

    @Column(name = "court_document_id", unique = true, nullable = false)
    private UUID courtDocumentId;

    @Id
    @Column(name = "material_id")
    private UUID materialId;

    @ElementCollection
    @CollectionTable(
            name = "material_usergroup",
            joinColumns = @JoinColumn(name = "material_id")
    )
    @Column(name = "user_groups", nullable = false)
    private List<String> userGroups = new ArrayList<>();

    public UUID getCourtDocumentId() {
        return courtDocumentId;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public List<String> getUserGroups() {
        return userGroups;
    }

    public void setUserGroups(final List<String> userGroups) {
        this.userGroups = userGroups;
    }

    public void setCourtDocumentId(final UUID courtDocumentId) {
        this.courtDocumentId = courtDocumentId;
    }

    public void setMaterialId(final UUID materialId) {
        this.materialId = materialId;
    }

}
