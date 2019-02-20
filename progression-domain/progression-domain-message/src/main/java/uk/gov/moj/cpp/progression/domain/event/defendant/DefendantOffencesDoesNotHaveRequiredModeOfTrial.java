package uk.gov.moj.cpp.progression.domain.event.defendant;

import java.util.UUID;

import uk.gov.justice.domain.annotation.Event;
/**
 * 
 * @deprecated
 *
 */
@Deprecated
@Event("progression.events.defendant-offences-does-not-have-required-modeoftrial")
public class DefendantOffencesDoesNotHaveRequiredModeOfTrial {

    private final UUID caseId;
    private final UUID defendantId;

    public DefendantOffencesDoesNotHaveRequiredModeOfTrial(final UUID caseId, final UUID defendentId) {
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
