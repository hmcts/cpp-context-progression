package uk.gov.moj.cpp.progression.domain.event;

import java.util.UUID;

import uk.gov.justice.domain.annotation.Event;

/**
 * 
 * @author jchondig
 *
 */
@Event("progression.event.prosecution-trial-estimate-added")
public class ProsecutionTrialEstimateAdded  {

	private UUID caseProgressionId;
	
	private int prosecutionTrialEstimate;
	
    private Long version;
    
    public UUID getCaseProgressionId() {
		return caseProgressionId;
	}

	public Long getVersion() {
		return version;
	}

	public int getprosecutionTrialEstimate() {
		return prosecutionTrialEstimate;
	}

	public ProsecutionTrialEstimateAdded(UUID caseProgressionId, int prosecutionTrialEstimate, Long version) {
		super();
		this.caseProgressionId = caseProgressionId;
		this.prosecutionTrialEstimate = prosecutionTrialEstimate;
		this.version = version;
	}
  
}
