package uk.gov.moj.cpp.progression.domain.event.defendant;

import uk.gov.justice.domain.annotation.Event;

@Event("progression.events.defendant-not-found")
public class DefendantNotFound {

    private final String defendantId;
    private final String description;

    public DefendantNotFound(String defendantId, String description) {
        this.defendantId = defendantId;
        this.description = description;
    }

    public String getDefendantId() {
        return defendantId;
    }

    public String getDescription() {
        return description;
    }
}
