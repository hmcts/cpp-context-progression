package uk.gov.moj.cpp.progression.domain.event.completedsendingsheet;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;
/**
 * 
 * @deprecated
 *
 */
@Deprecated
public class Hearing implements Serializable {

    private static final long serialVersionUID = 4870535860502023759L;

    private String courtCentreName;
    private String courtCentreId;
    private String type;
    private String sendingCommittalDate;
    private UUID caseId;
    private String caseUrn;
    private List<Defendant> defendants;

    public String getCourtCentreName() {
        return this.courtCentreName;
    }

    public void setCourtCentreName(final String courtCentreName) {
        this.courtCentreName = courtCentreName;
    }

    public String getCourtCentreId() {
        return this.courtCentreId;
    }

    public void setCourtCentreId(final String courtCentreId) {
        this.courtCentreId = courtCentreId;
    }

    public String getType() {
        return this.type;
    }

    public void setType(final String type) {
        this.type = type;
    }


    public String getSendingCommittalDate() {
        return this.sendingCommittalDate;
    }

    public void setSendingCommittalDate(final String sendingCommittalDate) {
        this.sendingCommittalDate = sendingCommittalDate;
    }

    public UUID getCaseId() {
        return this.caseId;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public String getCaseUrn() {
        return this.caseUrn;
    }

    public void setCaseUrn(final String caseUrn) {
        this.caseUrn = caseUrn;
    }

    public List<Defendant> getDefendants() {
        return this.defendants;
    }

    public void setDefendants(final List<Defendant> defendants) {
        this.defendants = defendants;
    }
}
