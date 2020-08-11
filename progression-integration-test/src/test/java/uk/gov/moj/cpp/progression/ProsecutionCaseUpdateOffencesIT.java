package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateOffencesHelper.OFFENCE_CODE;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateOffencesHelper;

import org.hamcrest.Matcher;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class ProsecutionCaseUpdateOffencesIT extends AbstractIT {

    private ProsecutionCaseUpdateOffencesHelper helper;
    private String caseId;
    private String defendantId;

    @Before
    public void setUp() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        final String offenceId = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1";
        helper = new ProsecutionCaseUpdateOffencesHelper(caseId, defendantId, offenceId);
    }

    @Test
    public void shouldUpdateProsecutionCaseOffences() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);

        final Matcher[] caseWithOffenceMatchers = getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceCode", is("TTH105HY")))
        );

        pollProsecutionCasesProgressionFor(caseId, caseWithOffenceMatchers);

        // when
        helper.updateOffences();

        // then
        helper.verifyInActiveMQ();
        helper.verifyInMessagingQueueForOffencesUpdated();
        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceCode", is(OFFENCE_CODE)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].count", is(1)));
    }

    @Test
    public void shouldUpdateProsecutionCaseAddOffences() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);

        final Matcher[] caseWithOffenceMatchers = getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceCode", is("TTH105HY")))
        );
        String payload = pollProsecutionCasesProgressionFor(caseId, caseWithOffenceMatchers);
        final JSONObject jsonObjectPayload = new JSONObject(payload);
        int orderIndex = Integer.parseInt(jsonObjectPayload.getJSONObject("prosecutionCase").getJSONArray("defendants").getJSONObject(0).getJSONArray("offences").getJSONObject(0).get("orderIndex").toString());

        // Add new offence and check orderIndex is incremented
        updateOffenceAndVerify(randomUUID().toString(), ++orderIndex, OFFENCE_CODE);
        String offenceId = randomUUID().toString();
        // Add new offence and check orderIndex is incremented
        updateOffenceAndVerify(offenceId, ++orderIndex, OFFENCE_CODE);
        // Update existing offence and check it has same orderIndex
        updateOffenceAndVerify(offenceId, orderIndex, "TFL124");
        // Add multiple offences and check order
        updateMultipleOffenceAndVerify(orderIndex);


    }

    private void updateMultipleOffenceAndVerify(int orderIndex) {
        String offenceId = randomUUID().toString();
        String secondOffenceId = randomUUID().toString();

        helper.updateMultipleOffences(offenceId, secondOffenceId, "TFL125");
        helper.verifyInActiveMQ();
        helper.verifyInMessagingQueueForOffencesUpdated();

        final Matcher[] matchers = {
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceCode", is("TFL125")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].id", is(offenceId)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].count", is(1)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].orderIndex", is(++orderIndex)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[1].offenceCode", is("TFL125")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[1].id", is(secondOffenceId)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[1].count", is(1)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[1].orderIndex", is(++orderIndex))
        };
        pollProsecutionCasesProgressionFor(caseId, matchers);
    }

    private void updateOffenceAndVerify(String newOffenceId, int orderIndex, String offenceCode) {

        // when
        helper.updateOffences(newOffenceId, offenceCode);

        // then
        helper.verifyInActiveMQ();
        helper.verifyInMessagingQueueForOffencesUpdated();

        final Matcher[] matchers = {
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceCode", is(offenceCode)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].id", is(newOffenceId)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].count", is(1)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].orderIndex", is(orderIndex))
        };
        pollProsecutionCasesProgressionFor(caseId, matchers);
    }
}

