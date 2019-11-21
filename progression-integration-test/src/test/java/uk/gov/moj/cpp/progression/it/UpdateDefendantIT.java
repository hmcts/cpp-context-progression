package uk.gov.moj.cpp.progression.it;

import static uk.gov.moj.cpp.progression.helper.StubUtil.setupUsersGroupQueryStub;
import static uk.gov.moj.cpp.progression.util.WiremockTestHelper.waitForStubToBeReady;

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
        setupUsersGroupQueryStub();
        waitForUsersAndGroupsStubToBeReady();
        addDefendantHelper.addMinimalDefendant();
        addDefendantHelper.verifyInActiveMQ();
        addDefendantHelper.verifyInPublicTopic();
        addDefendantHelper.verifyMinimalDefendantAdded();
    }

    private void waitForUsersAndGroupsStubToBeReady() {
        waitForStubToBeReady("/usersgroups-service/query/api/rest/usersgroups/users/.*", "application/json");
    }

    /**
     * Adds  and update a Defendant person info and then verifies by reading a case to determine
     * that the defendant has been updated.
     */
    @Test
    public void updateDefendantPersonAndVerify() {
        final UpdateDefendantHelper updateDefendantHelper = new UpdateDefendantHelper(caseId, addDefendantHelper.getDefendantId(), addDefendantHelper.getPersonId());
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
        final UpdateDefendantHelper updateDefendantHelper = new UpdateDefendantHelper(caseId, addDefendantHelper.getDefendantId(), addDefendantHelper.getPersonId());
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
        final UpdateDefendantHelper updateDefendantHelper = new UpdateDefendantHelper(caseId, addDefendantHelper.getDefendantId(), addDefendantHelper.getPersonId());
        updateDefendantHelper.verifyEmptyUpdateDefendantPayload();
    }

}