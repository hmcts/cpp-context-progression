package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class CaseDefendantHearingKey implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    @Column(name = "defendant_id", nullable = false)
    private UUID defendantId;

    @Column(name = "hearing_id", nullable = false)
    private UUID hearingId;

    public CaseDefendantHearingKey() {

    }

    public CaseDefendantHearingKey(final UUID caseId, final UUID defendantId, final UUID hearingId) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.hearingId = hearingId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public void setDefendantId(final UUID defendantId) {
        this.defendantId = defendantId;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public void setHearingId(final UUID hearingId) {
        this.hearingId = hearingId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.caseId, this.defendantId, this.hearingId);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (null == o || getClass() != o.getClass()) {
            return false;
        }
        return Objects.equals(this.caseId, ((CaseDefendantHearingKey) o).caseId)
                && Objects.equals(this.defendantId, ((CaseDefendantHearingKey) o).defendantId)
                && Objects.equals(this.hearingId, ((CaseDefendantHearingKey) o).hearingId);
    }

    @Override
    public String toString() {
        return "CaseDefendantHearingKey [caseId=" + caseId + ", defendantId=" + defendantId + ", hearingId=" + hearingId + "]";
    }
}
