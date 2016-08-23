package uk.gov.moj.cpp.progression.persistence.entity;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "Defendant")
public class Defendant {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID id;



    @Column(name = "defendant_id", unique = true, nullable = false)
    private UUID defendantId;

    @ManyToOne
    @JoinColumn(name = "case_progression_id", nullable = false)
    CaseProgressionDetail caseProgressionDetail;

    @Column(name = "sentence_hearing_review_decision", nullable = false)
    private Boolean sentenceHearingReviewDecision;

    @Column(name = "sentence_hearing_review_decision_date_time")
    private LocalDate sentenceHearingReviewDecisionDateTime;

    @Column(name = "drug_assessment")
    private Boolean drugAssessment;

    @Column(name = "dangerousness_assessment")
    private Boolean dangerousnessAssessment;

    @Column(name = "statement_of_means")
    private String statementOfMeans;

    @Column(name = "medical_documentation")
    private String medicalDocumentation;

    @Column(name = "defence_others")
    private String defenceOthers;

    @Column(name = "ancillary_orders")
    private String ancillaryOrders;

    @Column(name = "prosecution_others")
    private String prosecutionOthers;

    @Column(name = "is_psr_requested")
    private Boolean isPSRRequested;

    @Column(name = "is_statement_off_means")
    private Boolean isStatementOffMeans;

    @Column(name = "is_medical_documentation")
    private Boolean isMedicalDocumentation;

    @Column(name = "is_ancillary_orders")
    private Boolean isAncillaryOrders;

    public Defendant(UUID id, UUID defendantId, CaseProgressionDetail caseProgressionDetail,
                    Boolean sentenceHearingReviewDecision) {
        super();
        this.id = id;
        this.defendantId = defendantId;
        this.caseProgressionDetail = caseProgressionDetail;
        this.sentenceHearingReviewDecision = sentenceHearingReviewDecision;
    }

    public Defendant() {
        super();
    }

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

    public CaseProgressionDetail getCaseProgressionDetail() {
        return caseProgressionDetail;
    }

    public void setCaseProgressionDetail(CaseProgressionDetail caseProgressionDetail) {
        this.caseProgressionDetail = caseProgressionDetail;
    }

    public Boolean getSentenceHearingReviewDecision() {
        return sentenceHearingReviewDecision;
    }

    public void setSentenceHearingReviewDecision(Boolean sentenceHearingReviewDecision) {
        this.sentenceHearingReviewDecision = sentenceHearingReviewDecision;
    }

    public LocalDate getSentenceHearingReviewDecisionDateTime() {
        return sentenceHearingReviewDecisionDateTime;
    }

    public void setSentenceHearingReviewDecisionDateTime(
                    LocalDate sentenceHearingReviewDecisionDateTime) {
        this.sentenceHearingReviewDecisionDateTime = sentenceHearingReviewDecisionDateTime;
    }

    public Boolean getDrugAssessment() {
        return drugAssessment;
    }

    public void setDrugAssessment(Boolean drugAssessment) {
        this.drugAssessment = drugAssessment;
    }

    public Boolean getDangerousnessAssessment() {
        return dangerousnessAssessment;
    }

    public void setDangerousnessAssessment(Boolean dangerousnessAssessment) {
        this.dangerousnessAssessment = dangerousnessAssessment;
    }

    public String getStatementOfMeans() {
        return statementOfMeans;
    }

    public void setStatementOfMeans(String statementOfMeans) {
        this.statementOfMeans = statementOfMeans;
    }

    public String getMedicalDocumentation() {
        return medicalDocumentation;
    }

    public void setMedicalDocumentation(String medicalDocumentation) {
        this.medicalDocumentation = medicalDocumentation;
    }

    public String getDefenceOthers() {
        return defenceOthers;
    }

    public void setDefenceOthers(String defenceOthers) {
        this.defenceOthers = defenceOthers;
    }

    public String getAncillaryOrders() {
        return ancillaryOrders;
    }

    public void setAncillaryOrders(String ancillaryOrders) {
        this.ancillaryOrders = ancillaryOrders;
    }

    public String getProsecutionOthers() {
        return prosecutionOthers;
    }

    public void setProsecutionOthers(String prosecutionOthers) {
        this.prosecutionOthers = prosecutionOthers;
    }

    public Boolean getIsPSRRequested() {
        return isPSRRequested;
    }

    public void setIsPSRRequested(Boolean isPSRRequested) {
        this.isPSRRequested = isPSRRequested;
    }

    public Boolean getIsStatementOffMeans() {
        return isStatementOffMeans;
    }

    public void setIsStatementOffMeans(Boolean isStatementOffMeans) {
        this.isStatementOffMeans = isStatementOffMeans;
    }

    public Boolean getIsMedicalDocumentation() {
        return isMedicalDocumentation;
    }

    public void setIsMedicalDocumentation(Boolean isMedicalDocumentation) {
        this.isMedicalDocumentation = isMedicalDocumentation;
    }

    public Boolean getIsAncillaryOrders() {
        return isAncillaryOrders;
    }

    public void setIsAncillaryOrders(Boolean isAncillaryOrders) {
        this.isAncillaryOrders = isAncillaryOrders;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Defendant))
            return false;
        Defendant defendant = (Defendant) o;
        return Objects.equals(getDefendantId(), defendant.getDefendantId())
                        && Objects.equals(getSentenceHearingReviewDecision(),
                                        defendant.getSentenceHearingReviewDecision())
                        && Objects.equals(getSentenceHearingReviewDecisionDateTime(),
                                        defendant.getSentenceHearingReviewDecisionDateTime())
                        && Objects.equals(getDrugAssessment(), defendant.getDrugAssessment())
                        && Objects.equals(getDangerousnessAssessment(),
                                        defendant.getDangerousnessAssessment())
                        && Objects.equals(getIsPSRRequested(), defendant.getIsPSRRequested())
                        && Objects.equals(getIsStatementOffMeans(),
                                        defendant.getIsStatementOffMeans())
                        && Objects.equals(getIsMedicalDocumentation(),
                                        defendant.getIsMedicalDocumentation())
                        && Objects.equals(getIsAncillaryOrders(), defendant.getIsAncillaryOrders())
                        && Objects.equals(getStatementOfMeans(), defendant.getStatementOfMeans())
                        && Objects.equals(getDefenceOthers(), defendant.getDefenceOthers())
                        && Objects.equals(getAncillaryOrders(), defendant.getAncillaryOrders())
                        && Objects.equals(getProsecutionOthers(), defendant.getProsecutionOthers());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDefendantId(), getSentenceHearingReviewDecision(),
                        getSentenceHearingReviewDecisionDateTime(), getDrugAssessment(),
                        getDangerousnessAssessment(), getIsAncillaryOrders(), getStatementOfMeans(),
                        getDefenceOthers(), getAncillaryOrders(), getProsecutionOthers(),
                        getIsAncillaryOrders(), getIsMedicalDocumentation(), getIsPSRRequested());
    }

    @Override
    public String toString() {
        return "Defendant{" + "id=" + id + ", defendantId=" + defendantId
                        + ", sentenceHearingReviewDecision=" + sentenceHearingReviewDecision
                        + ", sentenceHearingReviewDecisionDateTime="
                        + sentenceHearingReviewDecisionDateTime + ", drugAssessment="
                        + drugAssessment + ", dangerousnessAssessment=" + dangerousnessAssessment
                        + ", isAncillaryOrders='" + isAncillaryOrders + '\''
                        + ", statementOfMeans='" + statementOfMeans + '\'' + ", defenceOthers='"
                        + defenceOthers + '\'' + ", ancillaryOrders='" + ancillaryOrders + '\''
                        + ", prosecutionOthers='" + prosecutionOthers + '\''
                        + ", isStatementOffMeans='" + isStatementOffMeans + '\''
                        + ", isPSRRequested='" + isPSRRequested + '\'' + ", isMedicalDocumentation="
                        + isMedicalDocumentation + '}';
    }
}
