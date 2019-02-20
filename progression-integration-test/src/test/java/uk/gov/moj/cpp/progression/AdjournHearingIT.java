package uk.gov.moj.cpp.progression;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getProsecutioncasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostSendCaseForListing;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.assertProsecutionCase;

import org.junit.Before;
import org.junit.Test;
import uk.gov.moj.cpp.progression.util.AdjournHearingHelper;
import uk.gov.moj.cpp.progression.util.ConvictionDateHelper;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.io.IOException;
import java.util.UUID;


public class AdjournHearingIT {

    AdjournHearingHelper helper;
    private String caseId;
    private String defendantId;
    private String offenceId;

    @Before
    public void setUp() throws IOException {
        caseId = UUID.randomUUID().toString();
        defendantId = UUID.randomUUID().toString();
        offenceId = UUID.fromString("3789ab16-0bb7-4ef1-87ef-c936bf0364f1").toString();
        helper = new AdjournHearingHelper(caseId, defendantId, offenceId);
        createMockEndpoints();
    }

    @Test
    public void shouldSendCaseForListing() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        String response = getProsecutioncasesProgressionFor(caseId);
        JsonObject prosecutioncasesJsonObject = getJsonObject(response);
        assertProsecutionCase(prosecutioncasesJsonObject.getJsonObject("prosecutionCase"), caseId, defendantId);
        assertThat(prosecutioncasesJsonObject.getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(0).getJsonObject("personDefendant").getJsonObject("personDetails").getString("firstName"), equalTo("Harry"));

        // when
        helper.adjournHearing();
        verifyPostSendCaseForListing(caseId, defendantId, offenceId);

    }

}

