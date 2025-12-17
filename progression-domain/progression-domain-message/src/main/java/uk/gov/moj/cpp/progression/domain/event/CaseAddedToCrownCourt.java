package uk.gov.moj.cpp.progression.domain.event;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import uk.gov.justice.domain.annotation.Event;

/**
 * * @deprecated
 * @author jchondig
 *
 */
@Deprecated
@Event("progression.events.case-added-to-crown-court")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseAddedToCrownCourt implements Serializable {

    private final UUID caseId;

    private final String courtCentreId;


    public CaseAddedToCrownCourt(final UUID caseId, final String courtCentreId) {
        super();
        this.caseId = caseId;
        this.courtCentreId = courtCentreId;
    }


    public UUID getCaseId() {
        return caseId;
    }

    public String getCourtCentreId() {
        return courtCentreId;
    }
}
