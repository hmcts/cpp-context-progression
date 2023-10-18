package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Duration.FIVE_HUNDRED_MILLISECONDS;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedings;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateOffencesHelper.OFFENCE_CODE;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateOffencesHelper;

import java.io.IOException;
import java.time.LocalDate;

import com.jayway.awaitility.Duration;
import org.hamcrest.Matcher;
import org.json.JSONObject;
import org.junit.Before;
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

    }

    @Test
    public void shouldUpdateVerdictForOffence() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);

        await().atMost(Duration.ONE_MINUTE).pollInterval(FIVE_HUNDRED_MILLISECONDS).until(() -> {
            final Matcher[] caseWithOffenceMatchers = getProsecutionCaseMatchers(caseId, defendantId,
                    singletonList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceCode", is("TTH105HY")))
            );
            final String payload = pollProsecutionCasesProgressionFor(caseId, caseWithOffenceMatchers);
            final JSONObject jsonObjectPayload = new JSONObject(payload);
            final int orderIndex = Integer.parseInt(jsonObjectPayload.getJSONObject("prosecutionCase").getJSONArray("defendants").getJSONObject(0).getJSONArray("offences").getJSONObject(0).get("orderIndex").toString());
            final String hearingId = jsonObjectPayload.getJSONObject("hearingsAtAGlance").getJSONArray("defendantHearings").getJSONObject(0).getJSONArray("hearingIds").get(0).toString();
            // Add new offence and check orderIndex is incremented
            updateOffenceVerdictAndVerify(hearingId, orderIndex, offenceId);
        });
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
                withJsonPath("$.prosecutionCase.defendants[0].offences["+ offenceIndex +"].offenceCode", is(offenceCode)),
                withJsonPath("$.prosecutionCase.defendants[0].offences["+ offenceIndex +"].id", is(newOffenceId)),
                withJsonPath("$.prosecutionCase.defendants[0].offences["+ offenceIndex +"].count", is(1)),
                withJsonPath("$.prosecutionCase.defendants[0].offences["+ offenceIndex +"].orderIndex", is(orderIndex))
        };
        pollProsecutionCasesProgressionFor(caseId, matchers);
    }
}

