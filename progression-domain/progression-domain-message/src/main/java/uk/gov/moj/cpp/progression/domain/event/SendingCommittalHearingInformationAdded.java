package uk.gov.moj.cpp.progression.domain.event;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import uk.gov.justice.domain.annotation.Event;

/**
 * 
 * @deprecated This is deprecated for Release 2.4
 *
 */
@Deprecated
@Event("progression.events.sending-committal-hearing-information-added")
@JsonIgnoreProperties({"caseProgressionId"})
public class SendingCommittalHearingInformationAdded {

    private UUID caseId;

    private String fromCourtCentre;

    private LocalDate sendingCommittalDate;

    private String courtCenterID;

    public SendingCommittalHearingInformationAdded(final UUID caseId, final String fromCourtCentre, final LocalDate sendingCommittalDate, final String courtCenterID) {
        super();
        this.caseId = caseId;
        this.fromCourtCentre = fromCourtCentre;
        this.sendingCommittalDate = sendingCommittalDate;
        this.courtCenterID = courtCenterID;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getFromCourtCentre() {
        return fromCourtCentre;
    }

    public LocalDate getSendingCommittalDate() {
        return sendingCommittalDate;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public void setFromCourtCentre(final String fromCourtCentre) {
        this.fromCourtCentre = fromCourtCentre;
    }

    public void setSendingCommittalDate(final LocalDate sendingCommittalDate) {
        this.sendingCommittalDate = sendingCommittalDate;
    }

    public String getCourtCenterID() {
        return courtCenterID;
    }

    public void setCourtCenterID(final String courtCenterID) {
        this.courtCenterID = courtCenterID;
    }
}
