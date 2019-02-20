package uk.gov.moj.cpp.progression.domain.event;

import java.util.UUID;

import uk.gov.justice.domain.annotation.Event;
/**
 * 
 * @deprecated This is deprecated for Release 2.4
 *
 */
@Deprecated
@Event("progression.events.case-already-exists-in-crown-court")
public class CaseAlreadyExistsInCrownCourt {

    private final UUID caseId;
    private String description;

    public CaseAlreadyExistsInCrownCourt(final UUID caseId,  final String description) {
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