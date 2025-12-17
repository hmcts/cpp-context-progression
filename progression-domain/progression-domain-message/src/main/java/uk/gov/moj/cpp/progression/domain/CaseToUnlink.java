package uk.gov.moj.cpp.progression.domain;

import java.io.Serializable;
import java.util.UUID;

public class CaseToUnlink implements Serializable {
    private static final long serialVersionUID = -7900931036143709202L;

    private final UUID caseId;

    private final String caseUrn;

    private final UUID linkGroupId;

    public CaseToUnlink(UUID caseId, String caseUrn, UUID linkGroupId) {
        this.caseId = caseId;
        this.caseUrn = caseUrn;
        this.linkGroupId = linkGroupId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getCaseUrn() {
        return caseUrn;
    }

    public UUID getLinkGroupId() {
        return linkGroupId;
    }

    public static Builder caseToUnlink(){
        return  new Builder();
    }

    public static class Builder {
        private UUID caseId;

        private String caseUrn;

        private UUID linkGroupId;

        public Builder withCaseId(final UUID caseId){
            this.caseId = caseId;
            return this;
        }

        public Builder withCaseUrn(final String caseUrn){
            this.caseUrn = caseUrn;
            return this;
        }

        public Builder withLinkGroupId(final UUID linkGroupId){
            this.linkGroupId = linkGroupId;
            return this;
        }

        public CaseToUnlink build(){
            return new CaseToUnlink(this.caseId, this.caseUrn, this.linkGroupId);
        }
    }
}
