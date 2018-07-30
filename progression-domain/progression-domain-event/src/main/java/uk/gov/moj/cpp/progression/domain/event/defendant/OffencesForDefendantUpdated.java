package uk.gov.moj.cpp.progression.domain.event.defendant;


import uk.gov.justice.domain.annotation.Event;

import java.util.List;
import java.util.UUID;

@Event("progression.events.offences-for-defendant-updated")
public class OffencesForDefendantUpdated {

    private final UUID caseId;

    private final UUID defendantId;
                                                                       
    private final List<OffenceForDefendant> offences;

    public OffencesForDefendantUpdated(UUID caseId, UUID defendantId, List<OffenceForDefendant> offences) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.offences = offences;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public List<OffenceForDefendant> getOffences() {
        return offences;
    }
}
