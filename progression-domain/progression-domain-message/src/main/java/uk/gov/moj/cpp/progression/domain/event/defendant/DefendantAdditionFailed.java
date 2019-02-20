package uk.gov.moj.cpp.progression.domain.event.defendant;

import uk.gov.justice.domain.annotation.Event;
/**
 * 
 * @deprecated
 *
 */
@Deprecated
@Event("progression.events.defendant-addition-failed")
public class DefendantAdditionFailed {

    private final String caseId;
    private final String defendantId;
    private final String description;

    public DefendantAdditionFailed(final String caseId, final String defendantId, final String description) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.description = description;
    }

    public String getDefendantId() {
        return defendantId;
    }

    public String getDescription() {
        return description;
    }

    public String getCaseId() {
        return caseId;
    }
}
