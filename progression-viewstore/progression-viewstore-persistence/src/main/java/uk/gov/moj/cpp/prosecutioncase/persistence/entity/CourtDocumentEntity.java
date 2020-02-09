package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import uk.gov.moj.cpp.progression.persistence.entity.BooleanTFConverter;

import static java.util.Objects.isNull;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
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

    @Column(name = "contains_financial_means")
    @Convert(converter = BooleanTFConverter.class)
    private Boolean containsFinancialMeans;

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

    public void setContainsFinancialMeans(final Boolean containsFinancialMeans) {
        if (isNull(containsFinancialMeans)) {
            this.containsFinancialMeans = false;
        } else {
            this.containsFinancialMeans = containsFinancialMeans;
        }
    }

    public Boolean getContainsFinancialMeans() {
        return isNull(containsFinancialMeans) ? false : containsFinancialMeans;
    }

    public Set<CourtDocumentIndexEntity> getIndices() {
        return indices;
    }

    public void setIndices(Set<CourtDocumentIndexEntity> indices) {
        this.indices = indices;
    }
}
