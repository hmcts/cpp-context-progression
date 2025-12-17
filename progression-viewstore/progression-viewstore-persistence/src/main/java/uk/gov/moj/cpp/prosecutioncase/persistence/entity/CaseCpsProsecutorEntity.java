package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "case_cps_prosecutor")
public class CaseCpsProsecutorEntity implements Serializable {
    private static final long serialVersionUID = 1197304451156L;

    @Id
    @Column(name = "case_id")
    private UUID caseId;

    @Column(name = "cps_prosecutor", nullable = false)
    private String cpsProsecutor;

    @Column(name = "old_cps_prosecutor")
    private String oldCpsProsecutor;

    public CaseCpsProsecutorEntity() {
        // for Jpa
    }

    public CaseCpsProsecutorEntity(final UUID caseId, final String cpsProsecutor, final String oldCpsProsecutor) {
        this.caseId = caseId;
        this.cpsProsecutor = cpsProsecutor;
        this.oldCpsProsecutor = oldCpsProsecutor;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public String getCpsProsecutor() {
        return cpsProsecutor;
    }

    public void setCpsProsecutor(final String cpsProsecutor) {
        this.cpsProsecutor = cpsProsecutor;
    }

    public String getOldCpsProsecutor() {
        return oldCpsProsecutor;
    }

    public void setOldCpsProsecutor(final String oldCpsProsecutor) {
        this.oldCpsProsecutor = oldCpsProsecutor;
    }
}
