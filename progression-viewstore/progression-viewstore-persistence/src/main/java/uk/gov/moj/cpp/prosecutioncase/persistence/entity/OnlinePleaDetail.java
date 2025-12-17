package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import static java.util.UUID.randomUUID;

import uk.gov.moj.cpp.progression.domain.pojo.PleaType;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "online_plea_detail")
public class OnlinePleaDetail {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "offence_id", updatable = false, nullable = false)
    private UUID offenceId;

    @Column(name = "case_id", updatable = false, nullable = false)
    private UUID caseId;

    @Column(name = "defendant_id", updatable = false, nullable = false)
    private UUID defendantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plea")
    private PleaType plea;

    @Column(name = "mitigation")
    private String mitigation;

    @Column(name = "not_guilty_because")
    private String notGuiltyBecause;

    // for JPA
    public OnlinePleaDetail() {
    }

    public OnlinePleaDetail(final UUID offenceId, final UUID caseId, final UUID defendantId, final PleaType plea, final String mitigation, final String notGuiltyBecause) {
        this.id = randomUUID();
        this.offenceId = offenceId;
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.plea = plea;
        this.mitigation = mitigation;
        this.notGuiltyBecause = notGuiltyBecause;
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public UUID getOffenceId() {
        return offenceId;
    }

    public void setOffenceId(final UUID offenceId) {
        this.offenceId = offenceId;
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

    public PleaType getPlea() {
        return plea;
    }

    public void setPlea(final PleaType plea) {
        this.plea = plea;
    }

    public String getMitigation() {
        return mitigation;
    }

    public void setMitigation(final String mitigation) {
        this.mitigation = mitigation;
    }

    public String getNotGuiltyBecause() {
        return notGuiltyBecause;
    }

    public void setNotGuiltyBecause(final String notGuiltyBecause) {
        this.notGuiltyBecause = notGuiltyBecause;
    }
}
