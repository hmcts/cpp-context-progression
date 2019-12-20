package uk.gov.moj.cpp.progression;

import static java.util.UUID.randomUUID;

import uk.gov.moj.cpp.progression.helper.AddDefendantHelper;

import java.util.UUID;

import org.junit.Test;

public class AddDefendantIT extends AbstractIT {

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
        final UUID CASE_ID = randomUUID();
        try (AddDefendantHelper addDefendantHelper = new AddDefendantHelper(CASE_ID.toString())) {
            addDefendantHelper.addFullDefendant();
            addDefendantHelper.verifyInActiveMQ();
            addDefendantHelper.verifyInPublicTopic();

            // adding the same police defendant again should trigger the failure scenario
            final String VALUE_DEFENDANT_ID = randomUUID().toString();
            addDefendantHelper.addFullDefendant(VALUE_DEFENDANT_ID);
            addDefendantHelper.verifyFailureMessageInPrivateTopic(VALUE_DEFENDANT_ID);
            addDefendantHelper.verifyFailureMessageInPublicTopic(VALUE_DEFENDANT_ID);
        }
    }

}