package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateOffencesHelper.OFFENCE_CODE;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateOffencesHelper;

import java.time.Duration;

import org.hamcrest.Matcher;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProsecutionCaseUpdateOffencesIT extends AbstractIT {

    private static final JmsMessageConsumerClient publicEventsConsumerForOffencesUpdated = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.defendant-offences-changed").getMessageConsumerClient();
    private static final JmsMessageConsumerClient privateEventsConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecution-case-offences-updated").getMessageConsumerClient();

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
        helper.verifyInActiveMQ(privateEventsConsumer);
        helper.verifyInMessagingQueueForOffencesUpdated(publicEventsConsumerForOffencesUpdated);
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
        final JmsMessageConsumerClient consumerForDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed").getMessageConsumerClient();
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);

        await().atMost(Duration.ofMinutes(1)).pollInterval(Duration.ofMillis(500)).until(() -> {
            final Matcher[] caseWithOffenceMatchers = getProsecutionCaseMatchers(caseId, defendantId,
                    singletonList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceCode", is("TTH105HY")))
            );
            final String payload = pollProsecutionCasesProgressionFor(caseId, caseWithOffenceMatchers);
            final JSONObject jsonObjectPayload = new JSONObject(payload);
            final int orderIndex = Integer.parseInt(jsonObjectPayload.getJSONObject("prosecutionCase").getJSONArray("defendants").getJSONObject(0).getJSONArray("offences").getJSONObject(0).get("orderIndex").toString());
            final String hearingId = jsonObjectPayload.getJSONObject("hearingsAtAGlance").getJSONArray("defendantHearings").getJSONObject(0).getJSONArray("hearingIds").get(0).toString();
            // Add new offence and check orderIndex is incremented
            updateOffenceVerdictAndVerify(hearingId, orderIndex, offenceId, consumerForDefendantListingStatusChanged);
            return true;
        });
    }

    @Test
    public void shouldUpdatePleaForOffence() throws Exception {
        final JmsMessageConsumerClient consumerForDefendantListingStatusChanged = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.prosecutionCase-defendant-listing-status-changed").getMessageConsumerClient();
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
        updateOffencePleaAndVerify(hearingId, orderIndex, offenceId, consumerForDefendantListingStatusChanged);
    }

    private void updateOffenceVerdictAndVerify(final String hearingId, final int orderIndex, final String offenceId, final JmsMessageConsumerClient consumerForDefendantListingStatusChanged) throws JSONException {
        // when
        helper.updateOffenceVerdict(hearingId, offenceId);

        final Matcher[] matchers = {
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].id", is(offenceId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].count", is(0)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].orderIndex", is(orderIndex)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].verdict", notNullValue())
        };

        // then
        helper.verifyVerdictInActiveMQ(consumerForDefendantListingStatusChanged, matchers);
    }

    private void updateOffencePleaAndVerify(final String hearingId, final int orderIndex, final String offenceId, final JmsMessageConsumerClient consumerForDefendantListingStatusChanged) throws JSONException {

        // when
        helper.updateOffencePlea(hearingId, offenceId);

        final Matcher[] matchers = {
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].id", is(offenceId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].count", is(0)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].orderIndex", is(orderIndex)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].plea", notNullValue())
        };

        // then
        helper.verifyVerdictInActiveMQ(consumerForDefendantListingStatusChanged, matchers);
    }

    private void updateOffenceAndVerify(final String newOffenceId, final int orderIndex, final String offenceCode) throws JSONException {

        final int offenceIndex = orderIndex - 1;

        // when
        helper.updateOffences(newOffenceId, offenceCode);

        // then
        helper.verifyInActiveMQ(privateEventsConsumer);
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

