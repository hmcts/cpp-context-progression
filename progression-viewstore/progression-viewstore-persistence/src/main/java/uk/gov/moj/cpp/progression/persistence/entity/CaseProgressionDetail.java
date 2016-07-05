package uk.gov.moj.cpp.progression.persistence.entity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;

@Entity
@Table(name = "CaseProgressionDetail")
public class CaseProgressionDetail {
    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID id;

    @Column(name = "courtcentreid")
    private String courtCentreId;

    @Column(name = "caseid", unique = true, nullable = false)
    private UUID caseId;

    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "dateofsending", nullable = false)
    private LocalDate dateOfSending;

    @Column(name = "isallstatementsidentified")
    private Boolean isAllStatementsIdentified;

    @Column(name = "defenceissue")
    private String defenceIssue;

    @Column(name = "sfrissue")
    private String sfrIssue;

    @Column(name = "trialestimatedefence")
    private Long trialEstimateDefence;

    @Column(name = "trialestimateprosecution")
    private Long trialEstimateProsecution;

    @Transient
    private List<TimeLineDate> timeLine;

    @Column(name = "isallstatementsserved")
    private Boolean isAllStatementsServed;

    @Column(name = "directionissuedon")
    private LocalDate directionIssuedOn;

    @Column(name = "ptphearingvacateddate")
    private LocalDate ptpHearingVacatedDate;

    @Column(name = "fromcourtcentre")
    private String fromCourtCentre;

    @Column(name = "sendingcommittaldate")
    private LocalDate sendingCommittalDate;

    @Column(name = "ispsrordered")
    private Boolean isPSROrdered;

    @Column(name = "sentencehearingdate")
    private LocalDate sentenceHearingDate;

    @Column(name = "readyforsentencehearingdate")
    private LocalDate readyForSentenceHearingDate;

    @Enumerated(EnumType.STRING)
    private CaseStatusEnum status;

    public CaseProgressionDetail() {
        super();
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public LocalDate getDateOfSending() {
        return dateOfSending;
    }

    public void setDateOfSending(LocalDate dateOfSending) {
        this.dateOfSending = dateOfSending;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Boolean getIsAllStatementsIdentified() {
        return isAllStatementsIdentified;
    }

    public void setIsAllStatementsIdentified(Boolean isAllStatementsIdentified) {
        this.isAllStatementsIdentified = isAllStatementsIdentified;
    }

    public String getDefenceIssue() {
        return defenceIssue;
    }

    public void setDefenceIssue(String defenceIssue) {
        this.defenceIssue = defenceIssue;
    }

    public String getSfrIssue() {
        return sfrIssue;
    }

    public void setSfrIssue(String sfrIssue) {
        this.sfrIssue = sfrIssue;
    }

    public Long getTrialEstimateDefence() {
        return trialEstimateDefence;
    }

    public void setTrialEstimateDefence(Long trialEstimateDefence) {
        this.trialEstimateDefence = trialEstimateDefence;
    }

    public Long getTrialEstimateProsecution() {
        return trialEstimateProsecution;
    }

    public void setTrialEstimateProsecution(Long trialEstimateProsecution) {
        this.trialEstimateProsecution = trialEstimateProsecution;
    }

    public List<TimeLineDate> getTimeLine() {
        return timeLine;
    }

    public void setTimeLine(List<TimeLineDate> timeLine) {
        this.timeLine = timeLine;
    }

    public Boolean getIsAllStatementsServed() {
        return isAllStatementsServed;
    }

    public void setIsAllStatementsServed(Boolean isAllStatementsServed) {
        this.isAllStatementsServed = isAllStatementsServed;
    }

    public LocalDate getDirectionIssuedOn() {
        return directionIssuedOn;
    }

    public void setDirectionIssuedOn(LocalDate directionIssuedOn) {
        this.directionIssuedOn = directionIssuedOn;
    }

    public LocalDate getPtpHearingVacatedDate() {
        return ptpHearingVacatedDate;
    }

    public void setPtpHearingVacatedDate(LocalDate ptpHearingVacatedDate) {
        this.ptpHearingVacatedDate = ptpHearingVacatedDate;
    }

    public String getCourtCentreId() {
        return courtCentreId;
    }

    public LocalDate getReadyForSentenceHearingDate() {
        return readyForSentenceHearingDate;
    }

    public void setReadyForSentenceHearingDate(LocalDate readyForSentenceHearingDate) {
        this.readyForSentenceHearingDate = readyForSentenceHearingDate;
    }

    public void setCourtCentreId(String courtCentreId) {
        this.courtCentreId = courtCentreId;
    }

    public String getFromCourtCentre() {
        return fromCourtCentre;
    }

    public void setFromCourtCentre(String fromCourtCentre) {
        this.fromCourtCentre = fromCourtCentre;
    }

    public LocalDate getSendingCommittalDate() {
        return sendingCommittalDate;
    }

    public void setSendingCommittalDate(LocalDate sendingCommittalDate) {
        this.sendingCommittalDate = sendingCommittalDate;
    }

    public Boolean getIsPSROrdered() {
        return isPSROrdered;
    }

    public void setIsPSROrdered(Boolean isPSROrdered) {
        this.isPSROrdered = isPSROrdered;
    }

    public LocalDate getSentenceHearingDate() {
        return sentenceHearingDate;
    }

    public void setSentenceHearingDate(LocalDate sentenceHearingDate) {
        this.sentenceHearingDate = sentenceHearingDate;
    }

    public CaseStatusEnum getStatus() {
        return status;
    }

    public void setStatus(CaseStatusEnum status) {
        this.status = status;
    }

}
