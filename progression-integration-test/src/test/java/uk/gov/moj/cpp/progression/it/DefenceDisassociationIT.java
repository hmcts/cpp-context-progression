package uk.gov.moj.cpp.progression.it;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.DefenceAssociationHelper.associateOrganisation;
import static uk.gov.moj.cpp.progression.helper.DefenceAssociationHelper.disassociateOrganisation;
import static uk.gov.moj.cpp.progression.helper.DefenceAssociationHelper.invokeDisassociateOrganisation;
import static uk.gov.moj.cpp.progression.helper.DefenceAssociationHelper.verifyDefenceOrganisationAssociatedDataPersisted;
import static uk.gov.moj.cpp.progression.helper.DefenceAssociationHelper.verifyDefenceOrganisationDisassociatedDataPersisted;
import static uk.gov.moj.cpp.progression.helper.StubUtil.resetStubs;
import static uk.gov.moj.cpp.progression.stub.AuthorisationServiceStub.stubEnableAllCapabilities;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationDetails;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationQuery;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetUsersAndGroupsQueryForDefenceUsers;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetUsersAndGroupsQueryForHMCTSUsers;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetUsersAndGroupsQueryForSystemUsers;

import uk.gov.moj.cpp.progression.helper.DefenceAssociationHelper;

import java.io.IOException;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

public class DefenceDisassociationIT extends BaseIntegrationTest {

    @Before
    public void setUp() throws IOException {
        resetStubs();
    }

    @Test
    public void shouldPerformDisassociationForADefenceUser() throws Exception {

        //Given
        final String userId = UUID.randomUUID().toString();
        final String defendantId = UUID.randomUUID().toString();
        final String organisationId = UUID.randomUUID().toString();
        final String organisationName = "Smith Associates Ltd.";

        stubGetUsersAndGroupsQueryForDefenceUsers(userId);
        stubEnableAllCapabilities();
        stubGetOrganisationQuery(userId, organisationId, organisationName);
        stubGetOrganisationDetails(organisationId, organisationName);

        try (final DefenceAssociationHelper helper = new DefenceAssociationHelper()) {
            associateOrganisation(defendantId, userId);
            helper.verifyDefenceOrganisationAssociatedEventGenerated(defendantId, organisationId);
            verifyDefenceOrganisationAssociatedDataPersisted(defendantId,
                    organisationId,
                    userId);

            //When
            disassociateOrganisation(defendantId, userId, organisationId);

            //Then
            helper.verifyDefenceOrganisationDisassociatedEventGenerated(defendantId, organisationId);
            verifyDefenceOrganisationDisassociatedDataPersisted(defendantId, organisationId, userId);
        }


    }

    @Test
    public void shouldPerformDisassociationForHMCTSUser() throws Exception {

        //Given
        final String userId = UUID.randomUUID().toString();
        final String defendantId = UUID.randomUUID().toString();
        final String organisationId = UUID.randomUUID().toString();
        final String organisationName = "Smith Associates Ltd.";

        stubGetUsersAndGroupsQueryForDefenceUsers(userId);
        stubEnableAllCapabilities();
        stubGetOrganisationQuery(userId, organisationId, organisationName);
        stubGetOrganisationDetails(organisationId, organisationName);

        try (final DefenceAssociationHelper helper = new DefenceAssociationHelper()) {

            associateOrganisation(defendantId, userId);
            helper.verifyDefenceOrganisationAssociatedEventGenerated(defendantId, organisationId);
            verifyDefenceOrganisationAssociatedDataPersisted(defendantId,
                    organisationId,
                    userId);

            final String hmctsUserId = UUID.randomUUID().toString();
            stubGetUsersAndGroupsQueryForHMCTSUsers(hmctsUserId);
            stubGetOrganisationQuery(hmctsUserId, organisationId, organisationName);

            //When
            disassociateOrganisation(defendantId, hmctsUserId, organisationId);

            //Then
            helper.verifyDefenceOrganisationDisassociatedEventGenerated(defendantId, organisationId);
            verifyDefenceOrganisationDisassociatedDataPersisted(defendantId, organisationId, userId);
        }
    }

    @Test
    public void shouldNotPerformDisassociationForASystemUser() throws Exception {

        //Given
        final String userId = UUID.randomUUID().toString();
        final String defendantId = UUID.randomUUID().toString();
        final String organisationId = UUID.randomUUID().toString();
        final String organisationName = "Smith Associates Ltd.";

        stubGetUsersAndGroupsQueryForDefenceUsers(userId);
        stubEnableAllCapabilities();
        stubGetOrganisationQuery(userId, organisationId, organisationName);
        stubGetOrganisationDetails(organisationId, organisationName);
        associateOrganisation(defendantId, userId);
        verifyDefenceOrganisationAssociatedDataPersisted(defendantId,
                organisationId,
                userId);

        final String systemUserId = UUID.randomUUID().toString();
        stubGetUsersAndGroupsQueryForSystemUsers(systemUserId);
        stubGetOrganisationQuery(systemUserId, organisationId, organisationName);

        //When
        final Response response = invokeDisassociateOrganisation(defendantId, systemUserId, organisationId);

        //Then - The Actual Association will check for a HTTP Forbidden status and stop processing smoothly
        assertThat(response.getStatus(), equalTo(HttpStatus.SC_FORBIDDEN));
    }
}
