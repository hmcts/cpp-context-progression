package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.CaseHearingsQueryHelper.pollForHearing;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollHearingWithStatusInitialised;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollHearingWithStatusResulted;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonPath;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommandWithUserId;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupLoggedInUsersPermissionQueryStub;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearingContaining;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.setupAsAuthorisedUser;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * SPRDT-1362. A split lists the moved offences as a brand new hearing and removes them from the
 * source, reusing the two flows progression already has rather than introducing a new event.
 *
 * <p>The happy-path test has to tell the split's call to listing apart from the one the case
 * referral already made for the same case and defendant. It does that on offence content: the new
 * hearing carries only the offence that moved, never the one that stayed behind.
 */
public class SplitHearingIT extends AbstractIT {

    private static final String SPLIT_HEARING_MEDIA_TYPE = "application/vnd.progression.split-hearing+json";
    private static final String PUBLIC_OFFENCES_REMOVED =
            "public.progression.offences-removed-from-existing-allocated-hearing";
    private static final StringToJsonObjectConverter STRING_TO_JSON = new StringToJsonObjectConverter();

    @BeforeEach
    public void setUp() {
        setupLoggedInUsersPermissionQueryStub();
    }

    @Test
    public void shouldListMovedOffencesAsANewHearingAndRemoveThemFromTheSource() throws Exception {
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();

        addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId, defendantId);
        final String sourceHearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonArray offences = offencesOnHearing(sourceHearingId);
        assertThat(offences, hasSize(2));
        final String movingOffenceId = offences.getJsonObject(0).getString("id");
        final String stayingOffenceId = offences.getJsonObject(1).getString("id");

        final JmsMessageConsumerClient offencesRemoved = newPublicJmsMessageConsumerClientProvider()
                .withEventNames(PUBLIC_OFFENCES_REMOVED)
                .getMessageConsumerClient();

        final Response response = postSplit(sourceHearingId,
                splitPayload(sourceHearingId, caseId, defendantId, movingOffenceId));
        assertThat(response.getStatusCode(), is(SC_ACCEPTED));

        // The removal half, published for anyone holding the source hearing.
        final JsonPath removed = retrieveMessageAsJsonPath(offencesRemoved, isJson(allOf(
                withJsonPath("$.hearingId", is(sourceHearingId)),
                withJsonPath("$.offenceIds", hasSize(1)),
                withJsonPath("$.offenceIds[0]", is(movingOffenceId)))));
        assertNotNull(removed);

        // The listing half: a new hearing goes to listing carrying only the offence that moved.
        verifyPostListCourtHearingContaining(
                asList(caseId, defendantId, movingOffenceId),
                singletonList(stayingOffenceId));

        // The source hearing keeps exactly what was left behind.
        pollForHearing(sourceHearingId,
                withJsonPath("$.hearing.id", is(sourceHearingId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences.length()", is(1)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].id", is(stayingOffenceId)));
    }

    /**
     * The real caller is listing's proxy, which sends only listNewHearing and
     * sendNotificationToParties — the hearing is identified by the URI parameter alone. The
     * framework merges that parameter into the payload after schema validation, so a body without
     * hearingId must be accepted and must still split the hearing named in the URI.
     */
    @Test
    public void shouldAcceptASplitWhoseBodyOmitsHearingId() throws Exception {
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();

        addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId, defendantId);
        final String sourceHearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonArray offences = offencesOnHearing(sourceHearingId);
        final String movingOffenceId = offences.getJsonObject(0).getString("id");
        final String stayingOffenceId = offences.getJsonObject(1).getString("id");

        final JsonObject bodyWithoutHearingId = createObjectBuilder(
                splitPayload(sourceHearingId, caseId, defendantId, movingOffenceId))
                .remove("hearingId")
                .build();

        final Response response = postSplit(sourceHearingId, bodyWithoutHearingId);
        assertThat(response.getStatusCode(), is(SC_ACCEPTED));

        pollForHearing(sourceHearingId,
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences.length()", is(1)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].id", is(stayingOffenceId)));
    }

    @Test
    public void shouldRejectSplitForAHearingThatDoesNotExist() {
        final String unknownHearingId = randomUUID().toString();

        final Response response = postSplit(unknownHearingId,
                splitPayload(unknownHearingId, randomUUID().toString(), randomUUID().toString(),
                        randomUUID().toString()));

        assertThat(response.getStatusCode(), is(SC_NOT_FOUND));
    }

    /**
     * Moving every offence is not a split — it would leave an empty hearing behind — so it is
     * rejected rather than quietly treated as a move.
     */
    @Test
    public void shouldRejectSplitThatWouldEmptyTheSourceHearing() throws Exception {
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();

        addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId, defendantId);
        final String sourceHearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonArray offences = offencesOnHearing(sourceHearingId);
        final Response response = postSplit(sourceHearingId, splitPayload(sourceHearingId, caseId, defendantId,
                offences.getJsonObject(0).getString("id"),
                offences.getJsonObject(1).getString("id")));

        assertThat(response.getStatusCode(), is(SC_BAD_REQUEST));
    }

    @Test
    public void shouldRejectSplitForAnOffenceThatIsNotOnTheHearing() throws Exception {
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();

        addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId, defendantId);
        final String sourceHearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final Response response = postSplit(sourceHearingId,
                splitPayload(sourceHearingId, caseId, defendantId, randomUUID().toString()));

        assertThat(response.getStatusCode(), is(SC_BAD_REQUEST));
    }

    /**
     * A resulted hearing is a concluded record; moving offences off it would rewrite history.
     */
    @Test
    public void shouldRejectSplitForAResultedHearing() throws Exception {
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String userId = randomUUID().toString();

        addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId, defendantId);
        final String sourceHearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);
        final JsonArray offences = offencesOnHearing(sourceHearingId);

        resultHearing(sourceHearingId, caseId, defendantId, userId);

        final Response response = postSplit(sourceHearingId, splitPayload(sourceHearingId, caseId, defendantId,
                offences.getJsonObject(0).getString("id")));

        assertThat(response.getStatusCode(), is(SC_CONFLICT));
    }

    /**
     * The split is initiated by a court user through listing's proxy, so the listing groups are
     * granted; a user outside them is refused before any event is raised.
     */
    @Test
    public void shouldRefuseSplitForAUserOutsideTheGrantedGroups() throws Exception {
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String unauthorisedUserId = randomUUID().toString();

        setupAsAuthorisedUser(fromString(unauthorisedUserId), "stub-data/usersgroups.get-invalid-groups-by-user.json");

        addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId, defendantId);
        final String sourceHearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);
        final JsonArray offences = offencesOnHearing(sourceHearingId);

        final Response response = postCommandWithUserId(
                getWriteUrl("/hearing/" + sourceHearingId + "/split"),
                SPLIT_HEARING_MEDIA_TYPE,
                splitPayload(sourceHearingId, caseId, defendantId,
                        offences.getJsonObject(0).getString("id")).toString(),
                unauthorisedUserId);

        assertThat(response.getStatusCode(), is(SC_FORBIDDEN));
    }

    private static Response postSplit(final String hearingId, final JsonObject payload) {
        return postCommand(getWriteUrl("/hearing/" + hearingId + "/split"),
                SPLIT_HEARING_MEDIA_TYPE,
                payload.toString());
    }

    private static JsonArray offencesOnHearing(final String hearingId) {
        final String hearingPayload = pollForHearing(hearingId, withJsonPath("$.hearing.id", is(hearingId)));
        return getJsonObject(hearingPayload)
                .getJsonObject("hearing")
                .getJsonArray("prosecutionCases").getJsonObject(0)
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences");
    }

    /**
     * hearingId is carried as a URI parameter; the REST adapter merges it into the payload, which is
     * why the request schema declares it.
     */
    private static JsonObject splitPayload(final String hearingId,
                                           final String caseId,
                                           final String defendantId,
                                           final String... offenceIds) {
        final javax.json.JsonArrayBuilder offences = createArrayBuilder();
        for (final String offenceId : offenceIds) {
            offences.add(offenceId);
        }

        final JsonObjectBuilder listNewHearing = createObjectBuilder()
                .add("jurisdictionType", "CROWN")
                .add("estimatedMinutes", 360)
                .add("listDefendantRequests", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("prosecutionCaseId", caseId)
                                .add("defendantId", defendantId)
                                .add("defendantOffences", offences)));

        return createObjectBuilder()
                .add("hearingId", hearingId)
                .add("listNewHearing", listNewHearing)
                .add("sendNotificationToParties", false)
                .build();
    }

    private static void resultHearing(final String hearingId,
                                      final String caseId,
                                      final String defendantId,
                                      final String userId) {
        final JmsMessageProducerClient producer = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

        final JsonObject hearingConfirmed = STRING_TO_JSON.convert(
                getPayload("public.listing.hearing-confirmed.json")
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", "88cdf36e-93e4-41b0-8277-17d9dba7f06f")
                        .replaceAll("COURT_CENTRE_NAME", "Lavender Hill Magistrate Court"));
        final JsonEnvelope confirmed = envelopeFrom(buildMetadata("public.listing.hearing-confirmed", userId), hearingConfirmed);
        producer.sendMessage("public.listing.hearing-confirmed", confirmed);
        pollHearingWithStatusInitialised(hearingId);

        final JsonObject hearingResulted = STRING_TO_JSON.convert(
                getPayload("public.events.hearing.hearing-resulted-with-custody.json")
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", "88cdf36e-93e4-41b0-8277-17d9dba7f06f")
                        .replaceAll("COURT_CENTRE_NAME", "Lavender Hill Magistrate Court"));
        final JsonEnvelope resulted = envelopeFrom(buildMetadata("public.events.hearing.hearing-resulted", userId), hearingResulted);
        producer.sendMessage("public.events.hearing.hearing-resulted", resulted);
        pollHearingWithStatusResulted(hearingId);
    }
}
