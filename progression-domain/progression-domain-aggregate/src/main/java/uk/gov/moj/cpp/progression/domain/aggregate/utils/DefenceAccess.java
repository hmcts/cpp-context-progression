package uk.gov.moj.cpp.progression.domain.aggregate.utils;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings({"squid:S1067"})
public class DefenceAccess implements Serializable {

    private Boolean isAgent;
    private UUID granteeUserId;
    private UUID granteeOrganisationId;
    private String granteeOrganisationType;
    private UUID grantorUserId;
    private UUID grantorOrganisationId;

    public Boolean getAgent() {
        return isAgent;
    }

    public void setAgent(final Boolean agent) {
        isAgent = agent;
    }

    public UUID getGranteeUserId() {
        return granteeUserId;
    }

    public void setGranteeUserId(final UUID granteeUserId) {
        this.granteeUserId = granteeUserId;
    }

    public UUID getGranteeOrganisationId() {
        return granteeOrganisationId;
    }

    public void setGranteeOrganisationId(final UUID granteeOrganisationId) {
        this.granteeOrganisationId = granteeOrganisationId;
    }

    public String getGranteeOrganisationType() {
        return granteeOrganisationType;
    }

    public void setGranteeOrganisationType(final String granteeOrganisationType) {
        this.granteeOrganisationType = granteeOrganisationType;
    }

    public UUID getGrantorUserId() {
        return grantorUserId;
    }

    public void setGrantorUserId(final UUID grantorUserId) {
        this.grantorUserId = grantorUserId;
    }

    public UUID getGrantorOrganisationId() {
        return grantorOrganisationId;
    }

    public void setGrantorOrganisationId(final UUID grantorOrganisationId) {
        this.grantorOrganisationId = grantorOrganisationId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DefenceAccess that = (DefenceAccess) o;
        return Objects.equals(isAgent, that.isAgent) &&
                Objects.equals(granteeUserId, that.granteeUserId) &&
                Objects.equals(granteeOrganisationId, that.granteeOrganisationId) &&
                Objects.equals(grantorUserId, that.grantorUserId) &&
                Objects.equals(grantorOrganisationId, that.grantorOrganisationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isAgent, granteeUserId, granteeOrganisationId,
                grantorUserId, grantorOrganisationId);
    }
}
