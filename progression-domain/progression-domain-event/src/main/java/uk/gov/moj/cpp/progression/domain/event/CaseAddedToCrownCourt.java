package uk.gov.moj.cpp.progression.domain.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;

/**
 * 
 * @author jchondig
 *
 */
@Event("progression.events.case-added-to-crown-court")
public class CaseAddedToCrownCourt {

    private final UUID caseId;

    private final String courtCentreId;


    public CaseAddedToCrownCourt(UUID caseId, String courtCentreId) {
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
