package uk.gov.moj.cpp.progression.listing.domain;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import java.io.Serializable;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings({"squid:S00107", "squid:S00121", "squid:S1948", "squid:S1067"})
public class JudicialRole implements Serializable {
    private static final long serialVersionUID = 1L;


    private final Boolean isBenchChairman;

    private final Boolean isDeputy;

    private final UUID judicialId;

    private final JudicialRoleType judicialRoleType;


    private final UUID userId;

    public JudicialRole(final Optional<Boolean> isBenchChairman, final Optional<Boolean> isDeputy, final UUID judicialId, final UUID userId, final JudicialRoleType judicialRoleType) {
        this.isBenchChairman = isBenchChairman.orElse(null);
        this.isDeputy = isDeputy.orElse(null);
        this.judicialId = judicialId;
        this.judicialRoleType = judicialRoleType;
        this.userId = userId;
    }

    public Optional<Boolean> getIsBenchChairman() {
        return isBenchChairman != null ? of(isBenchChairman) : empty();
    }

    public Optional<Boolean> getIsDeputy() {
        return isDeputy != null ? of(isDeputy) : empty();
    }

    public UUID getJudicialId() {
        return judicialId;
    }

    public JudicialRoleType getJudicialRoleType() {
        return judicialRoleType;
    }

    public static Builder judicialRole() {
        return new JudicialRole.Builder();
    }

    public UUID getUserId() {
        return userId;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final JudicialRole that = (JudicialRole) obj;

        return java.util.Objects.equals(this.isBenchChairman, that.isBenchChairman) &&
                java.util.Objects.equals(this.isDeputy, that.isDeputy) &&
                java.util.Objects.equals(this.judicialId, that.judicialId) &&
                java.util.Objects.equals(this.judicialRoleType, that.judicialRoleType) &&
                java.util.Objects.equals(this.userId, that.userId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(isBenchChairman, isDeputy, judicialId, judicialRoleType, userId);

    }

    @Override
    public String toString() {
        return "JudicialRole{" +
                "isBenchChairman='" + isBenchChairman + "'," +
                "isDeputy='" + isDeputy + "'," +
                "judicialId='" + judicialId + "'," +
                "judicialRoleType='" + judicialRoleType + "'" +
                "}";
    }

    public static class Builder {
        private Optional<Boolean> isBenchChairman;

        private Optional<Boolean> isDeputy;

        private UUID judicialId;

        private UUID userId;

        private JudicialRoleType judicialRoleType;

        public Builder withIsBenchChairman(final Optional<Boolean> isBenchChairman) {
            this.isBenchChairman = isBenchChairman;
            return this;
        }

        public Builder withIsDeputy(final Optional<Boolean> isDeputy) {
            this.isDeputy = isDeputy;
            return this;
        }

        public Builder withJudicialId(final UUID judicialId) {
            this.judicialId = judicialId;
            return this;
        }

        public Builder withUserId(final UUID userId) {
            this.userId = userId;
            return this;
        }

        public Builder withJudicialRoleType(final JudicialRoleType judicialRoleType) {
            this.judicialRoleType = judicialRoleType;
            return this;
        }

        public JudicialRole build() {
            return new JudicialRole(isBenchChairman, isDeputy, judicialId, userId, judicialRoleType);
        }
    }
}
