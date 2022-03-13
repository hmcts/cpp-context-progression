package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.matchDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionAndReturnHearingId;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonObject;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.stub.HearingStub.stubInitiateHearing;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantWithMatchedHelper.getUpdatedDefendantMatchers;
import static uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantWithMatchedHelper.initiateCourtProceedingsForMatchedDefendants;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;

import com.jayway.restassured.path.json.JsonPath;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantWithMatchedHelper;

import java.util.Optional;

import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("squid:S1607")
public class ProsecutionCaseUpdateDefendantWithMatchedIT extends AbstractIT {
    private static final MessageProducer messageProducerClientPublic = publicEvents.createPublicProducer();
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PROGRESSION_QUERY_HEARING_JSON = "application/vnd.progression.query.hearing+json";

    private String prosecutionCaseId_1;
    private String defendantId_1;
    private String masterDefendantId_1;
    private String prosecutionCaseId_2;
    private String defendantId_2;
    private String hearingId;

    @Before
    public void setUp() {
        stubInitiateHearing();
        prosecutionCaseId_1 = randomUUID().toString();
        defendantId_1 = randomUUID().toString();
        masterDefendantId_1 = randomUUID().toString();
        prosecutionCaseId_2 = randomUUID().toString();
        defendantId_2 = randomUUID().toString();

    }

    @Test
    public void shouldUpdateProsecutionCaseDefendantWithMatched() throws Exception {

        // initiation of first case
        try (final MessageConsumer publicEventConsumerForProsecutionCaseCreated = publicEvents
                .createPublicConsumer("public.progression.prosecution-case-created")) {
            initiateCourtProceedingsForMatchedDefendants(prosecutionCaseId_1, defendantId_1, masterDefendantId_1);
            verifyInMessagingQueueForProsecutionCaseCreated(publicEventConsumerForProsecutionCaseCreated);
        }
        Matcher[] prosecutionCaseMatchers = getProsecutionCaseMatchers(prosecutionCaseId_1, defendantId_1, emptyList());
        pollProsecutionCasesProgressionFor(prosecutionCaseId_1, prosecutionCaseMatchers);
        hearingId = pollProsecutionCasesProgressionAndReturnHearingId(prosecutionCaseId_1, defendantId_1, getProsecutionCaseMatchers(prosecutionCaseId_1, defendantId_1));

        // initiation of second case
        try (final MessageConsumer publicEventConsumerForProsecutionCaseCreated = publicEvents
                .createPublicConsumer("public.progression.prosecution-case-created")) {
            initiateCourtProceedingsForMatchedDefendants(prosecutionCaseId_2, defendantId_2, defendantId_2);
            verifyInMessagingQueueForProsecutionCaseCreated(publicEventConsumerForProsecutionCaseCreated);
        }
        prosecutionCaseMatchers = getProsecutionCaseMatchers(prosecutionCaseId_2, defendantId_2, emptyList());
        pollProsecutionCasesProgressionFor(prosecutionCaseId_2, prosecutionCaseMatchers);

        // match defendant2 associated to case 2
        try (final MessageConsumer publicEventConsumerForDefendantUpdated = publicEvents
                .createPublicConsumer("public.progression.case-defendant-changed")){
            matchDefendant(prosecutionCaseId_2, defendantId_2, prosecutionCaseId_1, defendantId_1, masterDefendantId_1);
            verifyInMessagingQueueForDefendantUpdated(publicEventConsumerForDefendantUpdated);
        }

        // confirm Hearing with 2 Prosecution Case
        sendMessage(messageProducerClientPublic,
                PUBLIC_LISTING_HEARING_CONFIRMED, getHearingJsonObject("public.listing.hearing-confirmed-multiple-case.json",
                        hearingId, prosecutionCaseId_1, defendantId_1, prosecutionCaseId_2, defendantId_2), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                        .withUserId(randomUUID().toString())
                        .build());


        Matcher[] hearingMatchers = {
                withJsonPath("$", notNullValue()),
                withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearing.prosecutionCases.[*].id", hasItems(prosecutionCaseId_1, prosecutionCaseId_2)),
                withJsonPath("$.hearing.prosecutionCases.[*].defendants.[*].id", hasItems(defendantId_1, defendantId_2))
        };

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON, hearingMatchers);


        // Update Multiple Defendant on Same Hearing
        final MessageConsumer messageConsumerHearingPopulatedToProbationCaseWorker = privateEvents.createPrivateConsumer("progression.events.hearing-populated-to-probation-caseworker");

        ProsecutionCaseUpdateDefendantWithMatchedHelper prosecutionCaseUpdateDefendantWithMatchedHelper = new ProsecutionCaseUpdateDefendantWithMatchedHelper();

        prosecutionCaseUpdateDefendantWithMatchedHelper.updateDefendant(defendantId_1, prosecutionCaseId_1, hearingId);


        // Verify Original Defendant
        prosecutionCaseMatchers = getUpdatedDefendantMatchers("$.prosecutionCase", prosecutionCaseId_1, defendantId_1, emptyList());
        pollProsecutionCasesProgressionFor(prosecutionCaseId_1, prosecutionCaseMatchers);

        // Verify Matched Defendant
        prosecutionCaseMatchers = getUpdatedDefendantMatchers("$.prosecutionCase", prosecutionCaseId_2, defendantId_2, emptyList());
        pollProsecutionCasesProgressionFor(prosecutionCaseId_2, prosecutionCaseMatchers);

        Matcher[] hearingDefendantMatchers = getUpdatedDefendantMatchers("$.hearing.prosecutionCases[0]", prosecutionCaseId_1, defendantId_1, emptyList());
        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON, hearingDefendantMatchers);

        final JsonPath message = QueueUtil.retrieveMessage(messageConsumerHearingPopulatedToProbationCaseWorker, isJson(allOf(hearingDefendantMatchers)));
        messageConsumerHearingPopulatedToProbationCaseWorker.close();
        assertNotNull(message);
    }

    private void verifyInMessagingQueueForProsecutionCaseCreated(final MessageConsumer publicEventConsumerForProsecutionCaseCreated) {
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(publicEventConsumerForProsecutionCaseCreated);
        assertTrue(message.isPresent());
        final JsonObject reportingRestrictionObject = message.get().getJsonObject("prosecutionCase")
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0)
                .getJsonArray("reportingRestrictions").getJsonObject(0);
        assertNotNull(reportingRestrictionObject);
    }

    private void verifyInMessagingQueueForDefendantUpdated(final MessageConsumer publicEventConsumerForDefendantUpdated) {
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(publicEventConsumerForDefendantUpdated);
        assertTrue(message.isPresent());
    }

    private JsonObject getHearingJsonObject(final String path, final String hearingId,
                                            final String caseId_1, final String defendantId_1,
                                            final String caseId_2, final String defendantId_2) {
        final String strPayload = getPayload(path)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("CASE_ID_1", caseId_1)
                .replaceAll("CASE_ID_2", caseId_2)
                .replaceAll("DEFENDANT_ID_1", defendantId_1)
                .replaceAll("DEFENDANT_ID_2", defendantId_2);
        return new StringToJsonObjectConverter().convert(strPayload);
    }

}

