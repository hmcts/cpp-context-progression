package uk.gov.moj.cpp.progression.domain.event;

import java.util.UUID;

import uk.gov.justice.domain.annotation.Event;

/**
 * 
 * @author jchondig
 *
 */
@Event("progression.event.pre-sentence-report-ordered")
public class PreSentenceReportOrdered  {

	private UUID caseProgressionId;
	
	private Boolean isPSROrdered;
	
    public UUID getCaseProgressionId() {
		return caseProgressionId;
	}

    public Boolean getIsPSROrdered() {
        return isPSROrdered;
    }
    
    public PreSentenceReportOrdered(UUID caseProgressionId,
            Boolean isPSROrdered) {
        super();
        this.caseProgressionId = caseProgressionId;
        this.isPSROrdered = isPSROrdered;
    }

}
