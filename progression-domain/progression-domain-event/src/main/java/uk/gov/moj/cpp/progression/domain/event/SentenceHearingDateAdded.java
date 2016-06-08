package uk.gov.moj.cpp.progression.domain.event;

import java.time.LocalDate;
import java.util.UUID;

import uk.gov.justice.domain.annotation.Event;

@Event("progression.events.sentence-hearing-date-added")
public class SentenceHearingDateAdded {

	private UUID caseProgressionId;


	private LocalDate sentenceHearingDate;

	public SentenceHearingDateAdded(UUID caseProgressionId, LocalDate sentenceHearingDate) {

		super();
		this.caseProgressionId = caseProgressionId;
		this.sentenceHearingDate = sentenceHearingDate;
	}


	public UUID getCaseProgressionId() {
		return caseProgressionId;
	}


	public LocalDate getSentenceHearingDate() {
		return sentenceHearingDate;
	}
}
