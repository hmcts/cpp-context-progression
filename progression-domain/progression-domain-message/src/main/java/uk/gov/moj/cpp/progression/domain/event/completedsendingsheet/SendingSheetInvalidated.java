package uk.gov.moj.cpp.progression.domain.event.completedsendingsheet;

import java.util.UUID;

import uk.gov.justice.domain.annotation.Event;
/**
 * 
 * @deprecated
 *
 */
@Deprecated
@Event("progression.events.sending-sheet-invalidated")
public class SendingSheetInvalidated {
    private final UUID caseId;
    private final String description;

    public SendingSheetInvalidated(final UUID caseId, final String description) {
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
