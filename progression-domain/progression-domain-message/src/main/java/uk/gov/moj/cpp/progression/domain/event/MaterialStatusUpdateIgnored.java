package uk.gov.moj.cpp.progression.domain.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event("progression.events.material-status-update-ignored")
public class MaterialStatusUpdateIgnored {
    private final UUID materialId;
    private final String status;

    public MaterialStatusUpdateIgnored(final UUID materialId, final String status) {
        this.materialId = materialId;
        this.status = status;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public String getStatus() {
        return status;
    }
}
