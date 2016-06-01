package uk.gov.moj.cpp.progression.domain.event;

import java.time.LocalDate;
import java.util.UUID;

import uk.gov.justice.domain.annotation.Event;

@Event("progression.event.case-sent-to-crown-court")
public class CaseSentToCrownCourt {
	
	private UUID caseProgressionId;

	private UUID caseId;

	private LocalDate dateOfSending;
	
	private Long version;

	public CaseSentToCrownCourt(UUID caseProgressionId, UUID caseId, LocalDate dateOfSending, Long version) {
		super();
		this.caseProgressionId = caseProgressionId;
		this.caseId = caseId;
		this.dateOfSending = dateOfSending;
		this.version = version;
	}

	public UUID getCaseProgressionId() {
		return caseProgressionId;
	}

	public UUID getCaseId() {
		return caseId;
	}

	public LocalDate getDateOfSending() {
		return dateOfSending;
	}

	public Long getVersion() {
		return version;
	}
}
