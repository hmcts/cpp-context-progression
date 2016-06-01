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

    private Long version;

	public SfrIssuesAdded(UUID caseProgressionId, String sfrIssues, Long version) {
		super();
		this.caseProgressionId = caseProgressionId;
		this.sfrIssues = sfrIssues;
		this.version = version;
	}

	public UUID getCaseProgressionId() {
		return caseProgressionId;
	}

	public String getSfrIssues() {
		return sfrIssues;
	}

	public Long getVersion() {
		return version;
	}
}
