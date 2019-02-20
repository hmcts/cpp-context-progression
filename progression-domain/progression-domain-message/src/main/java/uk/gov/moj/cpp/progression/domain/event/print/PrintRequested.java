package uk.gov.moj.cpp.progression.domain.event.print;

import uk.gov.justice.domain.annotation.Event;

import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("progression.event.print-requested")
public class PrintRequested {

    private final UUID caseId;
    private final UUID notificationId;
    private final UUID materialId;

    @JsonCreator
    public PrintRequested(
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("notificationId") final UUID notificationId,
            @JsonProperty("materialId") final UUID materialId) {
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
        return Objects.equals(caseId, that.caseId) &&
                Objects.equals(notificationId, that.notificationId) &&
                Objects.equals(materialId, that.materialId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseId, notificationId, materialId);
    }

    @Override
    public String toString() {
        return "PrintRequested{" +
                "caseId=" + caseId +
                ", notificationId=" + notificationId +
                ", materialId=" + materialId +
                '}';
    }
}


