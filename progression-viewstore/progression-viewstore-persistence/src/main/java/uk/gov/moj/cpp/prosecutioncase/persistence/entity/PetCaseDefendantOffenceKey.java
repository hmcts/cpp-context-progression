package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class PetCaseDefendantOffenceKey implements Serializable {

    private static final long serialVersionUID = 1L;
    @Column(name = "defendant_id", unique = false, nullable = false)
    private UUID defendantId;

    @Column(name = "offence_id", unique = false, nullable = false)
    private UUID offenceId;

    public PetCaseDefendantOffenceKey() {
        //For JPA
    }
    public PetCaseDefendantOffenceKey(final UUID defendantId, final UUID offenceId) {
        this.defendantId = defendantId;
        this.offenceId = offenceId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public void setDefendantId(final UUID defendantId) {
        this.defendantId = defendantId;
    }

    public UUID getOffenceId() {
        return offenceId;
    }

    public void setOffenceId(final UUID offenceId) {
        this.offenceId = offenceId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final PetCaseDefendantOffenceKey petKey = (PetCaseDefendantOffenceKey) o;
        return Objects.equals(defendantId, petKey.defendantId) &&
                Objects.equals(offenceId, petKey.offenceId);
    }
    @Override
    public int hashCode() {
        return Objects.hash(defendantId, offenceId);
    }
}
