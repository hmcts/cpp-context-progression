package uk.gov.moj.cpp.progression.domain.event;

import java.util.UUID;

import uk.gov.justice.domain.annotation.Event;

/**
 * 
 * @author jchondig
 *
 */
@Event("progression.event.defence-issues-added")
public class DefenceIssuesAdded  {

	private UUID caseProgressionId;
	
    private String defenceIssues;

    private Long version;

	public DefenceIssuesAdded(UUID caseProgressionId, String defenceIssues, Long version) {
		super();
		this.caseProgressionId = caseProgressionId;
		this.defenceIssues = defenceIssues;
		this.version = version;
	}

	public UUID getCaseProgressionId() {
		return caseProgressionId;
	}

	public String getDefenceIssues() {
		return defenceIssues;
	}

	public Long getVersion() {
		return version;
	}
}
