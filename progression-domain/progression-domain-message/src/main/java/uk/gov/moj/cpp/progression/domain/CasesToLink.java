package uk.gov.moj.cpp.progression.domain;

import uk.gov.moj.cpp.progression.domain.event.link.LinkType;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public class CasesToLink implements Serializable {

    private static final long serialVersionUID = 4150539872337376511L;

    private final UUID prosecutionCaseId;

    private final List<String> caseUrns;

    private final LinkType linkType;

    public CasesToLink(final UUID prosecutionCaseId, final List<String> caseUrns, final LinkType linkType) {
        this.prosecutionCaseId = prosecutionCaseId;
        this.caseUrns = caseUrns;
        this.linkType = linkType;
    }

    public UUID getProsecutionCaseId() {
        return prosecutionCaseId;
    }

    public List<String> getCaseUrns() {
        return caseUrns;
    }

    public LinkType getLinkType() {
        return linkType;
    }

    public static Builder casesToLink() {
        return new Builder();
    }

    public static class Builder {

        private UUID prosecutionCaseId;
        private List<String> caseUrns;
        private LinkType linkType;

        public Builder withProsecutionCaseId(final UUID prosecutionCaseId) {
            this.prosecutionCaseId = prosecutionCaseId;
            return this;
        }

        public Builder withCaseUrns(final List<String> caseUrns) {
            this.caseUrns = caseUrns;
            return this;
        }

        public Builder withLinkType(final LinkType linkType) {
            this.linkType = linkType;
            return this;
        }

        public CasesToLink build() {
            return new CasesToLink(prosecutionCaseId, caseUrns, linkType);
        }

    }
}
