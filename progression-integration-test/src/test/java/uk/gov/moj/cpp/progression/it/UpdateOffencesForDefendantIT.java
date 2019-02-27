package uk.gov.moj.cpp.progression.it;

import uk.gov.moj.cpp.progression.helper.AddDefendantHelper;
import uk.gov.moj.cpp.progression.helper.UpdateOffencesForDefendantHelper;
import uk.gov.moj.cpp.progression.stub.ReferenceDataStub;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

public class UpdateOffencesForDefendantIT extends BaseIntegrationTest {

    private static final String REF_DATA_QUERY_CJSCODE_PAYLOAD =
            "/restResource/ref-data-cjscode.json";

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
        ReferenceDataStub.stubQueryOffences(REF_DATA_QUERY_CJSCODE_PAYLOAD);
    }

    /**
     * Adds a Defendant to a case and then verifies by reading a case to determine that
     * the defendant has been added.
     */
    @Test
    public void updateOffencesForDefendantAndVerify() {
        final UpdateOffencesForDefendantHelper updateOffenceForDefendantHelper = new UpdateOffencesForDefendantHelper(caseId, addDefendantHelper.getDefendantId());
        updateOffenceForDefendantHelper.updateOffencesForDefendant();
        updateOffenceForDefendantHelper.verifyInActiveMQ();
        updateOffenceForDefendantHelper.verifyInMessagingQueueOffencesForDefendentUpdated();
        updateOffenceForDefendantHelper.verifyOffencesForDefendantUpdated();
        updateOffenceForDefendantHelper.verifyOffencesPleasForDefendantUpdated();

    }

    @Test
    public void updateOffencesForDefendantWithOrderAndVerify() {
        final UpdateOffencesForDefendantHelper updateOffenceForDefendantHelper = new UpdateOffencesForDefendantHelper(caseId, addDefendantHelper.getDefendantId());
        updateOffenceForDefendantHelper.updateMultipleOffencesForDefendant();
        updateOffenceForDefendantHelper.verifyInActiveMQ();
        updateOffenceForDefendantHelper.verifyInMessagingQueueOffencesForDefendentUpdated();
        updateOffenceForDefendantHelper.verifyOffencesForDefendantUpdatedWithOffenceOrdering(addDefendantHelper.getCaseUrn());

    }

    @Test
    public void updateOffenceForDefendantAndVerifyPublicEvent() {
        final UpdateOffencesForDefendantHelper updateOffenceForDefendantHelper = new UpdateOffencesForDefendantHelper(caseId, addDefendantHelper.getDefendantId());
        updateOffenceForDefendantHelper.updateOffencesForDefendant(addDefendantHelper.getOffenceId());
        updateOffenceForDefendantHelper.verifyInActiveMQ();
        updateOffenceForDefendantHelper.verifyInMessagingQueueOffencesForDefendentUpdated();
        updateOffenceForDefendantHelper.verifyOffencesForDefendantUpdated();
        updateOffenceForDefendantHelper.verifyOffencesPleasForDefendantUpdated();
    }

    @Test
    public void updateOffenceForDefendantAndVerifyPublicEventNotRaised() {
        final UpdateOffencesForDefendantHelper updateOffenceForDefendantHelper = new UpdateOffencesForDefendantHelper(caseId, addDefendantHelper.getDefendantId());
        updateOffenceForDefendantHelper.updateOffencesForDefendant(addDefendantHelper.getOffenceId());
        updateOffenceForDefendantHelper.verifyInActiveMQ();
        updateOffenceForDefendantHelper.verifyInMessagingQueueOffencesForDefendentUpdated();
        updateOffenceForDefendantHelper.verifyOffencesForDefendantUpdated();
        updateOffenceForDefendantHelper.verifyOffencesPleasForDefendantUpdated();

        updateOffenceForDefendantHelper.updateOffencesForDefendant(addDefendantHelper.getOffenceId());

    }

}