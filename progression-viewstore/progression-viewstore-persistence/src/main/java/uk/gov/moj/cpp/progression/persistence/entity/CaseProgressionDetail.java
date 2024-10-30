package uk.gov.moj.cpp.progression.persistence.entity;

import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Objects;
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
/**
 * @deprecated
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
@Entity
@Table(name = "CaseProgressionDetail")
public class CaseProgressionDetail implements Serializable {
    private static final long serialVersionUID = 97304452922115611L;
    @Id
    @Column(name = "caseid", unique = true, nullable = false)
    private UUID caseId;

    @Column(name = "courtcentreid")
    private String courtCentreId;

    @Column(name = "caseurn", nullable = false)
    private String caseUrn;

    @Column(name = "fromcourtcentre")
    private String fromCourtCentre;

    @Column(name = "sendingcommittaldate")
    private LocalDate sendingCommittalDate;

    @Column(name = "sentencehearingdate")
    private LocalDate sentenceHearingDate;

    @Column(name = "casestatusupdateddatetime")
    private ZonedDateTime caseStatusUpdatedDateTime;

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

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public String getCourtCentreId() {
        return courtCentreId;
    }

    public ZonedDateTime getCaseStatusUpdatedDateTime() {
        return caseStatusUpdatedDateTime;
    }

    public void setCaseStatusUpdatedDateTime(final ZonedDateTime caseStatusUpdatedDateTime) {
        this.caseStatusUpdatedDateTime = caseStatusUpdatedDateTime;
    }

    public void setCourtCentreId(final String courtCentreId) {
        this.courtCentreId = courtCentreId;
    }

    public String getFromCourtCentre() {
        return fromCourtCentre;
    }

    public void setFromCourtCentre(final String fromCourtCentre) {
        this.fromCourtCentre = fromCourtCentre;
    }

    public LocalDate getSendingCommittalDate() {
        return sendingCommittalDate;
    }

    public void setSendingCommittalDate(final LocalDate sendingCommittalDate) {
        this.sendingCommittalDate = sendingCommittalDate;
    }

    public LocalDate getSentenceHearingDate() {
        return sentenceHearingDate;
    }

    public void setSentenceHearingDate(final LocalDate sentenceHearingDate) {
        this.sentenceHearingDate = sentenceHearingDate;
    }

    public CaseStatusEnum getStatus() {
        return status;
    }

    public void setStatus(final CaseStatusEnum status) {
        this.status = status;
    }

    public Set<Defendant> getDefendants() {
        return defendants;
    }

    public void setDefendants(final Set<Defendant> defendants) {
        this.defendants = defendants;
    }

    public Defendant getDefendant(final UUID defendentId) {
        return defendants.stream().filter(defendent -> defendent.getDefendantId().equals(defendentId))
                .findFirst().orElse(null);
    }

    public void addDefendant(final Defendant defendantDetail) {
        Objects.requireNonNull(defendantDetail);
        defendants.add(defendantDetail);
        defendantDetail.setCaseProgressionDetail(this);
    }

    public String getCaseUrn() {
        return caseUrn;
    }

    public void setCaseUrn(final String caseUrn) {
        this.caseUrn = caseUrn;
    }
}
