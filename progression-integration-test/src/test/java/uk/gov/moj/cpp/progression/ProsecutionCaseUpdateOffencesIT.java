package uk.gov.moj.cpp.progression;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.test.utils.core.http.FibonacciPollWithStartAndMax;
import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateOffencesHelper;

import java.time.Duration;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;

import org.hamcrest.Matcher;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.RestHelper.INITIAL_INTERVAL_IN_MILLISECONDS;
import static uk.gov.moj.cpp.progression.helper.RestHelper.INTERVAL_IN_MILLISECONDS;
import static uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateOffencesHelper.OFFENCE_CODE;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

@SuppressWarnings("java:S2699")
public class ProsecutionCaseUpdateOffencesIT extends AbstractIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionCaseUpdateOffencesIT.class);


    private final JmsMessageConsumerClient publicEventsConsumerForOffencesUpdated = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.defendant-offences-changed").getMessageConsumerClient();

    private ProsecutionCaseUpdateOffencesHelper helper;
    private String caseId;
    private String defendantId;
    private String offenceId;

    @BeforeEach
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
        helper.verifyInMessagingQueueForOffencesUpdated(publicEventsConsumerForOffencesUpdated);
        final String casePayload = pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceCode", is(OFFENCE_CODE)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].count", is(1)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceFacts.alcoholReadingMethodCode", is("B")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].id", is("3789ab16-e588-4b7f-806a-44dc0eb0e75e")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].label", is("Complainant's anonymity protected by virtue of Section 1 of the Sexual Offences Amendment Act 1992")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].orderedDate", is("2020-01-01")));

        final JSONObject caseJsonObject = new JSONObject(casePayload);
        final int orderIndex = Integer.parseInt(caseJsonObject.getJSONObject("prosecutionCase").getJSONArray("defendants").getJSONObject(0).getJSONArray("offences").getJSONObject(0).get("orderIndex").toString());
        final String hearingId = caseJsonObject.getJSONObject("hearingsAtAGlance").getJSONArray("defendantHearings").getJSONObject(0).getJSONArray("hearingIds").get(0).toString();

        updateOffenceVerdictAndVerify(hearingId, orderIndex, offenceId, 1);

        updateOffencePleaAndVerify(hearingId, orderIndex, offenceId, 1);

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
    public void shouldUpdateAndClearVerdictForOffence() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);

        await().atMost(Duration.ofSeconds(15)).pollInterval(new FibonacciPollWithStartAndMax(Duration.ofMillis(INITIAL_INTERVAL_IN_MILLISECONDS), Duration.ofMillis(INTERVAL_IN_MILLISECONDS))).until(() -> {
            try {
                final Matcher[] caseWithOffenceMatchers = getProsecutionCaseMatchers(caseId, defendantId,
                        singletonList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceCode", is("TTH105HY")))
                );
                final String payload = pollProsecutionCasesProgressionFor(caseId, caseWithOffenceMatchers);
                final JSONObject jsonObjectPayload = new JSONObject(payload);
                final int orderIndex = Integer.parseInt(jsonObjectPayload.getJSONObject("prosecutionCase").getJSONArray("defendants").getJSONObject(0).getJSONArray("offences").getJSONObject(0).get("orderIndex").toString());
                final String hearingId = jsonObjectPayload.getJSONObject("hearingsAtAGlance").getJSONArray("defendantHearings").getJSONObject(0).getJSONArray("hearingIds").get(0).toString();
                // Add new offence and check orderIndex is incremented
                updateOffenceVerdictAndVerify(hearingId, orderIndex, offenceId, 0);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
            return true;
        });

        await().atMost(Duration.ofSeconds(15)).pollInterval(new FibonacciPollWithStartAndMax(Duration.ofMillis(INITIAL_INTERVAL_IN_MILLISECONDS), Duration.ofMillis(INTERVAL_IN_MILLISECONDS))).until(() -> {
            try {
                final Matcher[] caseWithOffenceMatchers = getProsecutionCaseMatchers(caseId, defendantId,
                        singletonList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceCode", is("TTH105HY")))
                );
                final String payload = pollProsecutionCasesProgressionFor(caseId, caseWithOffenceMatchers);
                final JSONObject jsonObjectPayload = new JSONObject(payload);
                final int orderIndex = Integer.parseInt(jsonObjectPayload.getJSONObject("prosecutionCase").getJSONArray("defendants").getJSONObject(0).getJSONArray("offences").getJSONObject(0).get("orderIndex").toString());
                final String hearingId = jsonObjectPayload.getJSONObject("hearingsAtAGlance").getJSONArray("defendantHearings").getJSONObject(0).getJSONArray("hearingIds").get(0).toString();
                // Add new offence and check orderIndex is incremented
                updateOffenceClearVerdictAndVerify(hearingId, orderIndex, offenceId);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
            return true;
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
        updateOffencePleaAndVerify(hearingId, orderIndex, offenceId, 0);
    }

    private void updateOffenceVerdictAndVerify(final String hearingId, final int orderIndex, final String offenceId, final int offenceCount) throws JSONException {
        // when
        helper.updateOffenceVerdict(hearingId, offenceId);

        final Matcher[] matchers = {
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].id", is(offenceId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].count", is(offenceCount)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].orderIndex", is(orderIndex)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].verdict", notNullValue())
        };

        // then
        getHearingForDefendant(hearingId, matchers);
    }

    private void updateOffenceClearVerdictAndVerify(final String hearingId, final int orderIndex, final String offenceId) throws JSONException {
        // when
        helper.updateClearOffenceVerdict(hearingId, offenceId);

        final Matcher[] matchers = {
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].id", is(offenceId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].count", is(0)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].orderIndex", is(orderIndex)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0]", not(hasItem("verdict")))
        };

        // then
        getHearingForDefendant(hearingId, matchers);

    }

    private void updateOffencePleaAndVerify(final String hearingId, final int orderIndex, final String offenceId, final int offenceCount) throws JSONException {

        // when
        helper.updateOffencePlea(hearingId, offenceId);

        final Matcher[] matchers = {
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].id", is(offenceId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].count", is(offenceCount)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].orderIndex", is(orderIndex)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].plea", notNullValue())
        };

        // then
        getHearingForDefendant(hearingId, matchers);
    }

    private void updateOffenceAndVerify(final String newOffenceId, final int orderIndex, final String offenceCode) throws JSONException {

        final int offenceIndex = orderIndex - 1;

        // when
        helper.updateOffences(newOffenceId, offenceCode);

        // then
        helper.verifyInMessagingQueueForOffencesUpdated(publicEventsConsumerForOffencesUpdated);

        final Matcher[] matchers = {
                withJsonPath("$.prosecutionCase.defendants[0].offences[" + offenceIndex + "].offenceCode", is(offenceCode)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[" + offenceIndex + "].id", is(newOffenceId)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[" + offenceIndex + "].count", is(1)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[" + offenceIndex + "].orderIndex", is(orderIndex))
        };
        pollProsecutionCasesProgressionFor(caseId, matchers);
    }
}

