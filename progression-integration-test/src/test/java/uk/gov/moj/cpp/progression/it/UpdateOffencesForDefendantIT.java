package uk.gov.moj.cpp.progression.it;

import org.junit.Before;
import org.junit.Test;
import uk.gov.moj.cpp.progression.helper.AddDefendantHelper;
import uk.gov.moj.cpp.progression.helper.AuthorisationServiceStub;
import uk.gov.moj.cpp.progression.helper.UpdateOffencesForDefendantHelper;

import java.util.UUID;

public class UpdateOffencesForDefendantIT extends BaseIntegrationTest {

    private AddDefendantHelper addDefendantHelper;
    private String caseId;

    /**
     * Sets up the case in preparation for adding a defendant
     */
    @Before
    public void setUp() {
        caseId = UUID.randomUUID().toString();
        addDefendantHelper = new AddDefendantHelper(caseId);
        addDefendantHelper.addMinimalDefendant();
        addDefendantHelper.verifyInActiveMQ();
        addDefendantHelper.verifyInPublicTopic();
        addDefendantHelper.verifyMinimalDefendantAdded();
    }

    /**
     * Adds a Defendant to a case and then verifies by reading a case to determine that
     * the defendant has been added.
     */
    @Test
    public void updateOffencesForDefendantAndVerify() {
        UpdateOffencesForDefendantHelper updateOffenceForDefendantHelper = new UpdateOffencesForDefendantHelper(caseId, addDefendantHelper.getDefendantId());
        updateOffenceForDefendantHelper.updateOffencesForDefendant();
        updateOffenceForDefendantHelper.updateOffencesPlea();
        updateOffenceForDefendantHelper.verifyInActiveMQ();
        updateOffenceForDefendantHelper.verifyInMessagingQueueOffencesForDefendentUpdated();
        updateOffenceForDefendantHelper.verifyOffencesForDefendantUpdated();
        updateOffenceForDefendantHelper.verifyOffencesPleasForDefendantUpdated();

    }

}