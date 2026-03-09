package uk.gov.moj.cpp.progression.exception;

import java.util.UUID;

public class CrimeHearingCaseEventPcrNotificationException extends RuntimeException {

    public CrimeHearingCaseEventPcrNotificationException(final UUID fileId, final UUID materialId, final String prisonCourtRegisterId, final String url) {
        super("Error: Failed to send PCR notification to Crime Court Hearing service with url: " + url 
                + " for prison court register fileId: " + fileId
                + ", for materialId: " + materialId
                + ", for prisonCourtRegisterId: " + prisonCourtRegisterId);
    }
}

