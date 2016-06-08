package uk.gov.moj.cpp.progression.domain.event;

import java.util.UUID;

import uk.gov.justice.domain.annotation.Event;

/**
 * Event to confirm all evidence is served.
 * 
 * @author Jchondig
 *
 */
@Event("progression.events.all-statements-served")
public class AllStatementsServed {

    private UUID caseProgressionId;

    public AllStatementsServed() {
        super();
    }

    public AllStatementsServed(UUID caseProgressionId) {
        super();
        this.caseProgressionId = caseProgressionId;
    }

    public UUID getCaseProgressionId() {
        return caseProgressionId;
    }

}
