package uk.gov.moj.cpp.progression.domain.event;

import java.util.UUID;

import uk.gov.justice.domain.annotation.Event;

/**
 * 
 * @author jchondig
 *
 */
@Event("progression.event.sfr-issues-added")
public class SfrIssuesAdded  {

	private UUID caseProgressionId;
	
    private String sfrIssues;

	public SfrIssuesAdded(UUID caseProgressionId, String sfrIssues) {
		super();
		this.caseProgressionId = caseProgressionId;
		this.sfrIssues = sfrIssues;
	}

	public UUID getCaseProgressionId() {
		return caseProgressionId;
	}

	public String getSfrIssues() {
		return sfrIssues;
	}

}
