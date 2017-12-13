package uk.gov.moj.cpp.progression.domain.event.defendant;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event("progression.events.defendant-allocation-decision-updated")
public class DefendantAllocationDecisionUpdated {

    private final UUID caseId;
    private final UUID defendantId;
    private final String allocationDecision;

    public DefendantAllocationDecisionUpdated(UUID caseId, UUID defendentId,
                                              String allocationDecision) {
        super();
        this.caseId = caseId;
        this.defendantId = defendentId;
        this.allocationDecision = allocationDecision;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getAllocationDecision() {
        return allocationDecision;
    }

}
