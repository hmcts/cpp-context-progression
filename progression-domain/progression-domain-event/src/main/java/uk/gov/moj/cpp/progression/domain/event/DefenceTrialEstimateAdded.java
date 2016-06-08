package uk.gov.moj.cpp.progression.domain.event;

import java.util.UUID;

import uk.gov.justice.domain.annotation.Event;

/**
 * 
 * @author jchondig
 *
 */
@Event("progression.events.defence-trial-estimate-added")
public class DefenceTrialEstimateAdded {

    private UUID caseProgressionId;

    private int defenceTrialEstimate;

    public DefenceTrialEstimateAdded(UUID caseProgressionId, int defenceTrialEstimate) {
        super();
        this.caseProgressionId = caseProgressionId;
        this.defenceTrialEstimate = defenceTrialEstimate;
    }

    public UUID getCaseProgressionId() {
        return caseProgressionId;
    }

    public int getDefenceTrialEstimate() {
        return defenceTrialEstimate;
    }

}
