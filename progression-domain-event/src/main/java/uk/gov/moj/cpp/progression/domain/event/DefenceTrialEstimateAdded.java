package uk.gov.moj.cpp.progression.domain.event;

import java.util.UUID;

import uk.gov.justice.domain.annotation.Event;

/**
 * 
 * @author jchondig
 *
 */
@Event("progression.event.defence-trial-estimate-added")
public class DefenceTrialEstimateAdded  {

	private UUID caseProgressionId;
	
	private int defenceTrialEstimate;
	
    private Long version;
    
    public UUID getCaseProgressionId() {
		return caseProgressionId;
	}

	public Long getVersion() {
		return version;
	}

	public int getDefenceTrialEstimate() {
		return defenceTrialEstimate;
	}

	public DefenceTrialEstimateAdded(UUID caseProgressionId, int defenceTrialEstimate, Long version) {
		super();
		this.caseProgressionId = caseProgressionId;
		this.defenceTrialEstimate = defenceTrialEstimate;
		this.version = version;
	}
  
}
