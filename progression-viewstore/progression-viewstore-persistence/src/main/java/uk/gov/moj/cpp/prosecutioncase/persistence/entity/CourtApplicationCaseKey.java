package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
public class CourtApplicationCaseKey implements Serializable {
    private static final long serialVersionUID = 4484701847317100837L;

    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    public CourtApplicationCaseKey() {

    }

    public CourtApplicationCaseKey(UUID id, UUID applicationId, UUID caseId) {
        this.id = id;
        this.applicationId = applicationId;
        this.caseId = caseId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final CourtApplicationCaseKey that = (CourtApplicationCaseKey) o;
        return id.equals(that.id) &&
                Objects.equals(applicationId, that.applicationId) &&
                Objects.equals(caseId, that.caseId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, applicationId, caseId);
    }

    @Override
    public String toString() {
        return "CourtApplicationCaseKey{" +
                "id=" + id +
                ", applicationId=" + applicationId +
                ", caseId=" + caseId +
                '}';
    }
}
