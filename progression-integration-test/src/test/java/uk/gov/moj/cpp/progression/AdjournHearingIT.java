package uk.gov.moj.cpp.progression;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getProsecutioncasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearing;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.assertProsecutionCase;

import uk.gov.moj.cpp.progression.util.AdjournHearingHelper;

import java.io.IOException;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;


public class AdjournHearingIT {

    AdjournHearingHelper helper;
    private String caseId;
    private String defendantId;
    private String offenceId;
    private String applicationId;

    @Before
    public void setUp() throws IOException {
        caseId = UUID.randomUUID().toString();
        defendantId = UUID.randomUUID().toString();
        offenceId = UUID.fromString("3789ab16-0bb7-4ef1-87ef-c936bf0364f1").toString();
        applicationId = UUID.randomUUID().toString();
        helper = new AdjournHearingHelper(caseId, defendantId, offenceId, applicationId);
        createMockEndpoints();
    }

    @Test
    public void shouldListCourtHearing() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        String response = getProsecutioncasesProgressionFor(caseId);
        JsonObject prosecutioncasesJsonObject = getJsonObject(response);
        assertProsecutionCase(prosecutioncasesJsonObject.getJsonObject("prosecutionCase"), caseId, defendantId);
        assertThat(prosecutioncasesJsonObject.getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(0).getJsonObject("personDefendant").getJsonObject("personDetails").getString("firstName"), equalTo("Harry"));

        // when
        helper.adjournHearing();
        verifyPostListCourtHearing(caseId, defendantId, offenceId, applicationId);

    }

}

