package uk.gov.moj.cpp.progression.service.dto;

import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;

public class InformantNotificationTracker {

    private ProsecutionCaseIdentifier informant;

    private Boolean isNotificationAlreadySent  = false;

    public void setNotificationAlreadySentTrue() {
        isNotificationAlreadySent = true;
    }

    public Boolean shouldSendNotificationToInformant() {
       return !isNotificationAlreadySent;
    }

    public ProsecutionCaseIdentifier getInformant() {
        return informant;
    }

    public void setInformant(final ProsecutionCaseIdentifier informant) {
        this.informant = informant;
    }

    @Override
    public String toString() {
        return "InformantNotificationTracker{" +
                "informant=" + informant +
                ", isNotificationAlreadySent=" + isNotificationAlreadySent +
                '}';
    }
}