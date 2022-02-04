package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "pet_case_defendant_offence")
@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
public class PetCaseDefendantOffence implements Serializable {
    private static final long serialVersionUID = 4945821424721498615L;

    @EmbeddedId
    private PetCaseDefendantOffenceKey id;
    @Column(name = "case_id")
    private UUID caseId;
    @Column(name = "pet_id")
    private UUID petId;

    public PetCaseDefendantOffence() {
    }

    public PetCaseDefendantOffence(final PetCaseDefendantOffenceKey id, final UUID caseId, final UUID petId) {
        this.id = id;
        this.caseId = caseId;
        this.petId = petId;
    }

    public PetCaseDefendantOffenceKey getId() {
        return id;
    }

    public void setId(PetCaseDefendantOffenceKey id) {
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
        private PetCaseDefendantOffenceKey id;
        private UUID caseId;
        private UUID petId;
        public Builder withPetkey(final PetCaseDefendantOffenceKey id) {
            this.id = id;
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

        public PetCaseDefendantOffence build() {
            return new PetCaseDefendantOffence(id, caseId, petId);
        }
    }
}
