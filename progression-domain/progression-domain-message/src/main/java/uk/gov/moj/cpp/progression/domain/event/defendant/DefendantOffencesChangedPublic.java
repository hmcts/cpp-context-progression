package uk.gov.moj.cpp.progression.domain.event.defendant;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
/**
 * 
 * @deprecated
 *
 */
@Deprecated
public class DefendantOffencesChangedPublic implements Serializable {

    private static final long serialVersionUID = 1L;
    private final LocalDate modifiedDate = LocalDate.now();
    private List<DefendantOffences> updatedOffences;
    private List<DeletedDefendantOffences> deletedOffences;
    private List<DefendantOffences> addedOffences;

    public void setUpdatedOffences(final List<DefendantOffences> updatedOffences) {
        this.updatedOffences = updatedOffences;
    }

    public void setDeletedOffences(final List<DeletedDefendantOffences> deletedOffences) {
        this.deletedOffences = deletedOffences;
    }

    public void setAddedOffences(final List<DefendantOffences> addedOffences) {
        this.addedOffences = addedOffences;
    }

    public LocalDate getModifiedDate() {
        return modifiedDate;
    }

    public List<DefendantOffences> getUpdatedOffences() {
        return updatedOffences;
    }

    public List<DeletedDefendantOffences> getDeletedOffences() {
        return deletedOffences;
    }

    public List<DefendantOffences> getAddedOffences() {
        return addedOffences;
    }

}
