package uk.gov.moj.cpp.progression.query.view.response;

import java.time.LocalDate;

public class IndicateStatementsDetailView {

    private String id;

    private String caseId;

    private String isKeyEvidence;

    private String evidenceName;

    private LocalDate planDate;

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIsKeyEvidence() {
        return isKeyEvidence;
    }

    public void setIsKeyEvidence(String isKeyEvidence) {
        this.isKeyEvidence = isKeyEvidence;
    }

    public String getEvidenceName() {
        return evidenceName;
    }

    public void setEvidenceName(String evidenceName) {
        this.evidenceName = evidenceName;
    }

    public LocalDate getPlanDate() {
        return planDate;
    }

    public void setPlanDate(LocalDate planDate) {
        this.planDate = planDate;
    }
}
