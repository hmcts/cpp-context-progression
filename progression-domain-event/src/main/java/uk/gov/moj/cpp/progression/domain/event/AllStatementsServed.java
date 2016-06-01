package uk.gov.moj.cpp.progression.domain.event;

import java.util.UUID;

import uk.gov.justice.domain.annotation.Event;

/**
 * Event to confirm all evidence is served.
 * 
 * @author Jchondig
 *
 */
@Event("progression.event.all-statements-served")
public class AllStatementsServed {
	
	
	private UUID caseProgressionId;
	
	private long version;
	

    public AllStatementsServed(UUID caseProgressionId, Long version) {
    	
        super();
        this.caseProgressionId = caseProgressionId;
        this.version = version;
    }


	public UUID getCaseProgressionId() {
		return caseProgressionId;
	}


	public long getVersion() {
		return version;
	}
}
