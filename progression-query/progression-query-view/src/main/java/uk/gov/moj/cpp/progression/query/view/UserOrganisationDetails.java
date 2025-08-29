package uk.gov.moj.cpp.progression.query.view;

import java.util.UUID;

public class UserOrganisationDetails {

    private UUID organisationId;
    private  String organisationName;

    public UserOrganisationDetails() {

    }

    public UserOrganisationDetails(final UUID organisationId, final String organisationName) {
        this.organisationId = organisationId;
        this.organisationName = organisationName;
    }

    public UUID getOrganisationId() {
        return organisationId;
    }

    public String getOrganisationName() {
        return organisationName;
    }
}
