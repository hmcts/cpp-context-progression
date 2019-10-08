package uk.gov.moj.cpp.progression.it;

import static uk.gov.moj.cpp.progression.helper.DefenceAssociationHelper.associateOrganisation;
import static uk.gov.moj.cpp.progression.helper.DefenceAssociationHelper.disassociateOrganisation;
import static uk.gov.moj.cpp.progression.helper.DefenceAssociationHelper.verifyDefenceOrganisationAssociatedDataPersisted;
import static uk.gov.moj.cpp.progression.helper.DefenceAssociationHelper.verifyDefenceOrganisationDisassociatedDataPersisted;
import static uk.gov.moj.cpp.progression.helper.StubUtil.resetStubs;
import static uk.gov.moj.cpp.progression.stub.AuthorisationServiceStub.stubEnableAllCapabilities;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationQuery;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetUsersAndGroupsQuery;

import uk.gov.moj.cpp.progression.helper.DefenceAssociationHelper;

import java.io.IOException;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

public class DefenceDisassociationIT extends BaseIntegrationTest {

    @Before
    public void setUp() throws IOException {
        resetStubs();
    }

    @Test
    public void shouldPerformDisassociation() throws Exception {

        //Given
        final String userId = UUID.randomUUID().toString();
        final String defendantId = UUID.randomUUID().toString();
        final String organisationId = UUID.randomUUID().toString();
        final String organisationName = "Smith Associates Ltd.";

        stubGetUsersAndGroupsQuery(userId);
        stubEnableAllCapabilities();
        stubGetOrganisationQuery(userId, organisationId, organisationName);

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


}
