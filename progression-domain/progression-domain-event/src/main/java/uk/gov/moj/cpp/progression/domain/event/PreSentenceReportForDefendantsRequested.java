package uk.gov.moj.cpp.progression.domain.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantPSR;

import java.util.List;
import java.util.UUID;

@Event("progression.events.pre-sentence-report-for-defendants-requested")
public class PreSentenceReportForDefendantsRequested {

    private final UUID caseProgressionId;
    private final List<DefendantPSR> defendants;

    public PreSentenceReportForDefendantsRequested(UUID caseProgressionId, List<DefendantPSR> defendants) {
        this.caseProgressionId = caseProgressionId;
        this.defendants = defendants;
    }

    public UUID getCaseProgressionId() {
        return caseProgressionId;
    }

    public List<DefendantPSR> getDefendants() {
        return defendants;
    }
}
