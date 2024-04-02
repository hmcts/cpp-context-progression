package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;


@Entity
@Table(name = "press_list_opa_notice")
public class PressListOpaNotice implements Serializable {

    private static final long serialVersionUID = 9137449412665L;

    @Id
    @Column(name = "defendant_id", updatable = false, nullable = false)
    private UUID defendantId;
    @Column(name = "case_id", updatable = false, nullable = false)
    private UUID caseId;
    @Column(name = "hearing_id", updatable = false, nullable = false)
    private UUID hearingId;
    public PressListOpaNotice() {
    }

    public PressListOpaNotice(final UUID caseId,
                              final UUID defendantId,
                              final UUID hearingId) {
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
}
