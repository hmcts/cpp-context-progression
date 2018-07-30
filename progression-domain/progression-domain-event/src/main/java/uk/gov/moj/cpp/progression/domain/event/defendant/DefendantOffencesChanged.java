package uk.gov.moj.cpp.progression.domain.event.defendant;

import uk.gov.justice.domain.annotation.Event;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Event("progression.events.defendant-offences-changed")
public class DefendantOffencesChanged implements Serializable {

    private static final long serialVersionUID = 1L;
    private final LocalDate modifiedDate = LocalDate.now();
    private List<BaseDefendantOffences> updatedOffences;
    private List<BaseDefendantOffences> deletedOffences;
    private List<BaseDefendantOffences> addedOffences;

    public void setUpdatedOffences(final List<BaseDefendantOffences> updatedOffences) {
        this.updatedOffences = updatedOffences;
    }

    public void setDeletedOffences(final List<BaseDefendantOffences> deletedOffences) {
        this.deletedOffences = deletedOffences;
    }

    public void setAddedOffences(final List<BaseDefendantOffences> addedOffences) {
        this.addedOffences = addedOffences;
    }

    public LocalDate getModifiedDate() {
        return modifiedDate;
    }

    public List<BaseDefendantOffences> getUpdatedOffences() {
        return updatedOffences;
    }

    public List<BaseDefendantOffences> getDeletedOffences() {
        return deletedOffences;
    }

    public List<BaseDefendantOffences> getAddedOffences() {
        return addedOffences;
    }
}
