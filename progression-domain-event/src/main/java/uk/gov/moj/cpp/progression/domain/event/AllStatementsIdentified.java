package uk.gov.moj.cpp.progression.domain.event;

import java.util.UUID;

import uk.gov.justice.domain.annotation.Event;

/**
 * Event to indicate a case has been sent to crown court.
 * 
 * @author hasan shaik
 *
 */
@Event("progression.event.all-statements-identified")
public class AllStatementsIdentified {
	
	private UUID caseProgressionId;
	

	public AllStatementsIdentified() {
	    super();
	}
	 
    public AllStatementsIdentified(UUID caseProgressionId) {
        super();
        this.caseProgressionId = caseProgressionId;
    }

	public UUID getCaseProgressionId() {
		return caseProgressionId;
	}

}
