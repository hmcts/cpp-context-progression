package uk.gov.moj.cpp.progression;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getProsecutioncasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.assertProsecutionCase;

import org.junit.Before;
import org.junit.Test;
import uk.gov.moj.cpp.progression.util.ConvictionDateHelper;
import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantHelper;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.io.IOException;
import java.util.UUID;


public class ConvictionDateIT {

    ConvictionDateHelper helper;
    private String caseId;
    private String defendantId;
    private String offenceId;

    @Before
    public void setUp() throws IOException {
        caseId = UUID.randomUUID().toString();
        defendantId = UUID.randomUUID().toString();
        offenceId = UUID.fromString("3789ab16-0bb7-4ef1-87ef-c936bf0364f1").toString();
        helper = new ConvictionDateHelper(caseId, offenceId);
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
        helper.addConvictionDate();

        // then
        helper.verifyInActiveMQForConvictionDateChanged();
        response = getProsecutioncasesProgressionFor(caseId);
        prosecutioncasesJsonObject = getJsonObject(response);
        assertThat(prosecutioncasesJsonObject.getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0).getString("chargeDate"), equalTo("2018-01-01"));
        assertThat(prosecutioncasesJsonObject.getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0).getString("convictionDate"), equalTo("2017-02-02"));

        helper.removeConvictionDate();

        helper.verifyInActiveMQForConvictionDateRemoved();
        response = getProsecutioncasesProgressionFor(caseId);
        prosecutioncasesJsonObject = getJsonObject(response);
        assertEquals(JsonValue.NULL, prosecutioncasesJsonObject.getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0).getOrDefault("convictionDate", JsonValue.NULL));

    }

}

