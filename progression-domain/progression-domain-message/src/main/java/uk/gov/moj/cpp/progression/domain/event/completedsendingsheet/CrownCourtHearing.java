package uk.gov.moj.cpp.progression.domain.event.completedsendingsheet;

import java.io.Serializable;
import java.util.UUID;
/**
 * 
 * @deprecated
 *
 */
@Deprecated
public class CrownCourtHearing implements Serializable {

    private static final long serialVersionUID = 2330555785947889310L;

    private String ccHearingDate;
    private String courtCentreName;
    private UUID courtCentreId;

    public String getCcHearingDate() {
        return this.ccHearingDate;
    }

    public void setCcHearingDate(final String ccHearingDate) {
        this.ccHearingDate = ccHearingDate;
    }

    public String getCourtCentreName() {
        return courtCentreName;
    }

    public UUID getCourtCentreId() {
        return courtCentreId;
    }

    public void setCourtCentreName(final String courtCentreName) {
        this.courtCentreName = courtCentreName;
    }

    public void setCourtCentreId(final UUID courtCentreId) {
        this.courtCentreId = courtCentreId;
    }
}