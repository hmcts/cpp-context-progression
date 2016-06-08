package uk.gov.moj.cpp.progression.domain.event;

import java.time.LocalDate;
import java.util.UUID;

import uk.gov.justice.domain.annotation.Event;

/**
 * 
 * @author jchondig
 *
 */
@Event("progression.events.sending-committal-hearing-information-added")
public class SendingCommittalHearingInformationAdded {

    private UUID caseProgressionId;

    private String fromCourtCentre;

    private LocalDate sendingCommittalDate;

    public SendingCommittalHearingInformationAdded(UUID caseProgressionId, String fromCourtCentre, LocalDate sendingCommittalDate) {
        super();
        this.caseProgressionId = caseProgressionId;
        this.fromCourtCentre = fromCourtCentre;
        this.sendingCommittalDate = sendingCommittalDate;
    }

    public UUID getCaseProgressionId() {
        return caseProgressionId;
    }

    public String getFromCourtCentre() {
        return fromCourtCentre;
    }

    public LocalDate getSendingCommittalDate() {
        return sendingCommittalDate;
    }

}
