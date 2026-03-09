package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithDefendantAsAdult;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.*;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.messaging.Metadata;


import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


class HearingListedYouthUpdateIT extends AbstractIT {

    private static final String PUBLIC_LISTING_HEARING_LISTED = "public.listing.hearing-listed";
    private static final String PROGRESSION_COMMAND_UPDATE_DEFENDANT = "progression.command.update-defendant-for-prosecution-case";

    private String userId;
    private String courtCentreId;
    private String courtCentreName;
    private JmsMessageConsumerClient updateDefendantConsumer;

    @BeforeEach
    void setUp() {
        userId = randomUUID().toString();
        courtCentreId = "111bdd2a-6b7a-4002-bc8c-5c6f93844f40";
        courtCentreName="Lavender Hill Magistrates' Court";
    }



    @Test
    void shouldUpdateYouthOnlyOnceWhenHearingListedMultipleTimes() throws Exception {
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String offenceId = randomUUID().toString();

        addProsecutionCaseToCrownCourtWithDefendantAsAdult(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId,
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.prosecutionCase.defendants[0].id", is(defendantId)));

        updateDefendantConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME)
                .withEventNames(PROGRESSION_COMMAND_UPDATE_DEFENDANT)
                .getMessageConsumerClient();

        final JsonObject firstHearingListedPayload = buildHearingListedPayload(caseId, defendantId, offenceId, randomUUID().toString());
        final Metadata metadata = buildMetadata(PUBLIC_LISTING_HEARING_LISTED, userId);
        sendPublicEvent(PUBLIC_LISTING_HEARING_LISTED, firstHearingListedPayload, metadata);


        pollProsecutionCasesProgressionFor(caseId,
                withJsonPath("$.prosecutionCase.defendants[0].isYouth", is(true)));

        final JsonObject secondHearingListedPayload = buildHearingListedPayload(caseId, defendantId, offenceId, randomUUID().toString());
        sendPublicEvent(PUBLIC_LISTING_HEARING_LISTED, secondHearingListedPayload, buildMetadata(PUBLIC_LISTING_HEARING_LISTED, userId));

        assertThat(retrieveMessageAsJsonPath(updateDefendantConsumer, 2000).isPresent(), is(false));
    }

    private JsonObject buildHearingListedPayload(final String caseId, final String defendantId, final String offenceId, final String hearingId) {
        final String payload = getPayload("public.listing.hearing-listed.json")
                .replaceAll("CASE_ID", caseId)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("OFFENCE_ID", offenceId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId)
                .replaceAll("COURT_CENTRE_NAME",courtCentreName);

        return stringToJsonObjectConverter.convert(payload);
    }
}
