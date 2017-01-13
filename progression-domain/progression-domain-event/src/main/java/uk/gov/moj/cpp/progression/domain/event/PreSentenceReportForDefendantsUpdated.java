package uk.gov.moj.cpp.progression.domain.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantPreSentenceReportRequested;

import java.util.List;
import java.util.UUID;

@Event("progression.events.pre-sentence-report-for-defendants-updated")
public class PreSentenceReportForDefendantsUpdated {

    // TODO: Because of the JsonToObjectConverter, do these need to have setters and hence can't be final?

    private final UUID caseProgressionId;
    private final List<DefendantPreSentenceReportRequested> defendantPsrsRequested;

    public PreSentenceReportForDefendantsUpdated(UUID caseProgressionId, List<DefendantPreSentenceReportRequested> defendantPsrsRequested) {
        this.caseProgressionId = caseProgressionId;
        this.defendantPsrsRequested = defendantPsrsRequested;
    }

    public UUID getCaseProgressionId() {
        return caseProgressionId;
    }

    public List<DefendantPreSentenceReportRequested> getDefendantPsrsRequested() {
        return defendantPsrsRequested;
    }

    // TODO: toString...
}
