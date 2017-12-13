package uk.gov.moj.cpp.progression.domain.event.defendant;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event("progression.events.plea-updated")
public class PleaUpdated {
    private final String caseId;
    private final String offenceId;
    private final String plea;


    public PleaUpdated(String caseId, String offenceId, String plea) {
        this.caseId = caseId;
        this.offenceId = offenceId;
        this.plea = plea;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getOffenceId() {
        return offenceId;
    }

    public String getPlea() {
        return plea;
    }

}
