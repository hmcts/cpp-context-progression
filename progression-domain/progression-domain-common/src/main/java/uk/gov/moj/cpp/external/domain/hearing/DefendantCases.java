package uk.gov.moj.cpp.external.domain.hearing;

import java.io.Serializable;
import java.util.UUID;

public class DefendantCases implements Serializable {

    private static final long serialVersionUID = -8211660989962205172L;
    private UUID caseId;
    private String bailStatus;
    private String custodyTimeLimitDate;

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public String getBailStatus() {
        return this.bailStatus;
    }

    public void setBailStatus(final String bailStatus) {
        this.bailStatus = bailStatus;
    }

    public String getCustodyTimeLimitDate() {
        return custodyTimeLimitDate;
    }

    public void setCustodyTimeLimitDate(final String custodyTimeLimitDate) {
        this.custodyTimeLimitDate = custodyTimeLimitDate;
    }


}
