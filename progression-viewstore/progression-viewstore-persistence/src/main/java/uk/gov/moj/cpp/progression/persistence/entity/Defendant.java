package uk.gov.moj.cpp.progression.persistence.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Entity
@Table(name = "Defendant")
public class Defendant implements Serializable  {

    private static final long serialVersionUID = 97305852963115611L;

    public static final String UNCONDITIONAL = "unconditional";

    @Id
    @Column(name = "defendant_id", unique = true, nullable = false)
    private UUID defendantId;

    @ManyToOne
    @JoinColumn(name = "caseid", nullable = false)
    CaseProgressionDetail caseProgressionDetail;


    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "defendant",orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private Set<OffenceDetail> offences=new HashSet<>();


    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "defendant")
    private Set<DefendantBailDocument> defendantBailDocuments = new HashSet<>();

    @Column(name = "person_id")
    private UUID personId;


    @Column(name = "police_defendant_id")
    private String policeDefendantId;

    @Column(name = "bail_status")
    private String bailStatus;

    @Column(name = "defence_solicitor_firm")
    private String defenceSolicitorFirm;

    @Column(name = "custody_time_limit_date")
    private LocalDate custodyTimeLimitDate;

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

    @Embedded
    private InterpreterDetail interpreter;

    public Defendant(UUID defendantId, CaseProgressionDetail caseProgressionDetail,
                    Boolean sentenceHearingReviewDecision,Set<OffenceDetail> offences) {
        super();
        this.defendantId = defendantId;
        this.caseProgressionDetail = caseProgressionDetail;
        this.sentenceHearingReviewDecision = sentenceHearingReviewDecision;
        setOffences(offences);
    }

    public Defendant(UUID defendantId, CaseProgressionDetail caseProgressionDetail,
                     Boolean sentenceHearingReviewDecision) {
        super();
        this.defendantId = defendantId;
        this.caseProgressionDetail = caseProgressionDetail;
        this.sentenceHearingReviewDecision = sentenceHearingReviewDecision;
    }

    public Defendant(UUID defendantId,UUID personId,
                     String policeDefendantId,Set<OffenceDetail> offences,Boolean sentenceHearingReviewDecision) {
        super();
        this.defendantId = defendantId;
        this.personId = personId;
        this.policeDefendantId = policeDefendantId;
        this.sentenceHearingReviewDecision = sentenceHearingReviewDecision;
        setOffences(offences);
    }



    public Defendant() {
        super();
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


    public String getBailStatus() {
        return bailStatus;
    }

    public void setBailStatus(String bailStatus) {
        this.bailStatus = bailStatus;
        if(UNCONDITIONAL.equals(this.bailStatus)) {
            this.makeAllBailDocumentsNotActive();
        }
    }

    private void makeAllBailDocumentsNotActive() {
        this.defendantBailDocuments.stream().forEach(doc -> doc.setActive(Boolean.FALSE));
    }

    public String getDefenceSolicitorFirm() {
        return defenceSolicitorFirm;
    }

    public void setDefenceSolicitorFirm(String defenceSolicitorFirm) {
        this.defenceSolicitorFirm = defenceSolicitorFirm;
    }

    public void addDefendantBailDocument(DefendantBailDocument defendantBailDocument) {
        Objects.requireNonNull(defendantBailDocument);
        this.makeAllBailDocumentsNotActive();
        this.defendantBailDocuments.add(defendantBailDocument);
        defendantBailDocument.setDefendant(this);
    }

    public Set<DefendantBailDocument> getDefendantBailDocuments() {
        return defendantBailDocuments != null ? new HashSet<>(defendantBailDocuments) : new HashSet<>();
    }

    public LocalDate getCustodyTimeLimitDate() {
        return custodyTimeLimitDate;
    }

    public void setCustodyTimeLimitDate(LocalDate custodyTimeLimitDate) {
        this.custodyTimeLimitDate = custodyTimeLimitDate;
    }

    public UUID getPersonId() {
        return personId;
    }


    public void setPersonId(UUID personId) {
        this.personId = personId;
    }

    public Set<OffenceDetail> getOffences() {
        return offences;
    }

    public void setOffences(Set<OffenceDetail> offences) {
        if (offences != null) {
            this.offences = new HashSet<>(offences);
            this.offences.forEach(offence -> offence.setDefendant(this));
        }
        else {
            this.offences = new HashSet<>();
        }
    }
    public void addOffences(final Set<OffenceDetail> newOffences) {
        if (newOffences != null && !newOffences.isEmpty()) {
            newOffences.forEach(offence -> offence.setDefendant(this));
            this.offences.addAll(newOffences);
        }
    }

    public void addOffence(OffenceDetail offenceDetail) {
        Objects.requireNonNull(offenceDetail);
        offences.add(offenceDetail);
        offenceDetail.setDefendant(this);
    }

    public String getPoliceDefendantId() {
        return policeDefendantId;
    }

    public void setPoliceDefendantId(String policeDefendantId) {
        this.policeDefendantId = policeDefendantId;
    }

    public InterpreterDetail getInterpreter() {
        return interpreter;
    }

    public void setInterpreter(InterpreterDetail interpreter) {
        this.interpreter = interpreter;
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
        return new ToStringBuilder(this).append("defendantId", defendantId)
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
