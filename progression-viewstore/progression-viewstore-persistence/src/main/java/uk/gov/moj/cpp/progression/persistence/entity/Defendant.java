package uk.gov.moj.cpp.progression.persistence.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

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
    private LocalDateTime sentenceHearingReviewDecisionDateTime;

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

    @Column(name = "provide_guidance")
    private String provideGuidance;


    @Column(name = "is_no_more_information_required")
    private Boolean isNoMoreInformationRequired;

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

    public LocalDateTime getSentenceHearingReviewDecisionDateTime() {
        return sentenceHearingReviewDecisionDateTime;
    }

    public void setSentenceHearingReviewDecisionDateTime(
                    LocalDateTime sentenceHearingReviewDecisionDateTime) {
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

    public String getProvideGuidance() {
        return provideGuidance;
    }

    public void setProvideGuidance(String provideGuidance) {
        this.provideGuidance = provideGuidance;
    }
    
    public Boolean getIsNoMoreInformationRequired() {
        return isNoMoreInformationRequired;
    }

    public void setIsNoMoreInformationRequired(Boolean isNoMoreInformationRequired) {
        this.isNoMoreInformationRequired = isNoMoreInformationRequired;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Defendant)) {
            return false;
        }
        Defendant defendant = (Defendant) obj;

        return new EqualsBuilder().append(defendantId, defendant.getDefendantId())
                        .append(sentenceHearingReviewDecision,
                                        defendant.getSentenceHearingReviewDecision())
                        .append(sentenceHearingReviewDecisionDateTime,
                                        defendant.getSentenceHearingReviewDecisionDateTime())
                        .append(drugAssessment, defendant.getDrugAssessment())
                        .append(dangerousnessAssessment, defendant.getDangerousnessAssessment())
                        .append(isPSRRequested, defendant.getIsPSRRequested())
                        .append(isStatementOffMeans, defendant.getIsStatementOffMeans())
                        .append(isMedicalDocumentation, defendant.getIsMedicalDocumentation())
                        .append(isAncillaryOrders, defendant.getIsAncillaryOrders())
                        .append(statementOfMeans, defendant.getStatementOfMeans())
                        .append(defenceOthers, defendant.getDefenceOthers())
                        .append(ancillaryOrders, defendant.getAncillaryOrders())
                        .append(provideGuidance, defendant.getProvideGuidance())
                        .append(prosecutionOthers, defendant.getProsecutionOthers()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(defendantId).append(sentenceHearingReviewDecision)
                        .append(sentenceHearingReviewDecisionDateTime).append(drugAssessment)
                        .append(dangerousnessAssessment).append(isAncillaryOrders)
                        .append(statementOfMeans).append(defenceOthers).append(ancillaryOrders)
                        .append(prosecutionOthers).append(isAncillaryOrders)
                        .append(isMedicalDocumentation).append(isPSRRequested)
                        .append(provideGuidance).hashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("id", id).append("defendantId", defendantId)
                        .append("sentenceHearingReviewDecision", sentenceHearingReviewDecision)
                        .append("sentenceHearingReviewDecisionDateTime", sentenceHearingReviewDecisionDateTime)
                        .append("drugAssessment", drugAssessment)
                        .append("dangerousnessAssessment", dangerousnessAssessment)
                        .append("isAncillaryOrders", isAncillaryOrders)
                        .append("statementOfMeans", statementOfMeans)
                        .append("defenceOthers", defenceOthers)
                        .append("ancillaryOrders", ancillaryOrders)
                        .append("prosecutionOthers", prosecutionOthers)
                        .append("isStatementOffMeans", isStatementOffMeans)
                        .append("isPSRRequested", isPSRRequested)
                        .append("isMedicalDocumentation", isMedicalDocumentation)
                        .append("provideGuidance", provideGuidance).toString();

    }
}
