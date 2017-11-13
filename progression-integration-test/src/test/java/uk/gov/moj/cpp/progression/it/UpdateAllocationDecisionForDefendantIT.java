package uk.gov.moj.cpp.progression.it;

import org.junit.Before;
import org.junit.Test;
import uk.gov.moj.cpp.progression.helper.AddDefendantHelper;
import uk.gov.moj.cpp.progression.helper.UpdateAllocationDecisionForDefendantHelper;
import uk.gov.moj.cpp.progression.helper.UpdateOffencesForDefendantHelper;

import java.util.UUID;

public class UpdateAllocationDecisionForDefendantIT extends BaseIntegrationTest {

    /**
     * Sets up the case in preparation for adding a defendant
     */

    private String caseId;

    @Before
    public void setUp(){
        this.caseId=UUID.randomUUID().toString();
    }

    @Test
    public void addDefendantAndUpdateAllocationDecision() {

        // Add defendant
        try (AddDefendantHelper addDefendantHelper = new AddDefendantHelper(caseId)) {
            addDefendantHelper.addMinimalDefendant();
            addDefendantHelper.verifyInActiveMQ();
            addDefendantHelper.verifyMinimalDefendantAdded();
            String defendantID = addDefendantHelper.getDefendantId();

            UpdateOffencesForDefendantHelper updateOffenceForDefendantHelper = new UpdateOffencesForDefendantHelper(caseId, addDefendantHelper.getDefendantId());
            updateOffenceForDefendantHelper.updateOffencesForDefendant();
            updateOffenceForDefendantHelper.verifyInActiveMQ();
            updateOffenceForDefendantHelper.verifyOffencesForDefendantUpdated();
            // update defence solicitor firm
            UpdateAllocationDecisionForDefendantHelper updateAllocationDecisionForDefendantHelper =
                    new UpdateAllocationDecisionForDefendantHelper(caseId, defendantID);
            updateAllocationDecisionForDefendantHelper.updateAllocationDecision();
            updateAllocationDecisionForDefendantHelper.verifyInActiveMQ();
            updateAllocationDecisionForDefendantHelper.verifyAllocationDecisionUpdated();

            updateOffenceForDefendantHelper.updateOffencesForDefendant("NA");
            updateOffenceForDefendantHelper.verifyInActiveMQ();
            updateOffenceForDefendantHelper.verifyOffencesForDefendantUpdated();
            updateAllocationDecisionForDefendantHelper.verifyInMessagingQueueAllocationDecisionRemoved();
            updateAllocationDecisionForDefendantHelper.verifyAllocationDecisionRemoved();

        }
    }

    @Test
    public void shouldNotaddDefendantAndUpdateAllocationDecision() {

        // Add defendant
        try (AddDefendantHelper addDefendantHelper = new AddDefendantHelper(caseId)) {
            addDefendantHelper.addMinimalDefendant();
            addDefendantHelper.verifyInActiveMQ();
            addDefendantHelper.verifyMinimalDefendantAdded();
            String defendantID = addDefendantHelper.getDefendantId();

            // update defence solicitor firm
            UpdateAllocationDecisionForDefendantHelper updateAllocationDecisionForDefendantHelper =
                    new UpdateAllocationDecisionForDefendantHelper(caseId, defendantID);
            updateAllocationDecisionForDefendantHelper.updateAllocationDecision();
            updateAllocationDecisionForDefendantHelper.verifyInMessagingQueueOffenceDoesNotHaveModeOfTrial();



        }
    }
}
