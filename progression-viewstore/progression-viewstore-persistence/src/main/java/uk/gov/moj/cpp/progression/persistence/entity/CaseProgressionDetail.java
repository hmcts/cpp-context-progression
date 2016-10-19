package uk.gov.moj.cpp.progression.persistence.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
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

    @Column(name = "directionissuedon")
    private LocalDate directionIssuedOn;

    @Column(name = "fromcourtcentre")
    private String fromCourtCentre;

    @Column(name = "sendingcommittaldate")
    private LocalDate sendingCommittalDate;

    @Column(name = "sentencehearingdate")
    private LocalDate sentenceHearingDate;

    @Column(name = "readyforsentencehearingdate")
    private LocalDateTime readyForSentenceHearingDate;

    @Enumerated(EnumType.STRING)
    private CaseStatusEnum status;


    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER,
                    mappedBy = "caseProgressionDetail")
    private Set<Defendant> defendants = new HashSet<>();

    public CaseProgressionDetail() {
        super();
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
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

    public LocalDate getDirectionIssuedOn() {
        return directionIssuedOn;
    }

    public void setDirectionIssuedOn(LocalDate directionIssuedOn) {
        this.directionIssuedOn = directionIssuedOn;
    }

    public String getCourtCentreId() {
        return courtCentreId;
    }

    public LocalDateTime getReadyForSentenceHearingDate() {
        return readyForSentenceHearingDate;
    }

    public void setReadyForSentenceHearingDate(LocalDateTime readyForSentenceHearingDate) {
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

    public Set<Defendant> getDefendants() {
        return defendants;
    }

    public void setDefendants(Set<Defendant> defendants) {
        this.defendants = defendants;
    }

}
