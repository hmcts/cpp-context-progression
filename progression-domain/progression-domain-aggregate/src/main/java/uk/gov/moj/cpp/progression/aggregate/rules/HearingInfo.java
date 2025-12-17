package uk.gov.moj.cpp.progression.aggregate.rules;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class HearingInfo implements Serializable {

    private static final long serialVersionUID = -8729792116321209691L;

    private UUID hearingId;
    private String hearingType;
    private String jurisdictionType;
    private UUID courtCentreId;
    private String courtCentreName;
    private UUID courtRoomId;
    private String courtRoomName;

    public HearingInfo(final UUID hearingId, final String hearingType, final String jurisdictionType,
                       final UUID courtCentreId, final String courtCentreName,
                       final UUID courtRoomId, final String courtRoomName) {

        this.hearingId = hearingId;
        this.hearingType = hearingType;
        this.jurisdictionType = jurisdictionType;
        this.courtCentreId = courtCentreId;
        this.courtCentreName = courtCentreName;
        this.courtRoomId = courtRoomId;
        this.courtRoomName = courtRoomName;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public String getHearingType() {
        return hearingType;
    }

    public String getJurisdictionType() {
        return jurisdictionType;
    }

    public UUID getCourtCentreId() {
        return courtCentreId;
    }

    public String getCourtCentreName() {
        return courtCentreName;
    }

    public UUID getCourtRoomId() {
        return courtRoomId;
    }

    public String getCourtRoomName() {
        return courtRoomName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final HearingInfo that = (HearingInfo) o;
        return Objects.equals(hearingId, that.hearingId) &&
                Objects.equals(hearingType, that.hearingType) &&
                Objects.equals(jurisdictionType, that.jurisdictionType) &&
                Objects.equals(courtCentreId, that.courtCentreId) &&
                Objects.equals(courtCentreName, that.courtCentreName) &&
                Objects.equals(courtRoomId, that.courtRoomId) &&
                Objects.equals(courtRoomName, that.courtRoomName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hearingId, hearingType, jurisdictionType, courtCentreId, courtCentreName, courtRoomId, courtRoomName);
    }
}
