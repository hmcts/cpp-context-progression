package uk.gov.moj.cpp.progression.it;

import uk.gov.moj.cpp.progression.helper.AddDefendantHelper;
import uk.gov.moj.cpp.progression.helper.UpdateDefendantHelper;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

public class UpdateDefendantIT extends BaseIntegrationTest {
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
     * Adds  and update a Defendant person info and then verifies by reading a case to determine
     * that the defendant has been updated.
     */
    @Test
    public void updateDefendantPersonAndVerify() {
        UpdateDefendantHelper updateDefendantHelper = new UpdateDefendantHelper(caseId, addDefendantHelper.getDefendantId(), addDefendantHelper.getPersonId());
        updateDefendantHelper.updateDefendantPerson();
        updateDefendantHelper.verifyInActiveMQ();
        updateDefendantHelper.verifyDefendantPersonUpdated();
        updateDefendantHelper.verifyInMessagingQueueForDefendentUpdated();

    }


    /**
     * Adds  and update a Defendant Bail status and then verifies by reading a case to determine
     * that the defendant has been updated.
     */
    @Test
    public void updateDefendantBailStatusAndVerify() {
        UpdateDefendantHelper updateDefendantHelper = new UpdateDefendantHelper(caseId, addDefendantHelper.getDefendantId(), addDefendantHelper.getPersonId());
        updateDefendantHelper.updateDefendantBailStatus();
        updateDefendantHelper.verifyInActiveMQ();
        updateDefendantHelper.verifyDefendantBailStatusUpdated();
        updateDefendantHelper.verifyInMessagingQueueForDefendentUpdated();

    }

    /**
     * update a Defendant with no information should fail
     */
    @Test
    public void updateDefendantEmptyPayloadVerify() {
        UpdateDefendantHelper updateDefendantHelper = new UpdateDefendantHelper(caseId, addDefendantHelper.getDefendantId(), addDefendantHelper.getPersonId());
        updateDefendantHelper.verifyEmptyUpdateDefendantPayload();
    }

}