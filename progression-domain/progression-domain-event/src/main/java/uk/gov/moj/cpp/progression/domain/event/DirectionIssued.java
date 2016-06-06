package uk.gov.moj.cpp.progression.domain.event;

import java.time.LocalDate;
import java.util.UUID;

import uk.gov.justice.domain.annotation.Event;

/**
 * 
 * @author jchondig
 *
 */
@Event("progression.event.direction-issued")
public class DirectionIssued  {

	private UUID caseProgressionId;
	
	private LocalDate directionIssuedDate;
	
	public DirectionIssued(UUID caseProgressionId, LocalDate directionIssuedDate) {
		super();
		this.caseProgressionId = caseProgressionId;
		this.directionIssuedDate = directionIssuedDate;
	}

	public UUID getCaseProgressionId() {
		return caseProgressionId;
	}

	public LocalDate getDirectionIssuedDate() {
		return directionIssuedDate;
	}

}
