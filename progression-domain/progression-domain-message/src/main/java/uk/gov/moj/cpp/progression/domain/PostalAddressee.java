package uk.gov.moj.cpp.progression.domain;

import uk.gov.moj.cpp.progression.common.CourtApplicationPartyType;

import java.util.UUID;

public class PostalAddressee {
    private UUID courtApplicationPartyId;

    private String name;

    private PostalAddress address;

    private CourtApplicationPartyType courtApplicationPartyType;

    private UUID prosecutionAuthorityId;

    public PostalAddressee(final UUID courtApplicationPartyId, final String name, final PostalAddress address, final CourtApplicationPartyType courtApplicationPartyType, final UUID prosecutionAuthorityId) {
        this.courtApplicationPartyId = courtApplicationPartyId;
        this.name = name;
        this.address = address;
        this.courtApplicationPartyType = courtApplicationPartyType;
        this.prosecutionAuthorityId = prosecutionAuthorityId;
    }

    public String getName() {
        return name;
    }

    public UUID getCourtApplicationPartyId() {
        return courtApplicationPartyId;
    }

    public CourtApplicationPartyType getCourtApplicationPartyType() {
        return courtApplicationPartyType;
    }

    public UUID getProsecutionAuthorityId() {
        return prosecutionAuthorityId;
    }

    public PostalAddress getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return "PostalAddressee{" +
                "courtApplicationPartyId=" + courtApplicationPartyId +
                ", name='" + name + '\'' +
                ", address=" + address +
                ", courtApplicationPartyType='" + courtApplicationPartyType + '\'' +
                ", prosecutionAuthorityId='" + prosecutionAuthorityId + '\'' +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String name;

        private PostalAddress address;

        private UUID courtApplicationPartyId;

        private CourtApplicationPartyType courtApplicationPartyType;

        private UUID prosecutionAuthorityId;

        public Builder withName(final String name) {
            this.name = name;
            return this;
        }

        public Builder withAddress(final PostalAddress address) {
            this.address = address;
            return this;
        }

        public Builder withCourtApplicationPartyId(final UUID courtApplicationPartyId) {
            this.courtApplicationPartyId = courtApplicationPartyId;
            return this;
        }

        public Builder withProsecutionAuthorityId(final UUID prosecutionAuthorityId) {
            this.prosecutionAuthorityId = prosecutionAuthorityId;
            return this;
        }

        public Builder withCourtApplicationPartyType(final CourtApplicationPartyType courtApplicationPartyType) {
            this.courtApplicationPartyType = courtApplicationPartyType;
            return this;
        }

        public PostalAddressee build() {
            return new PostalAddressee(courtApplicationPartyId, name, address, courtApplicationPartyType, prosecutionAuthorityId);
        }
    }
}
