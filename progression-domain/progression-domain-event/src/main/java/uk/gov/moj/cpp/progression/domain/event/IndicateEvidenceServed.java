package uk.gov.moj.cpp.progression.domain.event;

import java.time.LocalDate;
import java.util.UUID;

import uk.gov.justice.domain.annotation.Event;

/**
 * Event to indicate a request to indicate a evidence serve has been raised.
 * 
 * @author jchondig
 *
 */
@Event("progression.events.indicate-evidence-served")
public class IndicateEvidenceServed {

	private UUID indicateEvidenceServedId;

	private UUID caseId;

	private Boolean isKeyEvidence;

	private String evidenceName;

	private LocalDate planDate;

	public IndicateEvidenceServed(UUID indicateEvidenceServedId, UUID caseId, LocalDate planDate,
			String evidenceName, Boolean isKeyEvidence) {
		super();
		this.indicateEvidenceServedId = indicateEvidenceServedId;
		this.caseId = caseId;
		this.isKeyEvidence = isKeyEvidence;
		this.evidenceName = evidenceName;
		this.planDate = planDate;
	}

	public UUID getIndicateEvidenceServedId() {
	    return indicateEvidenceServedId;
	}

	public UUID getCaseId() {
		return caseId;
	}

	public Boolean getIsKeyEvidence() {
		return isKeyEvidence;
	}

	public String getEvidenceName() {
		return evidenceName;
	}

	public LocalDate getPlanDate() {
		return planDate;
	}

}
