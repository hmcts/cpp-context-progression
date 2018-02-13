package uk.gov.moj.cpp.progression.domain.event;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import uk.gov.justice.domain.annotation.Event;

/**
 * 
 * @author jchondig
 *
 */
@Event("progression.events.sending-committal-hearing-information-added")
@JsonIgnoreProperties({"caseProgressionId"})
public class SendingCommittalHearingInformationAdded {

    private UUID caseId;

    private String fromCourtCentre;

    private LocalDate sendingCommittalDate;

    public SendingCommittalHearingInformationAdded(UUID caseId, String fromCourtCentre, LocalDate sendingCommittalDate) {
        super();
        this.caseId = caseId;
        this.fromCourtCentre = fromCourtCentre;
        this.sendingCommittalDate = sendingCommittalDate;
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

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public void setFromCourtCentre(String fromCourtCentre) {
        this.fromCourtCentre = fromCourtCentre;
    }

    public void setSendingCommittalDate(LocalDate sendingCommittalDate) {
        this.sendingCommittalDate = sendingCommittalDate;
    }

    
}
