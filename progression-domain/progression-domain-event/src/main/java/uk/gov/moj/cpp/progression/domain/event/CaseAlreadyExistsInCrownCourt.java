package uk.gov.moj.cpp.progression.domain.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event("progression.events.case-already-exists-in-crown-court")
public class CaseAlreadyExistsInCrownCourt {

    private final UUID caseId;
    String description;

    public CaseAlreadyExistsInCrownCourt(UUID caseId,  String description) {
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