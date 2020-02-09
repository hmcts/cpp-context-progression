package uk.gov.moj.cpp.progression.it;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.DefenceAssociationHelper.associateOrganisation;
import static uk.gov.moj.cpp.progression.helper.DefenceAssociationHelper.invokeDisassociateOrganisation;
import static uk.gov.moj.cpp.progression.helper.DefenceAssociationHelper.verifyDefenceOrganisationAssociatedDataPersisted;
import static uk.gov.moj.cpp.progression.helper.DefenceAssociationHelper.verifyDefenceOrganisationDisassociatedDataPersisted;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetGroupsForLoggedInQuery;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationDetails;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationQuery;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetUsersAndGroupsQueryForDefenceUsers;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetUsersAndGroupsQueryForHMCTSUsers;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetUsersAndGroupsQueryForSystemUsers;

import uk.gov.moj.cpp.progression.AbstractIT;
import uk.gov.moj.cpp.progression.helper.DefenceAssociationHelper;

import javax.ws.rs.core.Response;

import java.util.UUID;

import org.apache.http.HttpStatus;
import org.junit.Test;

public class DefenceDisassociationIT extends AbstractIT {

    @Test
    public void shouldPerformDisassociationForADefenceUser() throws Exception {
        //Given
        final String userId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String organisationId = randomUUID().toString();
		final String caseId = randomUUID().toString();
        final String organisationName = "Smith Associates Ltd.";

        stubGetUsersAndGroupsQueryForDefenceUsers(userId);
        stubGetOrganisationQuery(userId, organisationId, organisationName);
        stubGetOrganisationDetails(organisationId, organisationName);
        stubGetGroupsForLoggedInQuery(userId);


        try (final DefenceAssociationHelper helper = new DefenceAssociationHelper()) {
            associateOrganisation(defendantId, userId);
            helper.verifyDefenceOrganisationAssociatedEventGenerated(defendantId, organisationId);
            verifyDefenceOrganisationAssociatedDataPersisted(defendantId,
                    organisationId,
                    userId);

            //When
            final Response response = invokeDisassociateOrganisation(defendantId, userId, organisationId, caseId);
            assertThat(response.getStatus(), equalTo(HttpStatus.SC_ACCEPTED));
            //Then
            helper.verifyDefenceOrganisationDisassociatedEventGenerated(defendantId, organisationId);
            verifyDefenceOrganisationDisassociatedDataPersisted(defendantId, organisationId, userId);
        }
    }

    @Test
    public void shouldPerformDisassociationForHMCTSUser() throws Exception {

        //Given
        final String userId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String organisationId = randomUUID().toString();
		final String caseId = randomUUID().toString();
        final String organisationName = "Smith Associates Ltd.";

        stubGetUsersAndGroupsQueryForDefenceUsers(userId);
        stubGetOrganisationQuery(userId, organisationId, organisationName);
        stubGetOrganisationDetails(organisationId, organisationName);
        stubGetGroupsForLoggedInQuery(userId);

        try (final DefenceAssociationHelper helper = new DefenceAssociationHelper()) {

            associateOrganisation(defendantId, userId);
            helper.verifyDefenceOrganisationAssociatedEventGenerated(defendantId, organisationId);
            verifyDefenceOrganisationAssociatedDataPersisted(defendantId,
                    organisationId,
                    userId);

            final String hmctsUserId = randomUUID().toString();
            stubGetUsersAndGroupsQueryForHMCTSUsers(hmctsUserId);
            stubGetOrganisationQuery(hmctsUserId, organisationId, organisationName);

            //When
            final Response response = invokeDisassociateOrganisation(defendantId, hmctsUserId, organisationId, caseId);
            assertThat(response.getStatus(), equalTo(HttpStatus.SC_ACCEPTED));
            //Then
            helper.verifyDefenceOrganisationDisassociatedEventGenerated(defendantId, organisationId);
            verifyDefenceOrganisationDisassociatedDataPersisted(defendantId, organisationId, userId);
        }
    }

    @Test
    public void shouldPerformDisassociationForASystemUser() throws Exception {

        //Given
        final String userId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String organisationId = randomUUID().toString();
		final String caseId = randomUUID().toString();
        final String organisationName = "Smith Associates Ltd.";

        stubGetUsersAndGroupsQueryForDefenceUsers(userId);
        stubGetOrganisationQuery(userId, organisationId, organisationName);
        stubGetOrganisationDetails(organisationId, organisationName);
        associateOrganisation(defendantId, userId);
        stubGetGroupsForLoggedInQuery(userId);
        verifyDefenceOrganisationAssociatedDataPersisted(defendantId,
                organisationId,
                userId);

        final String systemUserId = randomUUID().toString();
        stubGetUsersAndGroupsQueryForSystemUsers(systemUserId);
        stubGetOrganisationQuery(systemUserId, organisationId, organisationName);

        //When
        final Response response = invokeDisassociateOrganisation(defendantId, systemUserId, organisationId, caseId);
        //Then - The Actual Association will check for a HTTP Forbidden status and stop processing smoothly
        assertThat(response.getStatus(), equalTo(HttpStatus.SC_ACCEPTED));
    }
}
