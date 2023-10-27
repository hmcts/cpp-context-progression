package uk.gov.moj.cpp.progression.domain.event;

import java.io.Serializable;
import java.util.UUID;

public class MatchedDefendantNotificationInfo  implements Serializable {

    private static final long serialVersionUID = -9035278630190960463L;

    private final UUID caseId;
    private final UUID offenceId;

    private String defendantEmail;
    private String prosecutorEmail;
    private String defendantAddress;
    private String prosecutorAddress;

    public MatchedDefendantNotificationInfo(final UUID caseId, UUID offenceId) {
        super();
        this.caseId = caseId;
        this.offenceId = offenceId;
    }
    public MatchedDefendantNotificationInfo(final UUID caseId, final UUID offenceId, String defendantEmail,String prosecutorEmail, String defendantAddress, String prosecutorAddress ) {
        this.caseId = caseId;
        this.offenceId = offenceId;
        this.defendantEmail = defendantEmail;
        this.prosecutorEmail = prosecutorEmail;
        this.defendantAddress = defendantAddress;
        this.prosecutorAddress = prosecutorAddress;

    }
    public UUID getCaseId() {
        return caseId;
    }

    public UUID getOffenceId() {
        return offenceId;
    }

    public String getDefendantEmail() {
        return defendantEmail;
    }

    public void setDefendantEmail(String defendantEmail) {
        this.defendantEmail = defendantEmail;
    }

    public String getProsecutorEmail() {
        return prosecutorEmail;
    }

    public void setProsecutorEmail(String prosecutorEmail) {
        this.prosecutorEmail = prosecutorEmail;
    }

    public String getDefendantAddress() {
        return defendantAddress;
    }

    public void setDefendantAddress(String defendantAddress) {
        this.defendantAddress = defendantAddress;
    }

    public String getProsecutorAddress() {
        return prosecutorAddress;
    }

    public void setProsecutorAddress(String prosecutorAddress) {
        this.prosecutorAddress = prosecutorAddress;
    }

}
