package uk.gov.moj.cpp.progression.it;

import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;

import uk.gov.moj.cpp.progression.helper.DefenceAssociationHelper;

import java.io.IOException;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

public class DefenceAssociationIT extends BaseIntegrationTest {

    private DefenceAssociationHelper defenceAssociationHelper;

    @Before
    public void setUp() throws IOException {
        createMockEndpoints();
        defenceAssociationHelper = new DefenceAssociationHelper();
    }

    @Test
    public void shouldPerformAssociation() throws Exception {

        final String defendantId = UUID.randomUUID().toString();
        final String organisationId = UUID.randomUUID().toString();

        defenceAssociationHelper.initiateDefenceAssociationForDefendant(defendantId, organisationId);
        defenceAssociationHelper.verifyPublicEventRaisedForDefenceAssociationForDefendant(defendantId, organisationId);

    }


}
