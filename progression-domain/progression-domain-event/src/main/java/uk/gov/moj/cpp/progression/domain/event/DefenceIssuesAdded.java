package uk.gov.moj.cpp.progression.domain.event;

import java.util.UUID;

import uk.gov.justice.domain.annotation.Event;

/**
 * 
 * @author jchondig
 *
 */
@Event("progression.events.defence-issues-added")
public class DefenceIssuesAdded {

    private UUID caseProgressionId;

    private String defenceIssues;

    public DefenceIssuesAdded(UUID caseProgressionId, String defenceIssues) {
        super();
        this.caseProgressionId = caseProgressionId;
        this.defenceIssues = defenceIssues;
    }

    public UUID getCaseProgressionId() {
        return caseProgressionId;
    }

    public String getDefenceIssues() {
        return defenceIssues;
    }

}
