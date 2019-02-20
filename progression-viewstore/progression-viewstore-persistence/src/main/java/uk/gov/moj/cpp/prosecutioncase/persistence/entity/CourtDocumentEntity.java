package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "court_document")
@SuppressWarnings({"squid:S2384"})
public class CourtDocumentEntity implements Serializable {

    private static final long serialVersionUID = 8137449412665L;

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID courtDocumentId;

    @Column(name = "payload")
    private String payload;

    @Column(name = "document_category")
    private String documentCategory;

    @Column(name = "name")
    private String name;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "courtDocument", orphanRemoval = true)
    private Set<CourtDocumentIndexEntity> indices = new HashSet<>();

    public UUID getCourtDocumentId() {
        return courtDocumentId;
    }

    public String getPayload() {
        return payload;
    }

    public void setCourtDocumentId(final UUID courtDocumentId) {
        this.courtDocumentId = courtDocumentId;
    }

    public void setPayload(final String payload) {
        this.payload = payload;
    }

    public String getDocumentCategory() {
        return documentCategory;
    }

    public void setDocumentCategory(String documentCategory) {
        this.documentCategory = documentCategory;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<CourtDocumentIndexEntity> getIndices() {
        return indices;
    }

    public void setIndices(Set<CourtDocumentIndexEntity> indices) {
        this.indices = indices;
    }
}
