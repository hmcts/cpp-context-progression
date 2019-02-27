package uk.gov.moj.cpp.progression;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getProsecutioncasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.assertProsecutionCase;

import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateOffencesHelper;

import java.io.IOException;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;


public class ProsecutionCaseUpdateOffencesIT {

    ProsecutionCaseUpdateOffencesHelper helper;
    private String caseId;
    private String defendantId;
    private String offenceId;

    @Before
    public void setUp() throws IOException {
        caseId = UUID.randomUUID().toString();
        defendantId = UUID.randomUUID().toString();
        offenceId = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1";
        helper = new ProsecutionCaseUpdateOffencesHelper(caseId, defendantId, offenceId);
        createMockEndpoints();
    }

    @Test
    public void shouldUpdateProsecutionCaseOffences() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        String response = getProsecutioncasesProgressionFor(caseId);
        JsonObject prosecutioncasesJsonObject = getJsonObject(response);
        assertProsecutionCase(prosecutioncasesJsonObject.getJsonObject("prosecutionCase"), caseId, defendantId);
        assertThat(prosecutioncasesJsonObject.getJsonObject("prosecutionCase")
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0)
                .getString("offenceCode"), equalTo("TTH105HY"));


        // when
        helper.updateOffences();

        // then
        helper.verifyInActiveMQ();
        helper.verifyInMessagingQueueForOffencesUpdated();
        response = getProsecutioncasesProgressionFor(caseId);
        prosecutioncasesJsonObject = getJsonObject(response);
        assertThat(prosecutioncasesJsonObject.getJsonObject("prosecutionCase")
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0)
                .getString("offenceCode"), equalTo("TEST"));
        assertThat(prosecutioncasesJsonObject.getJsonObject("prosecutionCase")
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0)
                .getInt("count"), equalTo(1));
    }


    @Test
    public void shouldUpdateProsecutionCaseAddDeleteOffences() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        String response = getProsecutioncasesProgressionFor(caseId);
        JsonObject prosecutioncasesJsonObject = getJsonObject(response);
        assertProsecutionCase(prosecutioncasesJsonObject.getJsonObject("prosecutionCase"), caseId, defendantId);
        assertThat(prosecutioncasesJsonObject.getJsonObject("prosecutionCase")
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0)
                .getString("offenceCode"), equalTo("TTH105HY"));

        final String newOffenceId = UUID.randomUUID().toString();

        // when
        helper.updateOffences(newOffenceId);

        // then
        helper.verifyInActiveMQ();
        helper.verifyInMessagingQueueForOffencesUpdated();
        response = getProsecutioncasesProgressionFor(caseId);
        prosecutioncasesJsonObject = getJsonObject(response);
        assertThat(prosecutioncasesJsonObject.getJsonObject("prosecutionCase")
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0)
                .getString("offenceCode"), equalTo("TEST"));
        assertThat(prosecutioncasesJsonObject.getJsonObject("prosecutionCase")
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0)
                .getString("id"), equalTo(newOffenceId));
        assertThat(prosecutioncasesJsonObject.getJsonObject("prosecutionCase")
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0)
                .getInt("count"), equalTo(1));
    }


}

