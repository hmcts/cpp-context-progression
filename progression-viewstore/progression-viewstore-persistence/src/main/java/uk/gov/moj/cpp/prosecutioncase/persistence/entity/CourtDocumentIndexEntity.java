package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@SuppressWarnings({"squid:S2384"})
@Entity
@Table(name = "court_document_index")
public class CourtDocumentIndexEntity implements Serializable {
    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID id;

    @Column(name = "defendant_id")
    private UUID defendantId;

    @Column(name = "prosecution_case_id")
    private UUID prosecutionCaseId;

    @Column(name = "application_id")
    private UUID applicationId;

    @ManyToOne
    @JoinColumn(name = "court_document_id", nullable = false)
    private CourtDocumentEntity courtDocument;

    @Column(name = "document_category")
    private String documentCategory;

    @Column(name = "hearing_id")
    private UUID hearingId;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public void setDefendantId(UUID defendantId) {
        this.defendantId = defendantId;
    }

    public UUID getProsecutionCaseId() {
        return prosecutionCaseId;
    }

    public void setProsecutionCaseId(UUID prosecutionCaseId) {
        this.prosecutionCaseId = prosecutionCaseId;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(UUID applicationId) {
        this.applicationId = applicationId;
    }

    public CourtDocumentEntity getCourtDocument() {
        return courtDocument;
    }

    public void setCourtDocument(CourtDocumentEntity courtDocument) {
        this.courtDocument = courtDocument;
    }

    public String getDocumentCategory() {
        return documentCategory;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public void setDocumentCategory(final String documentCategory) {
        this.documentCategory = documentCategory;
    }

    public void setHearingId(final UUID hearingId) {
        this.hearingId = hearingId;
    }
}
