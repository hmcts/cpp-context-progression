package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "match_defendant_case_hearing")
public class MatchDefendantCaseHearingEntity implements Serializable {

    private static final long serialVersionUID = 6924425311577045815L;

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID id;

    @Column(name = "defendant_id")
    private UUID defendantId;

    @Column(name = "master_defendant_id")
    private UUID masterDefendantId;

    @Column(name = "prosecution_case_id")
    private UUID prosecutionCaseId;

    @Column(name = "hearing_id")
    private UUID hearingId;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "hearing_id", insertable = false, updatable = false, referencedColumnName = "hearing_id")
    private HearingEntity hearing;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "prosecution_case_id", insertable = false, updatable = false, referencedColumnName = "id")
    private ProsecutionCaseEntity prosecutionCase;

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

    public UUID getMasterDefendantId() {
        return masterDefendantId;
    }

    public void setMasterDefendantId(UUID masterDefendantId) {
        this.masterDefendantId = masterDefendantId;
    }

    public UUID getProsecutionCaseId() {
        return prosecutionCaseId;
    }

    public void setProsecutionCaseId(UUID prosecutionCaseId) {
        this.prosecutionCaseId = prosecutionCaseId;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public void setHearingId(UUID hearingId) {
        this.hearingId = hearingId;
    }

    public HearingEntity getHearing() {
        return hearing;
    }

    public void setHearing(HearingEntity hearing) {
        this.hearing = hearing;
    }

    public ProsecutionCaseEntity getProsecutionCase() {
        return prosecutionCase;
    }

    public void setProsecutionCase(ProsecutionCaseEntity prosecutionCase) {
        this.prosecutionCase = prosecutionCase;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final MatchDefendantCaseHearingEntity matchDefendantCaseHearingEntity = (MatchDefendantCaseHearingEntity) o;
        return Objects.equals(defendantId, matchDefendantCaseHearingEntity.defendantId) &&
                Objects.equals(masterDefendantId, matchDefendantCaseHearingEntity.masterDefendantId) &&
                Objects.equals(prosecutionCaseId, matchDefendantCaseHearingEntity.prosecutionCaseId) &&
                Objects.equals(hearingId, matchDefendantCaseHearingEntity.hearingId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(defendantId, masterDefendantId, prosecutionCaseId, hearingId);
    }
}
