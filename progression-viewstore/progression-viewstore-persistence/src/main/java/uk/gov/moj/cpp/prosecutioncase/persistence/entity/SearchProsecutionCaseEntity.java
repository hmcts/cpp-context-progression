package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import uk.gov.moj.cpp.progression.persistence.entity.BooleanTFConverter;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "search_prosecution_case")
public class SearchProsecutionCaseEntity implements Serializable {

    private static final long serialVersionUID = 9830445115611L;

    @Id
    @Column(name = "defendant_id", unique = true, nullable = false)
    private UUID defendantId;

    @Column(name = "case_id", nullable = false)
    private String caseId;

    @Column(name = "reference", nullable = false)
    private String reference;

    @Column(name = "defendant_first_name")
    private String defendantFirstName;

    @Column(name = "defendant_middle_name")
    private String defendantMiddleName;

    @Column(name = "defendant_last_name")
    private String defendantLastName;

    @Column(name = "defendant_dob")
    private String defendantDob;

    @Column(name = "prosecutor", nullable = false)
    private String prosecutor;

    @Column(name = "cps_prosecutor")
    private String cpsProsecutor;

    @Column(name = "case_status", nullable = false)
    private String status;

    @Column(name = "search_target")
    private String searchTarget;

    @Column(name = "is_standalone_application", nullable = false)
    @Convert(converter = BooleanTFConverter.class)
    private Boolean isStandaloneApplication;

    public UUID getDefendantId() {
        return defendantId;
    }

    public void setDefendantId(final UUID defendantId) {
        this.defendantId = defendantId;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(final String caseId) {
        this.caseId = caseId;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(final String reference) {
        this.reference = reference;
    }

    public String getDefendantFirstName() {
        return defendantFirstName;
    }

    public void setDefendantFirstName(final String defendantFirstName) {
        this.defendantFirstName = defendantFirstName;
    }

    public String getDefendantMiddleName() {
        return defendantMiddleName;
    }

    public void setDefendantMiddleName(final String defendantMiddleName) {
        this.defendantMiddleName = defendantMiddleName;
    }

    public String getDefendantLastName() {
        return defendantLastName;
    }

    public void setDefendantLastName(final String defendantLastName) {
        this.defendantLastName = defendantLastName;
    }

    public String getDefendantDob() {
        return defendantDob;
    }

    public void setDefendantDob(final String defendantDob) {
        this.defendantDob = defendantDob;
    }

    public String getProsecutor() {
        return prosecutor;
    }

    public void setProsecutor(final String prosecutor) {
        this.prosecutor = prosecutor;
    }

    public String getCpsProsecutor() { return cpsProsecutor; }

    public void setCpsProsecutor(final String cpsProsecutor) {
        this.cpsProsecutor = cpsProsecutor;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getSearchTarget() {
        return searchTarget;
    }

    public void setSearchTarget(final String searchTarget) {
        this.searchTarget = searchTarget;
    }

    public Boolean getStandaloneApplication() {
        return isStandaloneApplication;
    }

    public void setStandaloneApplication(final Boolean standaloneApplication) {
        isStandaloneApplication = standaloneApplication;
    }
}
