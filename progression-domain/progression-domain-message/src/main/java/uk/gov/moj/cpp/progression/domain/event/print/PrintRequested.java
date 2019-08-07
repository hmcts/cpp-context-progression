package uk.gov.moj.cpp.progression.domain.event.print;

import uk.gov.justice.domain.annotation.Event;

import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("progression.event.print-requested")
public class PrintRequested {

    private final UUID applicationId;
    private final UUID caseId;
    private final UUID notificationId;
    private final UUID materialId;

    @JsonCreator
    public PrintRequested(
            @JsonProperty("notificationId") final UUID notificationId,
            @JsonProperty("applicationId") final UUID applicationId,
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("materialId") final UUID materialId) {
        this.applicationId = applicationId;
        this.caseId = caseId;
        this.notificationId = notificationId;
        this.materialId = materialId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public UUID getApplicationId() { return applicationId; }

    @SuppressWarnings("squid:S00121")
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final PrintRequested that = (PrintRequested) o;
        return Objects.equals(applicationId, that.applicationId) &&
                Objects.equals(caseId, that.caseId) &&
                Objects.equals(notificationId, that.notificationId) &&
                Objects.equals(materialId, that.materialId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(applicationId, caseId, notificationId, materialId);
    }

    @Override
    public String toString() {
        return "PrintRequested{" +
                "applicationId=" + applicationId +
                "caseId=" + caseId +
                ", notificationId=" + notificationId +
                ", materialId=" + materialId +
                '}';
    }
}


