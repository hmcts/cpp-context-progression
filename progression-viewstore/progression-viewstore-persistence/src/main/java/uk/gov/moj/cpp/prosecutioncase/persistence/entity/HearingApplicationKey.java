package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class HearingApplicationKey implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "hearing_id", nullable = false)
    private UUID hearingId;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    public HearingApplicationKey() {

    }

    public HearingApplicationKey(final UUID applicationId, final UUID hearingId) {
        this.applicationId = applicationId;
        this.hearingId = hearingId;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public void setHearingId(final UUID hearingId) {
        this.hearingId = hearingId;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(UUID applicationId) {
        this.applicationId = applicationId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.applicationId, this.hearingId);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (null == o || getClass() != o.getClass()) {
            return false;
        }
        return Objects.equals(this.applicationId, ((HearingApplicationKey) o).applicationId)
                && Objects.equals(this.hearingId, ((HearingApplicationKey) o).hearingId);
    }

    @Override
    public String toString() {
        return "HearingApplicationKey [applicationId=" + applicationId + ", hearingId=" + hearingId + "]";
    }
}
