package uk.gov.moj.cpp.progression.domain.event.completedsendingsheet;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import uk.gov.justice.domain.annotation.Event;
/**
 * 
 * @deprecated
 *
 */
@Deprecated
@Event("progression.events.sending-sheet-completed")
@JsonIgnoreProperties({"caseProgressionId"})
public class SendingSheetCompleted implements Serializable{

    private static final long serialVersionUID = -5126112201243837214L;

    private Hearing hearing;
    private CrownCourtHearing crownCourtHearing;

    public Hearing getHearing() {
        return this.hearing;
    }

    public void setHearing(final Hearing hearing) {
        this.hearing = hearing;
    }

    public CrownCourtHearing getCrownCourtHearing() {
        return this.crownCourtHearing;
    }

    public void setCrownCourtHearing(final CrownCourtHearing crownCourtHearing) {
        this.crownCourtHearing = crownCourtHearing;
    }
}
