package uk.gov.moj.cpp.progression.domain.event.defendant;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event("progression.events.defendant-offences-does-not-have-required-modeoftrial")
public class DefendantOffencesDoesNotHaveRequiredModeOfTrial {

    private final UUID caseId;
    private final UUID defendantId;

    public DefendantOffencesDoesNotHaveRequiredModeOfTrial(UUID caseId, UUID defendentId) {
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
