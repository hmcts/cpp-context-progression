package uk.gov.moj.cpp.progression.persistence.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
/**
 *
 * @deprecated
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
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
    private final Set<DefendantBailDocument> defendantBailDocuments = new HashSet<>();

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "person_id", unique = true)
    Person person;

    @Column(name = "police_defendant_id")
    private String policeDefendantId;

    @Column(name = "bail_status")
    private String bailStatus;

    @Column(name = "defence_solicitor_firm")
    private String defenceSolicitorFirm;

    @Column(name = "custody_time_limit_date")
    private LocalDate custodyTimeLimitDate;

    @Column(name = "sentence_hearing_review_decision", nullable = false)
    @Convert(converter = BooleanTFConverter.class)
    private Boolean sentenceHearingReviewDecision;

    @Column(name = "sentence_hearing_review_decision_date_time")
    private ZonedDateTime sentenceHearingReviewDecisionDateTime;

    @Column(name = "drug_assessment")
    @Convert(converter = BooleanTFConverter.class)
    private Boolean drugAssessment;

    @Column(name = "dangerousness_assessment")
    @Convert(converter = BooleanTFConverter.class)
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
    @Convert(converter = BooleanTFConverter.class)
    private Boolean isPSRRequested;

    @Column(name = "is_statement_off_means")
    @Convert(converter = BooleanTFConverter.class)
    private Boolean isStatementOffMeans;

    @Column(name = "is_medical_documentation")
    @Convert(converter = BooleanTFConverter.class)
    private Boolean isMedicalDocumentation;

    @Column(name = "is_ancillary_orders")
    @Convert(converter = BooleanTFConverter.class)
    private Boolean isAncillaryOrders;

    @Column(name = "provide_guidance")
    private String provideGuidance;


    @Column(name = "is_no_more_information_required")
    @Convert(converter = BooleanTFConverter.class)
    private Boolean isNoMoreInformationRequired;


    @Embedded
    private InterpreterDetail interpreter;

    public Defendant(final UUID defendantId, final CaseProgressionDetail caseProgressionDetail,
                    final Boolean sentenceHearingReviewDecision,final Set<OffenceDetail> offences) {
        super();
        this.defendantId = defendantId;
        this.caseProgressionDetail = caseProgressionDetail;
        this.sentenceHearingReviewDecision = sentenceHearingReviewDecision;
        setOffences(offences);
    }

    public Defendant(final UUID defendantId, final CaseProgressionDetail caseProgressionDetail,
                     final Boolean sentenceHearingReviewDecision) {
        super();
        this.defendantId = defendantId;
        this.caseProgressionDetail = caseProgressionDetail;
        this.sentenceHearingReviewDecision = sentenceHearingReviewDecision;
    }

    public Defendant(final UUID defendantId, final Person person,
                     final String policeDefendantId, final Set<OffenceDetail> offences, final Boolean sentenceHearingReviewDecision) {
        super();
        this.defendantId = defendantId;
        this.person = person;
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

    public void setDefendantId(final UUID defendantId) {
        this.defendantId = defendantId;
    }

    public CaseProgressionDetail getCaseProgressionDetail() {
        return caseProgressionDetail;
    }

    public void setCaseProgressionDetail(final CaseProgressionDetail caseProgressionDetail) {
        this.caseProgressionDetail = caseProgressionDetail;
    }

    public Boolean getSentenceHearingReviewDecision() {
        return sentenceHearingReviewDecision;
    }

    public void setSentenceHearingReviewDecision(final Boolean sentenceHearingReviewDecision) {
        this.sentenceHearingReviewDecision = sentenceHearingReviewDecision;
    }

    public ZonedDateTime getSentenceHearingReviewDecisionDateTime() {
        return sentenceHearingReviewDecisionDateTime;
    }

    public void setSentenceHearingReviewDecisionDateTime(
            final ZonedDateTime sentenceHearingReviewDecisionDateTime) {
        this.sentenceHearingReviewDecisionDateTime = sentenceHearingReviewDecisionDateTime;
    }

    public Boolean getDrugAssessment() {
        return drugAssessment;
    }

    public void setDrugAssessment(final Boolean drugAssessment) {
        this.drugAssessment = drugAssessment;
    }

    public Boolean getDangerousnessAssessment() {
        return dangerousnessAssessment;
    }

    public void setDangerousnessAssessment(final Boolean dangerousnessAssessment) {
        this.dangerousnessAssessment = dangerousnessAssessment;
    }

    public String getStatementOfMeans() {
        return statementOfMeans;
    }

    public void setStatementOfMeans(final String statementOfMeans) {
        this.statementOfMeans = statementOfMeans;
    }

    public String getMedicalDocumentation() {
        return medicalDocumentation;
    }

    public void setMedicalDocumentation(final String medicalDocumentation) {
        this.medicalDocumentation = medicalDocumentation;
    }

    public String getDefenceOthers() {
        return defenceOthers;
    }

    public void setDefenceOthers(final String defenceOthers) {
        this.defenceOthers = defenceOthers;
    }

    public String getAncillaryOrders() {
        return ancillaryOrders;
    }

    public void setAncillaryOrders(final String ancillaryOrders) {
        this.ancillaryOrders = ancillaryOrders;
    }

    public String getProsecutionOthers() {
        return prosecutionOthers;
    }

    public void setProsecutionOthers(final String prosecutionOthers) {
        this.prosecutionOthers = prosecutionOthers;
    }

    public Boolean getIsPSRRequested() {
        return isPSRRequested;
    }

    public void setIsPSRRequested(final Boolean isPSRRequested) {
        this.isPSRRequested = isPSRRequested;
    }

    public Boolean getIsStatementOffMeans() {
        return isStatementOffMeans;
    }

    public void setIsStatementOffMeans(final Boolean isStatementOffMeans) {
        this.isStatementOffMeans = isStatementOffMeans;
    }

    public Boolean getIsMedicalDocumentation() {
        return isMedicalDocumentation;
    }

    public void setIsMedicalDocumentation(final Boolean isMedicalDocumentation) {
        this.isMedicalDocumentation = isMedicalDocumentation;
    }

    public Boolean getIsAncillaryOrders() {
        return isAncillaryOrders;
    }

    public void setIsAncillaryOrders(final Boolean isAncillaryOrders) {
        this.isAncillaryOrders = isAncillaryOrders;
    }

    public String getProvideGuidance() {
        return provideGuidance;
    }

    public void setProvideGuidance(final String provideGuidance) {
        this.provideGuidance = provideGuidance;
    }
    
    public Boolean getIsNoMoreInformationRequired() {
        return isNoMoreInformationRequired;
    }

    public void setIsNoMoreInformationRequired(final Boolean isNoMoreInformationRequired) {
        this.isNoMoreInformationRequired = isNoMoreInformationRequired;
    }


    public String getBailStatus() {
        return bailStatus;
    }

    public void setBailStatus(final String bailStatus) {
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

    public void setDefenceSolicitorFirm(final String defenceSolicitorFirm) {
        this.defenceSolicitorFirm = defenceSolicitorFirm;
    }

    public void addDefendantBailDocument(final DefendantBailDocument defendantBailDocument) {
        Objects.requireNonNull(defendantBailDocument);
        this.makeAllBailDocumentsNotActive();
        this.defendantBailDocuments.add(defendantBailDocument);
        defendantBailDocument.setDefendant(this);
    }

    @SuppressWarnings("squid:S2583")
    public Set<DefendantBailDocument> getDefendantBailDocuments() {
        return defendantBailDocuments != null ? new HashSet<>(defendantBailDocuments) : new HashSet<>();
    }

    public LocalDate getCustodyTimeLimitDate() {
        return custodyTimeLimitDate;
    }

    public void setCustodyTimeLimitDate(final LocalDate custodyTimeLimitDate) {
        this.custodyTimeLimitDate = custodyTimeLimitDate;
    }

    public Set<OffenceDetail> getOffences() {
        return offences;
    }

    public void setOffences(final Set<OffenceDetail> offences) {
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

    public void addOffence(final OffenceDetail offenceDetail) {
        Objects.requireNonNull(offenceDetail);
        offences.add(offenceDetail);
        offenceDetail.setDefendant(this);
    }

    public String getPoliceDefendantId() {
        return policeDefendantId;
    }

    public void setPoliceDefendantId(final String policeDefendantId) {
        this.policeDefendantId = policeDefendantId;
    }

    public InterpreterDetail getInterpreter() {
        return interpreter;
    }

    public void setInterpreter(final InterpreterDetail interpreter) {
        this.interpreter = interpreter;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(final Person person) {
        this.person = person;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Defendant)) {
            return false;
        }
        final Defendant defendant = (Defendant) obj;

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
