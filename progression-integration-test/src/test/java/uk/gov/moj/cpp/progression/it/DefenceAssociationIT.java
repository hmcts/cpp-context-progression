package uk.gov.moj.cpp.progression.it;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.DefenceAssociationHelper.associateOrganisation;
import static uk.gov.moj.cpp.progression.helper.DefenceAssociationHelper.invokeAssociateOrganisation;
import static uk.gov.moj.cpp.progression.helper.DefenceAssociationHelper.verifyDefenceOrganisationAssociatedDataPersisted;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationDetails;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationQuery;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetUsersAndGroupsQueryForDefenceUsers;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetUsersAndGroupsQueryForHMCTSUsers;

import uk.gov.moj.cpp.progression.AbstractIT;
import uk.gov.moj.cpp.progression.helper.DefenceAssociationHelper;

import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.junit.Test;

public class DefenceAssociationIT extends AbstractIT {

    @Test
    public void shouldPerformAssociation() throws Exception {

        //Given
        final String userId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String organisationId = randomUUID().toString();
        final String organisationName = "Smith Associates Ltd.";

        stubGetUsersAndGroupsQueryForDefenceUsers(userId);
        stubGetOrganisationQuery(userId, organisationId, organisationName);
        stubGetOrganisationDetails(organisationId, organisationName);

        try (final DefenceAssociationHelper helper = new DefenceAssociationHelper()) {
            //When
            associateOrganisation(defendantId, userId);

            //Then
            helper.verifyDefenceOrganisationAssociatedEventGenerated(defendantId, organisationId);
        }
        verifyDefenceOrganisationAssociatedDataPersisted(defendantId,
                organisationId,
                userId);
    }

    @Test
    public void shouldReturnedAssociationDetailsWhenQueriedByHmctsUser() throws Exception {

        //Given
        final String userId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String organisationId = randomUUID().toString();
        final String organisationName = "Smith Associates Ltd.";
        final String hmctsUserId = randomUUID().toString();

        stubGetUsersAndGroupsQueryForDefenceUsers(userId);
        stubGetOrganisationQuery(userId, organisationId, organisationName);
        stubGetOrganisationDetails(organisationId, organisationName);
        stubGetUsersAndGroupsQueryForHMCTSUsers(hmctsUserId);
        stubGetOrganisationQuery(hmctsUserId, organisationId, organisationName);

        try (final DefenceAssociationHelper helper = new DefenceAssociationHelper()) {
            //When
            associateOrganisation(defendantId, userId);

            //Then
            helper.verifyDefenceOrganisationAssociatedEventGenerated(defendantId, organisationId);
        }

        //Then
        //Making sure that the Association can be Queried using a HMCTS User.....
        verifyDefenceOrganisationAssociatedDataPersisted(defendantId,
                organisationId,
                hmctsUserId);
    }

    @Test
    public void shouldNotPerformAssociation() throws Exception {

        //Given
        final String userId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String organisationId = randomUUID().toString();
        final String organisationName = "Smith Associates Ltd.";

        stubGetUsersAndGroupsQueryForHMCTSUsers(userId);
        stubGetOrganisationQuery(userId, organisationId, organisationName);
        stubGetOrganisationDetails(organisationId, organisationName);

        //When
        final Response response = invokeAssociateOrganisation(defendantId, userId);

        //Then - The Actual Association will check for a HTTP Forbidden status and stop processing smoothly
        assertThat(response.getStatus(), equalTo(HttpStatus.SC_FORBIDDEN));
    }

}
