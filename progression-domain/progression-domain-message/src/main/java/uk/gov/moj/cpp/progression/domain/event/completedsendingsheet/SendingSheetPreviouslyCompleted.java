package uk.gov.moj.cpp.progression.domain.event.completedsendingsheet;

import java.util.UUID;

import uk.gov.justice.domain.annotation.Event;
/**
 * 
 * @deprecated
 *
 */
@Deprecated
@Event("progression.events.sending-sheet-previously-completed")
public class SendingSheetPreviouslyCompleted {

    private final UUID caseId;
    String description;

    public SendingSheetPreviouslyCompleted(final UUID caseId, final String description) {
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