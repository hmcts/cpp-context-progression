package uk.gov.moj.cpp.progression.service.dto;

import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;

import java.util.UUID;

public class InformantNotificationTracker {

    private ProsecutionCaseIdentifier informant;

    private Boolean isNotificationAlreadySent  = false;

    public InformantNotificationTracker(final ProsecutionCaseIdentifier informant) {
        this.informant = informant;
    }

    public void setNotificationAlreadySentTrue() {
        isNotificationAlreadySent = true;
    }

    public Boolean isNotificationAlreadySent() {
       return isNotificationAlreadySent;
    }


    public UUID getProsecutionAuthorityId() {
        return (informant != null) ? informant.getProsecutionAuthorityId() : null;
    }


    @Override
    public String toString() {
        return "InformantNotificationTracker{" +
                "informant=" + informant +
                ", isNotificationAlreadySent=" + isNotificationAlreadySent +
                '}';
    }
}