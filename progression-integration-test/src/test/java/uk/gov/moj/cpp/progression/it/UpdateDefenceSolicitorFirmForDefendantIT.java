package uk.gov.moj.cpp.progression.it;

import org.junit.Before;
import org.junit.Test;
import uk.gov.moj.cpp.progression.helper.AddDefendantHelper;
import uk.gov.moj.cpp.progression.helper.UpdateDefenceSolicitorFirmForDefendantHelper;

import java.util.UUID;

public class UpdateDefenceSolicitorFirmForDefendantIT extends BaseIntegrationTest {



    @Test
    public void addDefendantAndUpdateDefenceSolicitorFirm() {
        String  caseId = UUID.randomUUID().toString();

        // Add defendant
        try (AddDefendantHelper addDefendantHelper = new AddDefendantHelper(caseId)) {
            addDefendantHelper.addMinimalDefendant();
            addDefendantHelper.verifyInActiveMQ();
            addDefendantHelper.verifyMinimalDefendantAdded();
            String defendantID = addDefendantHelper.getDefendantId();

            // update defence solicitor firm
            UpdateDefenceSolicitorFirmForDefendantHelper updateDefenceSolicitorFirmForDefendantHelper =
                    new UpdateDefenceSolicitorFirmForDefendantHelper(caseId, defendantID);
            updateDefenceSolicitorFirmForDefendantHelper.updateDefenceSolicitorFirm();
            updateDefenceSolicitorFirmForDefendantHelper.verifyInActiveMQ();
            updateDefenceSolicitorFirmForDefendantHelper.verifyDefenceSolicitorFirmUpdated();

        }
    }
}
