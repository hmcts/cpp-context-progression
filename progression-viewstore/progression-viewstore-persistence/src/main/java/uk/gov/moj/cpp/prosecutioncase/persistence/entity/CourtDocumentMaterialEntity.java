package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

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
