package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "pet_case_defendant_offence")
@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
public class PetCaseDefendantOffence implements Serializable {
    private static final long serialVersionUID = 3345821424721498600L;

    @Id
    private UUID id; //internal id for uniquely represent pet
    @Column(name = "case_id")
    private UUID caseId;
    @Column(name = "pet_id")
    private UUID petId;
    @Column(name = "is_youth")
    private boolean isYouth;
    @Column(name = "defendant_id")
    private UUID defendantId;

    @Column(name = "last_updated")
    private ZonedDateTime lastUpdated;

    public PetCaseDefendantOffence() {
    }

    public PetCaseDefendantOffence(final UUID id, final UUID caseId, final UUID petId, final boolean isYouth, final UUID defendantId) {
        this.id = id;
        this.caseId = caseId;
        this.petId = petId;
        this.isYouth = isYouth;
        this.defendantId = defendantId;
    }

    public PetCaseDefendantOffence(final UUID id, final UUID caseId, final UUID petId, final boolean isYouth, final UUID defendantId, final ZonedDateTime lastUpdated) {
        this.id = id;
        this.caseId = caseId;
        this.petId = petId;
        this.isYouth = isYouth;
        this.defendantId = defendantId;
        this.lastUpdated = lastUpdated;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getPetId() {
        return petId;
    }

    public void setPetId(UUID petId) {
        this.petId = petId;
    }

    public boolean getIsYouth() {
        return isYouth;
    }

    public void setYouth(final boolean youth) {
        isYouth = youth;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public void setDefendantId(final UUID defendantId) {
        this.defendantId = defendantId;
    }

    public void setIsYouth(boolean isYouth) {
        this.isYouth = isYouth;
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

        final PetCaseDefendantOffence petCaseDefendantOffence = (PetCaseDefendantOffence) o;
        return id.equals(petCaseDefendantOffence.id);
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
        private UUID caseId;
        private UUID petId;
        private UUID defendantId;
        private boolean isYouth;
        private ZonedDateTime lastUpdated;

        public Builder withPetkey(final UUID id) {
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
        public Builder withPetId(final UUID petId) {
            this.petId = petId;
            return this;
        }
        public Builder withIsYouth(final boolean isYouth) {
            this.isYouth = isYouth;
            return this;
        }

        public Builder withLastUpdated(final ZonedDateTime lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        public PetCaseDefendantOffence build() {
            return new PetCaseDefendantOffence(id, caseId, petId, isYouth, defendantId, lastUpdated);
        }
    }
}
