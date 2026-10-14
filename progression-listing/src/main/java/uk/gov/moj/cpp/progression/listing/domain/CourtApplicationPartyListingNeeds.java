package uk.gov.moj.cpp.progression.listing.domain;

import static java.util.Optional.ofNullable;

import java.util.Optional;
import java.util.UUID;

public class CourtApplicationPartyListingNeeds {
    private final UUID courtApplicationId;
    private final UUID courtApplicationPartyId;
    private final HearingLanguageNeeds hearingLanguageNeeds;

    public CourtApplicationPartyListingNeeds(UUID applicationId, UUID courtApplicationPartyId, HearingLanguageNeeds hearingLanguageNeeds) {
        this.courtApplicationId = applicationId;
        this.courtApplicationPartyId = courtApplicationPartyId;
        this.hearingLanguageNeeds = hearingLanguageNeeds;
    }

    public UUID getCourtApplicationId() {
        return courtApplicationId;
    }

    public UUID getCourtApplicationPartyId() {
        return courtApplicationPartyId;
    }

    public Optional<HearingLanguageNeeds> getHearingLanguageNeeds() {
        return ofNullable(hearingLanguageNeeds);
    }
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final CourtApplicationPartyListingNeeds that = (CourtApplicationPartyListingNeeds) obj;

        return java.util.Objects.equals(this.courtApplicationId, that.courtApplicationId) &&
                java.util.Objects.equals(this.courtApplicationPartyId, that.courtApplicationPartyId) &&
                java.util.Objects.equals(this.hearingLanguageNeeds, that.hearingLanguageNeeds);
    }
    @Override
    public int hashCode() {
        return java.util.Objects.hash(courtApplicationId, courtApplicationPartyId, hearingLanguageNeeds);
    }

    @Override
    public String toString() {
        return "CourtApplicationPartyListingNeeds{" +
                "courtApplicationId='" + courtApplicationId + "'," +
                "courtApplicationPartyId='" + courtApplicationPartyId + "'," +
                "hearingLanguageNeeds='" + hearingLanguageNeeds + "'" +
                "}";
    }
    public static Builder courtApplicationPartyListingNeeds() {
        return new CourtApplicationPartyListingNeeds.Builder();
    }

    public static final class Builder {
        private UUID courtApplicationId;
        private UUID courtApplicationPartyId;
        private HearingLanguageNeeds hearingLanguageNeeds;

        public Builder withCourtApplicationId(UUID applicationId) {
            this.courtApplicationId = applicationId;
            return this;
        }

        public Builder withCourtApplicationPartyId(UUID courtApplicationPartyId) {
            this.courtApplicationPartyId = courtApplicationPartyId;
            return this;
        }

        public Builder withHearingLanguageNeeds(HearingLanguageNeeds hearingLanguageNeeds) {
            this.hearingLanguageNeeds = hearingLanguageNeeds;
            return this;
        }

        public CourtApplicationPartyListingNeeds build() {
            return new CourtApplicationPartyListingNeeds(courtApplicationId, courtApplicationPartyId, hearingLanguageNeeds);
        }
    }
}
