package uk.gov.moj.cpp.progression.command;

import java.time.LocalDate;
import java.util.UUID;

import uk.gov.moj.cpp.progression.domain.constant.HearingTypeEnum;

public class ListHearing {
	private UUID caseId;

	private HearingTypeEnum hearingType;

	private String courtCentreName;

	private LocalDate dateOfSending;

	private Integer duration;
	
	public ListHearing(UUID caseId, HearingTypeEnum hearingType, String courtCentreName, LocalDate dateOfSending,
			Integer duration) {
		super();
		this.caseId = caseId;
		this.hearingType = hearingType;
		this.courtCentreName = courtCentreName;
		this.dateOfSending = dateOfSending;
		this.duration = duration;
	}

	public UUID getCaseId() {
		return caseId;
	}

	public HearingTypeEnum getHearingType() {
		return hearingType;
	}

	public String getCourtCentreName() {
		return courtCentreName;
	}

	public LocalDate getDateOfSending() {
		return dateOfSending;
	}

	public Integer getDuration() {
		return duration;
	}



}
