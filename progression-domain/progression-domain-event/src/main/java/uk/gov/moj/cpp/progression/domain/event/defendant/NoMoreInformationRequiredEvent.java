package uk.gov.moj.cpp.progression.domain.event.defendant;

import uk.gov.justice.domain.annotation.Event;

import java.time.LocalDateTime;
import java.util.UUID;

@Event("progression.events.no-more-information-required")
public class NoMoreInformationRequiredEvent {
    private final UUID defendantId;
    private final UUID caseId;

    public NoMoreInformationRequiredEvent( UUID caseId, UUID defendantId) {
        this.caseId = caseId;
        this.defendantId = defendantId;

    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

}
