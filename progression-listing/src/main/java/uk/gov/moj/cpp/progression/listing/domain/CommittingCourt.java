package uk.gov.moj.cpp.progression.listing.domain;

import static java.util.Optional.empty;

import java.util.Optional;
import java.util.UUID;

public class CommittingCourt {

    private final UUID courtCentreId;

    private Optional<String> courtHouseCode;

    private final String courtHouseName;

    private Optional<String> courtHouseShortName;

    private final CourtHouseType courtHouseType;

    public CommittingCourt(final UUID courtCentreId, final Optional<String> courtHouseCode, final String courtHouseName, final Optional<String> courtHouseShortName, final CourtHouseType courtHouseType) {
        this.courtCentreId = courtCentreId;
        this.courtHouseCode = courtHouseCode;
        this.courtHouseName = courtHouseName;
        this.courtHouseShortName = courtHouseShortName;
        this.courtHouseType = courtHouseType;
    }

    public UUID getCourtCentreId() {
        return courtCentreId;
    }

    public Optional<String> getCourtHouseCode() {
        return courtHouseCode.isPresent() ? courtHouseCode : empty();
    }

    public String getCourtHouseName() {
        return courtHouseName;
    }

    public Optional<String> getCourtHouseShortName() {
        return courtHouseShortName.isPresent() ? courtHouseShortName : empty();
    }

    public CourtHouseType getCourtHouseType() {
        return courtHouseType;
    }

    public static CommittingCourt.Builder committingCourt() {
        return new CommittingCourt.Builder();
    }

    public static class Builder {
        private UUID courtCentreId;

        private String courtHouseCode;

        private String courtHouseName;

        private String courtHouseShortName;

        private CourtHouseType courtHouseType;

        public Builder withCourtCentreId(final UUID courtCentreId) {
            this.courtCentreId = courtCentreId;
            return this;
        }

        public Builder withCourtHouseCode(final String courtHouseCode) {
            this.courtHouseCode = courtHouseCode;
            return this;
        }

        public Builder withCourtHouseCode(final Optional<String> courtHouseCode) {
            this.courtHouseCode = courtHouseCode.orElse(null);
            return this;
        }

        public Builder withCourtHouseName(final String courtHouseName) {
            this.courtHouseName = courtHouseName;
            return this;
        }

        public Builder withCourtHouseShortName(final String courtHouseShortName) {
            this.courtHouseShortName = courtHouseShortName;
            return this;
        }

        public Builder withCourtHouseShortName(final Optional<String> courtHouseShortName) {
            this.courtHouseShortName = courtHouseShortName.orElse(null);
            return this;
        }

        public Builder withCourtHouseType(final CourtHouseType courtHouseType) {
            this.courtHouseType = courtHouseType;
            return this;
        }

        public CommittingCourt build() {
            return new CommittingCourt(courtCentreId, Optional.ofNullable(courtHouseCode), courtHouseName, Optional.ofNullable(courtHouseShortName), courtHouseType);
        }
    }

}
