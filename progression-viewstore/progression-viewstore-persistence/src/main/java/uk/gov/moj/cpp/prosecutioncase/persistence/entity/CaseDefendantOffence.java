package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import uk.gov.justice.core.courts.FormType;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "form_case_defendant_offence")
@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
public class CaseDefendantOffence implements Serializable {
    private static final long serialVersionUID = 4896206475180611344L;

    @Id
    @Column(unique = true, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "defendant_id", nullable = false)
    private UUID defendantId;

    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    @Column(name = "court_form_id", nullable = false)
    private UUID courtFormId;

    @Column(name = "form_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private FormType formType;

    @Column(name = "last_updated")
    private ZonedDateTime lastUpdated;

    public CaseDefendantOffence() {
    }

    public CaseDefendantOffence(final UUID id, final UUID defendantId, final UUID caseId, final UUID courtFormId, final FormType formType) {
        this.id = id;
        this.defendantId = defendantId;
        this.caseId = caseId;
        this.courtFormId = courtFormId;
        this.formType = formType;
    }

    public CaseDefendantOffence(final UUID id, final UUID defendantId, final UUID caseId, final UUID courtFormId, final FormType formType, final ZonedDateTime lastUpdated) {
        this.id = id;
        this.defendantId = defendantId;
        this.caseId = caseId;
        this.courtFormId = courtFormId;
        this.formType = formType;
        this.lastUpdated = lastUpdated;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public void setDefendantId(UUID defendantId) {
        this.defendantId = defendantId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCourtFormId() {
        return courtFormId;
    }

    public void setCourtFormId(UUID courtFormId) {
        this.courtFormId = courtFormId;
    }

    public FormType getFormType() {
        return formType;
    }

    public void setFormType(final FormType formType) {
        this.formType = formType;
    }

    public ZonedDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(final ZonedDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final CaseDefendantOffence caseDefendantOffence = (CaseDefendantOffence) o;
        return id.equals(caseDefendantOffence.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private UUID id;
        private UUID defendantId;
        private UUID caseId;
        private UUID courtFormId;
        private FormType formType;
        private ZonedDateTime lastUpdated;

        public Builder withId(final UUID id) {
            this.id = id;
            return this;
        }
        public Builder withDefendantId(final UUID defendantId) {
            this.defendantId = defendantId;
            return this;
        }

        public Builder withCaseId(final UUID caseId) {
            this.caseId = caseId;
            return this;
        }

        public Builder withCourtFormId(final UUID courtFormId) {
            this.courtFormId = courtFormId;
            return this;
        }

        public Builder withFormType(final FormType formType) {
            this.formType = formType;
            return this;
        }

        public Builder withLastUpdated(final ZonedDateTime lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        public CaseDefendantOffence build() {
            return new CaseDefendantOffence(id, defendantId, caseId, courtFormId, formType, lastUpdated);
        }
    }
}
