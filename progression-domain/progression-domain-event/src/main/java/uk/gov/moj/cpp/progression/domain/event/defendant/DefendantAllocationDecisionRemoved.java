package uk.gov.moj.cpp.progression.domain.event.defendant;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event("progression.events.defendant-allocation-decision-removed")
public class DefendantAllocationDecisionRemoved {

    private final UUID caseId;
    private final UUID defendantId;

    public DefendantAllocationDecisionRemoved(UUID caseId, UUID defendentId) {
        super();
        this.caseId = caseId;
        this.defendantId = defendentId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public UUID getCaseId() {
        return caseId;
    }


}
