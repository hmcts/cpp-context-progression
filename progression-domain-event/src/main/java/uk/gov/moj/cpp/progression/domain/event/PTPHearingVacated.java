package uk.gov.moj.cpp.progression.domain.event;

import java.time.LocalDate;
import java.util.UUID;

import uk.gov.justice.domain.annotation.Event;

@Event("progression.event.ptp-hearing-vacated")
public class PTPHearingVacated {

	public final static String EVENT_NAME = "progression.events.ptp-hearing-vacated";


	private UUID caseProgressionId;

	private Long version;

	private LocalDate ptpHearingVacatedDate;

	public PTPHearingVacated(UUID caseProgressionId, LocalDate ptpHearingVacatedDate, Long version) {

		super();
		this.caseProgressionId = caseProgressionId;
		this.version = version;
		this.ptpHearingVacatedDate = ptpHearingVacatedDate;
	}

	public static String getEventName() {
		return EVENT_NAME;
	}

	public UUID getCaseProgressionId() {
		return caseProgressionId;
	}

	public Long getVersion() {
		return version;
	}

	public LocalDate getPtpHearingVacatedDate() {
		return ptpHearingVacatedDate;
	}
}
