package uk.gov.moj.cpp.progression;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupUsersGroupQueryStub;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearing;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.moj.cpp.progression.util.AdjournHearingHelper;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;


public class AdjournHearingIT extends AbstractIT {

    AdjournHearingHelper helper;
    private String caseId;
    private String defendantId;
    private String offenceId;
    private String applicationId;

    @Before
    public void setUp() {
        setupUsersGroupQueryStub();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        offenceId = UUID.fromString("3789ab16-0bb7-4ef1-87ef-c936bf0364f1").toString();
        applicationId = randomUUID().toString();
        helper = new AdjournHearingHelper(caseId, defendantId, offenceId, applicationId);
    }

    @Test
    public void shouldListCourtHearing() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        String response = pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        JsonObject prosecutioncasesJsonObject = getJsonObject(response);
        assertThat(prosecutioncasesJsonObject.getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(0).getJsonObject("personDefendant").getJsonObject("personDetails").getString("firstName"), equalTo("Harry"));

        // when
        helper.adjournHearing();
        verifyPostListCourtHearing(caseId, defendantId, offenceId, applicationId);
    }

}

