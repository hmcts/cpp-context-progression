package uk.gov.moj.cpp.progression.command.defendant;

import java.util.UUID;

public class UpdateDefendantDefenceSolicitorFirm {

    private final UUID caseId;
    private final UUID defendantId;
    private final String defenceSolicitorFirm;

    public UpdateDefendantDefenceSolicitorFirm(UUID caseId, UUID defendantId,
                    String defenceSolicitorFirm) {
        super();
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.defenceSolicitorFirm = defenceSolicitorFirm;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public String getDefenceSolicitorFirm() {
        return defenceSolicitorFirm;
    }

}
