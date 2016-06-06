package uk.gov.moj.cpp.progression.domain.event;

import java.time.LocalDate;
import java.util.UUID;

import uk.gov.justice.domain.annotation.Event;

@Event("progression.event.sentence-hearing-date-added")
public class SentenceHearingDateAdded {

	public final static String EVENT_NAME = "progression.events.ptp-hearing-vacated";


	private UUID caseProgressionId;


	private LocalDate sentenceHearingDate;

	public SentenceHearingDateAdded(UUID caseProgressionId, LocalDate sentenceHearingDate) {

		super();
		this.caseProgressionId = caseProgressionId;
		this.sentenceHearingDate = sentenceHearingDate;
	}

	public static String getEventName() {
		return EVENT_NAME;
	}

	public UUID getCaseProgressionId() {
		return caseProgressionId;
	}


	public LocalDate getSentenceHearingDate() {
		return sentenceHearingDate;
	}
}
