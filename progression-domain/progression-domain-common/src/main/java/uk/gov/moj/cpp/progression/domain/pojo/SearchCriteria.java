package uk.gov.moj.cpp.progression.domain.pojo;

import static java.util.Optional.empty;

import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("PMD.BeanMembersShouldSerialize")
public class SearchCriteria {

    private final Optional<UUID> defendantId;
    private final UUID caseId;
    private final Optional<String> sectionId;
    private final Optional<String> documentName;


    public SearchCriteria(final Optional<UUID> defendantId, final UUID caseId, final Optional<String> sectionId, final Optional<String> documentName) {
        this.defendantId = defendantId;
        this.caseId = caseId;
        this.sectionId = sectionId;
        this.documentName = documentName;
    }

    public Optional<UUID> getDefendantId() {
        return defendantId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public Optional<String> getSectionId() {
        return sectionId;
    }

    public Optional<String> getDocumentName() {
        return documentName;
    }

    public static Builder searchCriteria() {
        return new Builder();
    }

    @Override
    @SuppressWarnings("squid:S1067")
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final SearchCriteria that = (SearchCriteria) obj;

        return java.util.Objects.equals(this.defendantId, that.defendantId) &&
                java.util.Objects.equals(this.caseId, that.caseId) &&
                java.util.Objects.equals(this.sectionId, that.sectionId) &&
                java.util.Objects.equals(this.documentName, that.documentName);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(defendantId, caseId, sectionId, documentName);
    }

    @Override
    public String toString() {
        return "SearchCriteria{" +
                "defendantId='" + defendantId + "'," +
                "caseId='" + caseId + "'," +
                "sectionId='" + sectionId + "'," +
                "documentName='" + documentName + "'" +
                "}";
    }

    public static class Builder {

        private Optional<UUID> defendantId = empty();
        private Optional<String> sectionId = empty();
        private Optional<String> documentName = empty();
        private UUID caseId;

        public Builder withDefendantId(final Optional<UUID> defendantId) {
            this.defendantId = defendantId;
            return this;
        }

        public Builder withCaseId(final UUID caseId) {
            this.caseId = caseId;
            return this;
        }

        public Builder withSectionId(final Optional<String> sectionId) {
            this.sectionId = sectionId;
            return this;
        }

        public Builder withDocumentName(final Optional<String> documentName) {
            this.documentName = documentName;
            return this;
        }


        public Builder withValuesFrom(final SearchCriteria searchCriteria) {
            this.caseId = searchCriteria.getCaseId();
            this.defendantId = searchCriteria.getDefendantId();
            this.sectionId = searchCriteria.getSectionId();
            this.documentName = searchCriteria.getDocumentName();
            return this;
        }

        public SearchCriteria build() {
            return new SearchCriteria(defendantId, caseId, sectionId, documentName);
        }
    }

}
