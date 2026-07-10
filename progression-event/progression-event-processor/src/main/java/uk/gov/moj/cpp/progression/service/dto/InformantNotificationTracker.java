package uk.gov.moj.cpp.progression.service.dto;

import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;

import java.util.UUID;

public class InformantNotificationTracker {

    private ProsecutionCaseIdentifier informant;

    private UUID prosecutionAuthorityId;

    private Boolean isNotificationAlreadySent  = false;

    public InformantNotificationTracker(final ProsecutionCaseIdentifier informant) {
        this.informant = informant;
    }

    public InformantNotificationTracker(final UUID prosecutionAuthorityId) {
        this.prosecutionAuthorityId = prosecutionAuthorityId;
    }

    public void setNotificationAlreadySentTrue() {
        isNotificationAlreadySent = true;
    }

    public Boolean isNotificationAlreadySent() {
       return isNotificationAlreadySent;
    }


    public UUID getProsecutionAuthorityId() {
        if (prosecutionAuthorityId != null) {
            return prosecutionAuthorityId;
        }
        return (informant != null) ? informant.getProsecutionAuthorityId() : null;
    }


    @Override
    public String toString() {
        return "InformantNotificationTracker{" +
                "informant=" + informant +
                ", prosecutionAuthorityId=" + prosecutionAuthorityId +
                ", isNotificationAlreadySent=" + isNotificationAlreadySent +
                '}';
    }
}