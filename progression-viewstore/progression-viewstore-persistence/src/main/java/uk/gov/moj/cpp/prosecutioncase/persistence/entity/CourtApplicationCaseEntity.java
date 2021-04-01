package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "court_application_case")
public class CourtApplicationCaseEntity implements Serializable {
    private static final long serialVersionUID = -451523222173396148L;

    @Id
    private CourtApplicationCaseKey id;

    @OneToOne(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    @JoinColumn(name = "application_id", insertable = false, updatable = false, referencedColumnName = "id")
    private CourtApplicationEntity courtApplication;

    @Column(name = "case_reference")
    private String caseReference;

    public CourtApplicationCaseKey getId() {
        return id;
    }

    public void setId(final CourtApplicationCaseKey id) {
        this.id = id;
    }

    public CourtApplicationEntity getCourtApplication() {
        return courtApplication;
    }

    public void setCourtApplication(final CourtApplicationEntity courtApplication) {
        this.courtApplication = courtApplication;
    }

    public String getCaseReference() {
        return caseReference;
    }

    public void setCaseReference(final String caseReference) {
        this.caseReference = caseReference;
    }
}
