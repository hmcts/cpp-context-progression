package uk.gov.moj.cpp.progression.persistence.entity;

import java.time.LocalDate;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "IndicateStatement")
public class IndicateStatement {
    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID id;

    @Column(name = "caseid", nullable = false)
    private UUID caseId;

    @Column(name = "iskeyevidence")
    private Boolean isKeyEvidence;

    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "evidencename")
    private String evidenceName;

    @Column(name = "plandate")
    private LocalDate planDate;

    public IndicateStatement() {
        super();
    }

    public IndicateStatement(UUID id, UUID caseId, Long version, LocalDate planDate, Boolean isKeyEvidence,
            String evidenceName) {
        super();
        this.id = id;
        this.caseId = caseId;
        this.version = version;
        this.planDate = planDate;
        this.isKeyEvidence = isKeyEvidence;
        this.evidenceName = evidenceName;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Boolean getIsKeyEvidence() {
        return isKeyEvidence;
    }

    public void setIsKeyEvidence(Boolean isKeyEvidence) {
        this.isKeyEvidence = isKeyEvidence;
    }

    public String getEvidenceName() {
        return evidenceName;
    }

    public void setEvidenceName(String evidenceName) {
        this.evidenceName = evidenceName;
    }

    public LocalDate getPlanDate() {
        return planDate;
    }

    public void setPlanDate(LocalDate planDate) {
        this.planDate = planDate;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

}
