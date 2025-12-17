package uk.gov.moj.cpp.progression.domain.event.defendant;

import uk.gov.justice.domain.annotation.Event;
/**
 * 
 * @deprecated
 *
 */
@Deprecated
@Event("progression.events.plea-update-failed")
public class OffenceNotFound {

    private final String offenceId;
    private final String description;

    public OffenceNotFound(final String offenceId, final String description) {
        this.offenceId = offenceId;
        this.description = description;
    }

    public String getOffenceId() {
        return offenceId;
    }

    public String getDescription() {
        return description;
    }
}
