package uk.gov.moj.cpp.progression.listing.domain;

import java.io.Serializable;
import java.util.UUID;

@SuppressWarnings({"squid:S00107", "squid:S2789", "pmd:BeanMembersShouldSerialize", "squid:S00122", "squid:S2789", "squid:S00121", "squid:S1067"})
public class SeedingHearing implements Serializable {
    private final JurisdictionType jurisdictionType;

    private final UUID seedingHearingId;

    private final String sittingDay;

    public SeedingHearing(final JurisdictionType jurisdictionType, final UUID seedingHearingId, final String sittingDay) {
        this.jurisdictionType = jurisdictionType;
        this.seedingHearingId = seedingHearingId;
        this.sittingDay = sittingDay;
    }

    public JurisdictionType getJurisdictionType() {
        return jurisdictionType;
    }

    public UUID getSeedingHearingId() {
        return seedingHearingId;
    }

    public String getSittingDay() {
        return sittingDay;

    }

    public static Builder seedingHearing() {
        return new SeedingHearing.Builder();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final SeedingHearing that = (SeedingHearing) obj;

        return java.util.Objects.equals(this.jurisdictionType, that.jurisdictionType) &&
                java.util.Objects.equals(this.seedingHearingId, that.seedingHearingId) &&
                java.util.Objects.equals(this.sittingDay, that.sittingDay);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(jurisdictionType, seedingHearingId, sittingDay);
    }

    @Override
    public String toString() {
        return "SeedingHearing{" +
                "jurisdictionType='" + jurisdictionType + "'," +
                "seedingHearingId='" + seedingHearingId + "'," +
                "sittingDay='" + sittingDay + "'" +
                "}";
    }


    public static class Builder {
        private JurisdictionType jurisdictionType;

        private UUID seedingHearingId;

        private String sittingDay;

        public Builder withJurisdictionType(final JurisdictionType jurisdictionType) {
            this.jurisdictionType = jurisdictionType;
            return this;
        }

        public Builder withSeedingHearingId(final UUID seedingHearingId) {
            this.seedingHearingId = seedingHearingId;
            return this;
        }

        public Builder withSittingDay(final String sittingDay) {
            this.sittingDay = sittingDay;
            return this;
        }


        public Builder withValuesFrom(final SeedingHearing seedingHearing) {
            this.jurisdictionType = seedingHearing.getJurisdictionType();
            this.seedingHearingId = seedingHearing.getSeedingHearingId();
            this.sittingDay = seedingHearing.getSittingDay();
            return this;
        }

        public SeedingHearing build() {
            return new SeedingHearing(jurisdictionType, seedingHearingId, sittingDay);
        }
    }
}
