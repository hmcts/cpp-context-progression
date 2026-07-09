package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.BooleanUtils.toBooleanDefaultIfNull;

import uk.gov.moj.cpp.progression.persistence.entity.BooleanTFConverter;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
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

    @Column(name = "document_metadata")
    private String documentMetadata;

    @Column(name = "document_category")
    private String documentCategory;

    @Column(name = "name")
    private String name;

    @Column(name = "contains_financial_means")
    @Convert(converter = BooleanTFConverter.class)
    private Boolean containsFinancialMeans;

    @Column(name = "seq_num")
    private Integer seqNum;

    @Column(name = "is_removed", nullable = false)
    private Boolean isRemoved;

    private CourtDocumentTypeRBAC courtDocumentTypeRBAC;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "courtDocument", orphanRemoval = true)
    private Set<CourtDocumentIndexEntity> indices = new HashSet<>();

    @Embedded
    public CourtDocumentTypeRBAC getCourtDocumentTypeRBAC() {
        return courtDocumentTypeRBAC;
    }

    public void setCourtDocumentTypeRBAC(final CourtDocumentTypeRBAC courtDocumentTypeRBAC) {
        this.courtDocumentTypeRBAC = courtDocumentTypeRBAC;
    }

    public UUID getCourtDocumentId() {
        return courtDocumentId;
    }

    public void setCourtDocumentId(final UUID courtDocumentId) {
        this.courtDocumentId = courtDocumentId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(final String payload) {
        this.payload = payload;
    }

    public String getDocumentMetadata() {
        return documentMetadata;
    }

    public void setDocumentMetadata(final String documentMetadata) {
        this.documentMetadata = documentMetadata;
    }

    public String getDocumentCategory() {
        return documentCategory;
    }

    public void setDocumentCategory(final String documentCategory) {
        this.documentCategory = documentCategory;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Boolean getContainsFinancialMeans() {
        return toBooleanDefaultIfNull(containsFinancialMeans, false);
    }

    public void setContainsFinancialMeans(final Boolean containsFinancialMeans) {
        if (isNull(containsFinancialMeans)) {
            this.containsFinancialMeans = false;
        } else {
            this.containsFinancialMeans = containsFinancialMeans;
        }
    }

    public Set<CourtDocumentIndexEntity> getIndices() {
        return indices;
    }

    public void setIndices(final Set<CourtDocumentIndexEntity> indices) {
        this.indices = indices;
    }

    public Integer getSeqNum() { return seqNum; }

    public void setSeqNum(final Integer seqNum) { this.seqNum = seqNum; }

    public Boolean isRemoved() {
        return isRemoved;
    }

    public void setIsRemoved(final Boolean removed) {
        isRemoved = removed;
    }


}
