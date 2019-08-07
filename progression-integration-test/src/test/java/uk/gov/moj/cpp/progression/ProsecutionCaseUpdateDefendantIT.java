package uk.gov.moj.cpp.progression;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getProsecutioncasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.assertProsecutionCase;

import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantHelper;

import java.io.IOException;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;

public class ProsecutionCaseUpdateDefendantIT {

    ProsecutionCaseUpdateDefendantHelper helper;
    private String caseId;
    private String defendantId;

    @Before
    public void setUp() throws IOException {
        caseId = UUID.randomUUID().toString();
        defendantId = UUID.randomUUID().toString();
        helper = new ProsecutionCaseUpdateDefendantHelper(caseId, defendantId);
        createMockEndpoints();
    }

    @Test
    public void shouldUpdateProsecutionCaseDefendant() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        String response = getProsecutioncasesProgressionFor(caseId);
        JsonObject prosecutioncasesJsonObject = getJsonObject(response);
        assertProsecutionCase(prosecutioncasesJsonObject.getJsonObject("prosecutionCase"), caseId, defendantId);
        assertThat(prosecutioncasesJsonObject.getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(0).getJsonObject("personDefendant").getJsonObject("personDetails").getString("firstName"), equalTo("Harry"));

        // when
        helper.updateDefendant();

        // then
        helper.verifyInActiveMQ();
        response = getProsecutioncasesProgressionFor(caseId);
        prosecutioncasesJsonObject = getJsonObject(response);
        assertThat(prosecutioncasesJsonObject.getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(0).getJsonObject("personDefendant").getJsonObject("personDetails").getString("firstName"), equalTo("updatedName"));
        JsonObject defendantJson = prosecutioncasesJsonObject.getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(0);
        assertThat(defendantJson.getString("pncId"), equalTo("1234567"));
        assertThat(defendantJson.getJsonArray("aliases").size(), equalTo(1));
        helper.verifyInMessagingQueueForDefendentChanged();
    }

}

