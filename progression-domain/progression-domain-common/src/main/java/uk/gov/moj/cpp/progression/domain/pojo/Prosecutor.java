package uk.gov.moj.cpp.progression.domain.pojo;

import java.io.Serializable;
import java.util.UUID;

public class Prosecutor implements Serializable {

    private UUID caseId;
    private UUID prosecutionAuthorityId;

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getProsecutionAuthorityId() {
        return prosecutionAuthorityId;
    }

    public void setProsecutionAuthorityId(final UUID prosecutionAuthorityId) {
        this.prosecutionAuthorityId = prosecutionAuthorityId;
    }
}
