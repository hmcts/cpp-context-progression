package uk.gov.moj.cpp.progression.query.view.response;

import java.time.LocalDate;

public class CaseProgressionDetailView {

    private String caseProgressionId;

    private String caseId;

    private LocalDate dateOfSending;

    private LocalDate dateCMISubmissionDeadline;

    private Long noOfDaysForCMISubmission;

    private String defenceIssues;

    private String sfrIssues;

    private Long defenceTrialEstimate;

    private Long prosecutionTrialEstimate;

    private Boolean isAllStatementsIdentified;

    private String version;

    private Boolean isAllStatementsServed;

    private LocalDate directionIssuedOn;

    private LocalDate ptpHearingVacatedDate;

    private String fromCourtCentre;

    private LocalDate sendingCommittalDate;

    private LocalDate sentenceHearingDate;

    private Boolean isPSROrdered;

    private LocalDate sentenceReviewDeadlineDate;

    private String status;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public LocalDate getDateCMISubmissionDeadline() {
        return dateCMISubmissionDeadline;
    }

    public void setDateCMISubmissionDeadline(LocalDate dateCMISubmissionDeadline) {
        this.dateCMISubmissionDeadline = dateCMISubmissionDeadline;
    }

    public Long getNoOfDaysForCMISubmission() {
        return noOfDaysForCMISubmission;
    }

    public void setNoOfDaysForCMISubmission(Long noOfDaysForCMISubmission) {
        this.noOfDaysForCMISubmission = noOfDaysForCMISubmission;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public LocalDate getDateOfSending() {
        return dateOfSending;
    }

    public void setDateOfSending(LocalDate dateOfSending) {
        this.dateOfSending = dateOfSending;
    }

    public String getCaseProgressionId() {
        return caseProgressionId;
    }

    public void setCaseProgressionId(String caseProgressionId) {
        this.caseProgressionId = caseProgressionId;
    }

    public String getDefenceIssues() {
        return defenceIssues;
    }

    public void setDefenceIssues(String defenceIssues) {
        this.defenceIssues = defenceIssues;
    }

    public String getSfrIssues() {
        return sfrIssues;
    }

    public void setSfrIssues(String sfrIssues) {
        this.sfrIssues = sfrIssues;
    }

    public Boolean getIsAllStatementsIdentified() {
        return isAllStatementsIdentified;
    }

    public Long getDefenceTrialEstimate() {
        return defenceTrialEstimate;
    }

    public void setDefenceTrialEstimate(Long trialEstimateDefence) {
        this.defenceTrialEstimate = trialEstimateDefence;
    }

    public Long getProsecutionTrialEstimate() {
        return prosecutionTrialEstimate;
    }

    public void setProsecutionTrialEstimate(Long trialEstimateProsecution) {
        this.prosecutionTrialEstimate = trialEstimateProsecution;
    }

    public void setIsAllStatementsIdentified(Boolean isAllStatementsIdentified) {
        this.isAllStatementsIdentified = isAllStatementsIdentified;
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

    public LocalDate getSentenceReviewDeadlineDate() {
        return sentenceReviewDeadlineDate;
    }

    public void setSentenceReviewDeadlineDate(LocalDate sentenceReviewDeadlineDate) {
        this.sentenceReviewDeadlineDate = sentenceReviewDeadlineDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
