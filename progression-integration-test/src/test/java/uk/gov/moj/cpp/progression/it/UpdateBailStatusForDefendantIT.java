package uk.gov.moj.cpp.progression.it;

import org.junit.Before;
import org.junit.Test;
import uk.gov.moj.cpp.progression.helper.AddDefendantHelper;
import uk.gov.moj.cpp.progression.helper.UpdateBailStatusForDefendantHelper;

import java.util.UUID;

import static java.util.UUID.randomUUID;

public class UpdateBailStatusForDefendantIT extends BaseIntegrationTest {

    private String caseId;

    @Before
    public void setUp(){
        this.caseId=UUID.randomUUID().toString();
    }

    @Test
    public void addDefendantAndUpdateBailStatus() {

        // Add defendant
        try (AddDefendantHelper addDefendantHelper1 = new AddDefendantHelper(caseId)) {
            addDefendantHelper1.addMinimalDefendant();
            addDefendantHelper1.verifyInActiveMQ();
            addDefendantHelper1.verifyMinimalDefendantAdded();
            String defendantID = addDefendantHelper1.getDefendantId();

            // update bail status
            UpdateBailStatusForDefendantHelper updateBailStatusForDefendantHelper =
                    new UpdateBailStatusForDefendantHelper(caseId, defendantID);
            updateBailStatusForDefendantHelper.updateBailStatus();
                updateBailStatusForDefendantHelper.verifySearchByMaterialId();
            updateBailStatusForDefendantHelper.verifyInActiveMQ();
            updateBailStatusForDefendantHelper.verifyBailStatusUpdated();
        }

    }
}
