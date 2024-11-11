package uk.gov.moj.cpp.progression.service.dto;

public class InformantNotificationRequired {

    private Boolean isNotificationAlreadySent  = false;

    public void setNotificationAlreadySentTrue() {
        isNotificationAlreadySent = true;
    }

    public Boolean shouldSendNotificationToInformant() {
       return !isNotificationAlreadySent;
    }

    @Override
    public String toString() {
        return "InformantNotificationRequired{" +
                "isNotificationAlreadySent=" + isNotificationAlreadySent +
                '}';
    }
}