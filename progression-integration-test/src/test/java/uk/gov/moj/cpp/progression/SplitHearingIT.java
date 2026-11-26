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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
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
    private static final String PRIVATE_LIST_HEARING_REQUESTED = "progression.event.list-hearing-requested";
    private static final StringToJsonObjectConverter STRING_TO_JSON = new StringToJsonObjectConverter();

    private static final String HEARING_TYPE_ID = "4a0e892d-c0c5-3c51-95b8-704d8c781776";
    private static final String COURT_CENTRE_ID = "88cdf36e-93e4-41b0-8277-17d9dba7f06f";
    private static final String COURT_CENTRE_NAME = "Lavender Hill Magistrate Court";
    private static final String COURT_ROOM_ID = "9e4932f7-97b2-3010-b942-ddd2624e4dd8";
    private static final String NEW_HEARING = "$.hearings[0]";

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
     * AC-5, CROWN: Crown lists a split as one virtual descriptor covering the whole sitting day —
     * a single 1080-minute slot, not three sessions of 360. AC-3: everything the listing officer
     * chose has to reach listing intact on a hearing id that is <em>not</em> the source's.
     */
    @Test
    public void shouldListACrownSplitAsOneAllDayBlock() throws Exception {
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();

        addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId, defendantId);
        final String sourceHearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonArray offences = offencesOnHearing(sourceHearingId);
        final String movingOffenceId = offences.getJsonObject(0).getString("id");
        final String stayingOffenceId = offences.getJsonObject(1).getString("id");

        final String slotDay = "2026-09-10";
        final JsonArray bookedSlots = createArrayBuilder()
                .add(slot(slotDay + "T09:00:00.000Z", 1080, "AD"))
                .build();

        final Response response = postSplit(sourceHearingId, splitPayloadForListing(
                sourceHearingId, caseId, defendantId, "CROWN", 1080, bookedSlots, null, false,
                movingOffenceId));
        assertThat(response.getStatusCode(), is(SC_ACCEPTED));

        final String listingRequest = verifyPostListCourtHearingContaining(
                asList(caseId, defendantId, movingOffenceId), singletonList(stayingOffenceId));

        assertThat(listingRequest, isJson(allOf(
                withJsonPath(NEW_HEARING + ".jurisdictionType", is("CROWN")),
                withJsonPath(NEW_HEARING + ".estimatedMinutes", is(1080)),
                withJsonPath(NEW_HEARING + ".courtCentre.id", is(COURT_CENTRE_ID)),
                withJsonPath(NEW_HEARING + ".bookedSlots", hasSize(1)),
                withJsonPath(NEW_HEARING + ".bookedSlots[0].duration", is(1080)),
                withJsonPath(NEW_HEARING + ".bookedSlots[0].startTime", containsString(slotDay)))));

        assertNewHearingIsSentForListing(listingRequest, sourceHearingId);
    }

    /**
     * AC-5, MAGISTRATES: a magistrates split is listed as separate half-day sessions, and they need
     * not be consecutive. This is the shape most likely to break courtscheduler booking, so all
     * three slots have to survive the hop to listing, along with the week-commencing window.
     */
    @Test
    public void shouldListAMagistratesSplitAcrossThreeNonConsecutiveDays() throws Exception {
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();

        addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId, defendantId);
        final String sourceHearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonArray offences = offencesOnHearing(sourceHearingId);
        final String movingOffenceId = offences.getJsonObject(0).getString("id");
        final String stayingOffenceId = offences.getJsonObject(1).getString("id");

        final String firstDay = "2026-09-14";
        final String secondDay = "2026-09-17";
        final String thirdDay = "2026-09-21";
        final JsonArray bookedSlots = createArrayBuilder()
                .add(slot(firstDay + "T09:00:00.000Z", 360, "AM"))
                .add(slot(secondDay + "T09:00:00.000Z", 360, "AM"))
                .add(slot(thirdDay + "T09:00:00.000Z", 360, "AM"))
                .build();
        final JsonObject weekCommencing = createObjectBuilder()
                .add("startDate", firstDay)
                .add("duration", 2)
                .build();

        final Response response = postSplit(sourceHearingId, splitPayloadForListing(
                sourceHearingId, caseId, defendantId, "MAGISTRATES", 1080, bookedSlots,
                weekCommencing, false, movingOffenceId));
        assertThat(response.getStatusCode(), is(SC_ACCEPTED));

        final String listingRequest = verifyPostListCourtHearingContaining(
                asList(caseId, defendantId, movingOffenceId), singletonList(stayingOffenceId));

        assertThat(listingRequest, isJson(allOf(
                withJsonPath(NEW_HEARING + ".jurisdictionType", is("MAGISTRATES")),
                withJsonPath(NEW_HEARING + ".bookedSlots", hasSize(3)),
                withJsonPath(NEW_HEARING + ".bookedSlots[*].duration", contains(360, 360, 360)),
                withJsonPath(NEW_HEARING + ".bookedSlots[0].startTime", containsString(firstDay)),
                withJsonPath(NEW_HEARING + ".bookedSlots[1].startTime", containsString(secondDay)),
                withJsonPath(NEW_HEARING + ".bookedSlots[2].startTime", containsString(thirdDay)),
                withJsonPath(NEW_HEARING + ".weekCommencingDate.startDate", containsString(firstDay)))));

        assertNewHearingIsSentForListing(listingRequest, sourceHearingId);
    }

    /**
     * AC-3, notification half. What a split owns is carrying the caller's choice through to the
     * event the notification processor reads; the happy-path tests above all send {@code false}, so
     * without this the flag is only ever exercised one way.
     *
     * <p>This stops at the event rather than waiting for an email. Sending one reaches
     * {@code DefenceService.getDefenceOrganisationByDefendantId}, which calls
     * {@code payloadAsJsonObject()} on the defence context's reply without allowing for an empty
     * one, so it throws for any defendant with no defence organisation — as the defendants built
     * here have none. That is a pre-existing gap in a service this ticket does not touch, and
     * {@code ListNewHearingIT} already covers the full email path with a fixture that has one.
     */
    @Test
    public void shouldNotifyPartiesWhenTheSplitAsksForIt() throws Exception {
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();

        addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId, defendantId);
        final String sourceHearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonArray offences = offencesOnHearing(sourceHearingId);
        final String movingOffenceId = offences.getJsonObject(0).getString("id");

        final JmsMessageConsumerClient listHearingRequested = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME)
                .withEventNames(PRIVATE_LIST_HEARING_REQUESTED)
                .getMessageConsumerClient();

        final JsonArray bookedSlots = createArrayBuilder()
                .add(slot("2026-09-10T09:00:00.000Z", 1080, "AD"))
                .build();

        final Response response = postSplit(sourceHearingId, splitPayloadForListing(
                sourceHearingId, caseId, defendantId, "CROWN", 1080, bookedSlots, null, true,
                movingOffenceId));
        assertThat(response.getStatusCode(), is(SC_ACCEPTED));

        final JsonPath raised = retrieveMessageAsJsonPath(listHearingRequested,
                isJson(withJsonPath("$.sendNotificationToParties", is(true))));
        assertNotNull(raised);
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

    /**
     * The split must list a <em>new</em> hearing: if the id reaching listing were the source's, the
     * source would be re-listed and its court-schedule booking overwritten, which is the SPRDT-1227
     * production defect this whole flow exists to fix.
     */
    private static void assertNewHearingIsSentForListing(final String listingRequest,
                                                         final String sourceHearingId) {
        final String newHearingId = getJsonObject(listingRequest)
                .getJsonArray("hearings").getJsonObject(0).getString("id");
        assertThat(newHearingId, is(not(sourceHearingId)));

        pollForHearing(newHearingId,
                withJsonPath("$.hearing.id", is(newHearingId)),
                withJsonPath("$.hearingListingStatus", is("SENT_FOR_LISTING")));
    }

    private static JsonObject slot(final String startTime, final int duration, final String session) {
        return createObjectBuilder()
                .add("startTime", startTime)
                .add("duration", duration)
                .add("session", session)
                .add("oucode", "C55BN00")
                .add("courtScheduleId", randomUUID().toString())
                .add("courtCentreId", COURT_CENTRE_ID)
                .add("roomId", COURT_ROOM_ID)
                .build();
    }

    /**
     * The full court-calendar payload the front end sends through listing's proxy, as opposed to
     * the minimal one the rejection tests use.
     */
    private static JsonObject splitPayloadForListing(final String hearingId,
                                                     final String caseId,
                                                     final String defendantId,
                                                     final String jurisdictionType,
                                                     final int estimatedMinutes,
                                                     final JsonArray bookedSlots,
                                                     final JsonObject weekCommencingDate,
                                                     final boolean sendNotificationToParties,
                                                     final String... offenceIds) {
        final javax.json.JsonArrayBuilder offences = createArrayBuilder();
        for (final String offenceId : offenceIds) {
            offences.add(offenceId);
        }

        final JsonObjectBuilder listNewHearing = createObjectBuilder()
                .add("jurisdictionType", jurisdictionType)
                .add("estimatedMinutes", estimatedMinutes)
                .add("earliestStartDateTime", "2026-09-10T09:00:00.000Z")
                .add("hearingType", createObjectBuilder()
                        .add("id", HEARING_TYPE_ID)
                        .add("description", "First hearing"))
                .add("courtCentre", createObjectBuilder()
                        .add("id", COURT_CENTRE_ID)
                        .add("name", COURT_CENTRE_NAME)
                        .add("roomId", COURT_ROOM_ID))
                .add("bookedSlots", bookedSlots)
                .add("listDefendantRequests", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("prosecutionCaseId", caseId)
                                .add("defendantId", defendantId)
                                .add("defendantOffences", offences)));

        if (weekCommencingDate != null) {
            listNewHearing.add("weekCommencingDate", weekCommencingDate);
        }

        return createObjectBuilder()
                .add("hearingId", hearingId)
                .add("listNewHearing", listNewHearing)
                .add("sendNotificationToParties", sendNotificationToParties)
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
                        .replaceAll("COURT_CENTRE_NAME", COURT_CENTRE_NAME));
        final JsonEnvelope confirmed = envelopeFrom(buildMetadata("public.listing.hearing-confirmed", userId), hearingConfirmed);
        producer.sendMessage("public.listing.hearing-confirmed", confirmed);
        pollHearingWithStatusInitialised(hearingId);

        final JsonObject hearingResulted = STRING_TO_JSON.convert(
                getPayload("public.events.hearing.hearing-resulted-with-custody.json")
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", "88cdf36e-93e4-41b0-8277-17d9dba7f06f")
                        .replaceAll("COURT_CENTRE_NAME", COURT_CENTRE_NAME));
        final JsonEnvelope resulted = envelopeFrom(buildMetadata("public.events.hearing.hearing-resulted", userId), hearingResulted);
        producer.sendMessage("public.events.hearing.hearing-resulted", resulted);
        pollHearingWithStatusResulted(hearingId);
    }
}
