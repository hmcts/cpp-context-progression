package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateOffencesHelper.OFFENCE_CODE;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateOffencesHelper;

import org.hamcrest.Matcher;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class ProsecutionCaseUpdateOffencesIT extends AbstractIT {

    private ProsecutionCaseUpdateOffencesHelper helper;
    private String caseId;
    private String defendantId;
    private String offenceId;

    @Before
    public void setUp() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        offenceId = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1";
        helper = new ProsecutionCaseUpdateOffencesHelper(caseId, defendantId, offenceId);
    }

    @Test
    public void shouldUpdateProsecutionCaseOffences() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);

        final Matcher[] caseWithOffenceMatchers = getProsecutionCaseMatchers(caseId, defendantId,
                newArrayList(
                        // defendant offence reporting restrictions and offencecode assertion
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].id", is("3789ab16-e588-4b7f-806a-44dc0eb0e75e")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].label", is("Complainant's anonymity protected by virtue of Section 1 of the Sexual Offences Amendment Act 1992")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].orderedDate", is("2021-08-28")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceCode", is("TTH105HY")))
        );

        pollProsecutionCasesProgressionFor(caseId, caseWithOffenceMatchers);

        // when
        helper.updateOffences();

        // then
        helper.verifyInActiveMQ();
        helper.verifyInMessagingQueueForOffencesUpdated();
        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceCode", is(OFFENCE_CODE)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].count", is(1)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceFacts.alcoholReadingMethodCode", is("B")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].id", is("3789ab16-e588-4b7f-806a-44dc0eb0e75e")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].label", is("Complainant's anonymity protected by virtue of Section 1 of the Sexual Offences Amendment Act 1992")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].orderedDate", is("2020-01-01")));
    }

    @Test
    @Ignore("This test is irrelavant after change in case aggregate as we don't delete the offence now")
    public void shouldUpdateProsecutionCaseAddOffences() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);

        final Matcher[] caseWithOffenceMatchers = getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceCode", is("TTH105HY")))
        );
        final String payload = pollProsecutionCasesProgressionFor(caseId, caseWithOffenceMatchers);
        final JSONObject jsonObjectPayload = new JSONObject(payload);
        int orderIndex = Integer.parseInt(jsonObjectPayload.getJSONObject("prosecutionCase").getJSONArray("defendants").getJSONObject(0).getJSONArray("offences").getJSONObject(0).get("orderIndex").toString());

        // Add new offence and check orderIndex is incremented
        updateOffenceAndVerify(randomUUID().toString(), ++orderIndex, OFFENCE_CODE);
        final String offenceId = randomUUID().toString();
        // Add new offence and check orderIndex is incremented
        updateOffenceAndVerify(offenceId, ++orderIndex, OFFENCE_CODE);
        // Update existing offence and check it has same orderIndex
        updateOffenceAndVerify(offenceId, orderIndex, "TFL124");
        // Add multiple offences and check order
        updateMultipleOffenceAndVerify(orderIndex);


    }

    @Test
    public void shouldUpdateVerdictForOffence() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);

        final Matcher[] caseWithOffenceMatchers = getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceCode", is("TTH105HY")))
        );
        final String payload = pollProsecutionCasesProgressionFor(caseId, caseWithOffenceMatchers);
        final JSONObject jsonObjectPayload = new JSONObject(payload);
        final int orderIndex = Integer.parseInt(jsonObjectPayload.getJSONObject("prosecutionCase").getJSONArray("defendants").getJSONObject(0).getJSONArray("offences").getJSONObject(0).get("orderIndex").toString());
        final String hearingId = jsonObjectPayload.getJSONObject("hearingsAtAGlance").getJSONArray("defendantHearings").getJSONObject(0).getJSONArray("hearingIds").get(0).toString();
        // Add new offence and check orderIndex is incremented
        updateOffenceVerdictAndVerify(hearingId, orderIndex, offenceId);
    }

    @Test
    public void shouldUpdatePleaForOffence() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);

        final Matcher[] caseWithOffenceMatchers = getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceCode", is("TTH105HY")))
        );
        final String payload = pollProsecutionCasesProgressionFor(caseId, caseWithOffenceMatchers);
        final JSONObject jsonObjectPayload = new JSONObject(payload);
        final int orderIndex = Integer.parseInt(jsonObjectPayload.getJSONObject("prosecutionCase").getJSONArray("defendants").getJSONObject(0).getJSONArray("offences").getJSONObject(0).get("orderIndex").toString());
        final String hearingId = jsonObjectPayload.getJSONObject("hearingsAtAGlance").getJSONArray("defendantHearings").getJSONObject(0).getJSONArray("hearingIds").get(0).toString();
        // Add new offence and check orderIndex is incremented
        updateOffencePleaAndVerify(hearingId, orderIndex, offenceId);
    }

    private void updateMultipleOffenceAndVerify(int orderIndex) {
        final String offenceId = randomUUID().toString();
        final String secondOffenceId = randomUUID().toString();

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


    private void updateOffenceVerdictAndVerify(final String hearingId, final int orderIndex, final String offenceId) {

        // when
        helper.updateOffenceVerdict(hearingId, offenceId);

        final Matcher[] matchers = {
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].id", is(offenceId)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].count", is(1)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].orderIndex", is(orderIndex)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].verdict", notNullValue())
        };

        // then
        helper.verifyVerdictInActiveMQ(matchers);
    }

    private void updateOffencePleaAndVerify(final String hearingId, final int orderIndex, final String offenceId) {

        // when
        helper.updateOffencePlea(hearingId, offenceId);

        final Matcher[] matchers = {
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].id", is(offenceId)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].count", is(1)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].orderIndex", is(orderIndex)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].plea", notNullValue())
        };

        // then
        helper.verifyVerdictInActiveMQ(matchers);
    }

    private void updateOffenceAndVerify(final String newOffenceId, final int orderIndex, final String offenceCode) {

        final int offenceIndex = orderIndex - 1;

        // when
        helper.updateOffences(newOffenceId, offenceCode);

        // then
        helper.verifyInActiveMQ();
        helper.verifyInMessagingQueueForOffencesUpdated();

        final Matcher[] matchers = {
                withJsonPath("$.prosecutionCase.defendants[0].offence["+ offenceIndex +"].offenceCode", is(offenceCode)),
                withJsonPath("$.prosecutionCase.defendants[0].offence["+ offenceIndex +"].id", is(newOffenceId)),
                withJsonPath("$.prosecutionCase.defendants[0].offence["+ offenceIndex +"].count", is(1)),
                withJsonPath("$.prosecutionCase.defendants[0].offence["+ offenceIndex +"].orderIndex", is(orderIndex))
        };
        pollProsecutionCasesProgressionFor(caseId, matchers);
    }
}

