package uk.gov.moj.cpp.progression.domain.pojo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class RemoveOffencesFromExistingHearing implements Serializable {

    private static final long serialVersionUID = -235873276691234565L;

    private final UUID hearingId;

    private final List<UUID> offenceIds;

    @JsonCreator
    public RemoveOffencesFromExistingHearing(@JsonProperty("hearingId") final UUID hearingId,
                                             @JsonProperty("offenceIds") final List<UUID> offenceIds) {
        this.hearingId = hearingId;
        this.offenceIds = new ArrayList<>(offenceIds);
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public List<UUID> getOffenceIds() {
        return new ArrayList<>(offenceIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.hearingId, this.offenceIds);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final RemoveOffencesFromExistingHearing that = (RemoveOffencesFromExistingHearing) o;
        return Objects.equals(this.hearingId, that.hearingId)
                && Objects.equals(this.offenceIds, that.offenceIds);
    }

}

