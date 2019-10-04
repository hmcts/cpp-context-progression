package uk.gov.moj.cpp.progression.it;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.DefenceAssociationHelper.associateOrganisation;
import static uk.gov.moj.cpp.progression.helper.DefenceAssociationHelper.invokeAssociateOrganisation;
import static uk.gov.moj.cpp.progression.helper.DefenceAssociationHelper.verifyDefenceOrganisationAssociatedDataPersisted;
import static uk.gov.moj.cpp.progression.helper.StubUtil.resetStubs;
import static uk.gov.moj.cpp.progression.stub.AuthorisationServiceStub.stubEnableAllCapabilities;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationQuery;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetUsersAndGroupsQueryForDefenceUsers;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetUsersAndGroupsQueryForHMCTSUsers;

import uk.gov.moj.cpp.progression.helper.DefenceAssociationHelper;

import java.io.IOException;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

public class DefenceAssociationIT extends BaseIntegrationTest {

    @Before
    public void setUp() throws IOException {
        resetStubs();
    }

    @Test
    public void shouldPerformAssociation() throws Exception {

        //Given
        final String userId = UUID.randomUUID().toString();
        final String defendantId = UUID.randomUUID().toString();
        final String organisationId = UUID.randomUUID().toString();
        final String organisationName = "Smith Associates Ltd.";

        stubGetUsersAndGroupsQueryForDefenceUsers(userId);
        stubEnableAllCapabilities();
        stubGetOrganisationQuery(userId, organisationId, organisationName);

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
    public void shouldNotPerformAssociation() throws Exception {

        //Given
        final String userId = UUID.randomUUID().toString();
        final String defendantId = UUID.randomUUID().toString();
        final String organisationId = UUID.randomUUID().toString();
        final String organisationName = "Smith Associates Ltd.";

        stubGetUsersAndGroupsQueryForHMCTSUsers(userId);
        stubEnableAllCapabilities();
        stubGetOrganisationQuery(userId, organisationId, organisationName);

        //When
        final Response response = invokeAssociateOrganisation(defendantId, userId);

        //Then - The Actual Association will check for a HTTP Forbidden status and stop processing smoothly
        assertThat(response.getStatus(), equalTo(HttpStatus.SC_FORBIDDEN));
    }

}
