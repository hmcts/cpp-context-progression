package uk.gov.moj.cpp.progression.it;

import org.junit.Before;
import org.junit.Test;
import uk.gov.moj.cpp.progression.helper.AddDefendantHelper;

import java.io.IOException;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;

public class AddDefendantIT extends BaseIntegrationTest {
    @Before
    public void setUp() throws IOException {
        createMockEndpoints();
    }

    @Test
    public void addMinimalPayloadForDefendantAndVerify() {
        try (AddDefendantHelper addDefendantHelper = new AddDefendantHelper(randomUUID().toString())) {
            addDefendantHelper.addMinimalDefendant();
            addDefendantHelper.verifyInActiveMQ();
            addDefendantHelper.verifyInPublicTopic();
            addDefendantHelper.verifyMinimalDefendantAdded();
            addDefendantHelper.verifySearchForCaseByURN();
        }
    }

    @Test
    public void addDefendantTwiceAndVerifyFailedEventGenerated() {
        try (AddDefendantHelper addDefendantHelper = new AddDefendantHelper(randomUUID().toString())) {
            addDefendantHelper.addFullDefendant();
            addDefendantHelper.verifyInActiveMQ();
            addDefendantHelper.verifyInPublicTopic();
            addDefendantHelper.addFullDefendant();
            addDefendantHelper.verifyFailureMessageInPrivateTopic();
            addDefendantHelper.verifyFailureMessageInPublicTopic();
        }
    }

    @Test
    public void addDefendantsWithSamePoliceIdAndVerifyFailedEventGenerated() {
        UUID CASE_ID = randomUUID();
        try (AddDefendantHelper addDefendantHelper = new AddDefendantHelper(CASE_ID.toString())) {
            addDefendantHelper.addFullDefendant();
            addDefendantHelper.verifyInActiveMQ();
            addDefendantHelper.verifyInPublicTopic();

            // adding the same police defendant again should trigger the failure scenario
            String VALUE_DEFENDANT_ID = randomUUID().toString();
            addDefendantHelper.addFullDefendant(VALUE_DEFENDANT_ID);
            addDefendantHelper.verifyFailureMessageInPrivateTopic(VALUE_DEFENDANT_ID);
            addDefendantHelper.verifyFailureMessageInPublicTopic(VALUE_DEFENDANT_ID);
        }
    }

}