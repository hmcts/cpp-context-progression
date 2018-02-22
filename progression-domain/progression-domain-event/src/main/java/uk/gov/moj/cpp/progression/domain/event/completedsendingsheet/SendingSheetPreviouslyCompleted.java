package uk.gov.moj.cpp.progression.domain.event.completedsendingsheet;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event("progression.events.sending-sheet-previously-completed")
public class SendingSheetPreviouslyCompleted {

    private final UUID caseId;
    String description;

    public SendingSheetPreviouslyCompleted(UUID caseId, String description) {
        this.caseId = caseId;
        this.description = description;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getDescription() {
        return description;
    }
}