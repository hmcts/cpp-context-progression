package uk.gov.moj.cpp.progression.query.api;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HearingDetails {

    private String type;
    private UUID hearingTypeId;
    private List<UUID> userIds = new ArrayList<>();

    public String getType() {
        return type;
    }

    public List<UUID> getUserIds() {
        return new ArrayList<>(userIds);
    }

    public void setType(final String type) {
        this.type = type;
    }

    public void addUserId(final UUID userId) {
        this.userIds.add(userId);
    }

    public UUID getHearingTypeId() {
        return hearingTypeId;
    }

    public void setHearingTypeId(final UUID hearingTypeId) {
        this.hearingTypeId = hearingTypeId;
    }
}
