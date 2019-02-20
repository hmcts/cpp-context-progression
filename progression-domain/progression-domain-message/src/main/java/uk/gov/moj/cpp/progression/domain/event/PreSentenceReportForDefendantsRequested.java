package uk.gov.moj.cpp.progression.domain.event;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantPSR;
/**
 * 
 * @deprecated This is deprecated for Release 2.4
 *
 */
@Deprecated
@Event("progression.events.pre-sentence-report-for-defendants-requested")
@JsonIgnoreProperties({"caseProgressionId"})
public class PreSentenceReportForDefendantsRequested {

    private final UUID caseId;
    private final List<DefendantPSR> defendants;

    public PreSentenceReportForDefendantsRequested(final UUID caseId, final List<DefendantPSR> defendants) {
        this.caseId = caseId;
        this.defendants = defendants;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public List<DefendantPSR> getDefendants() {
        return defendants;
    }
}
