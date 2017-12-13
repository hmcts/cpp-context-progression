package uk.gov.moj.cpp.progression.domain.event.defendant;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event("progression.events.defence-solicitor-firm-for-defendant-updated")
public class DefenceSolicitorFirmUpdatedForDefendant {

    private final UUID caseId;
    private final UUID defendantId;
    private final String defenceSolicitorFirm;

    public DefenceSolicitorFirmUpdatedForDefendant(UUID caseId, UUID defendentId,
                    String defenceSolicitorFirm) {
        super();
        this.caseId = caseId;
        this.defendantId = defendentId;
        this.defenceSolicitorFirm = defenceSolicitorFirm;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getDefenceSolicitorFirm() {
        return defenceSolicitorFirm;
    }

}
