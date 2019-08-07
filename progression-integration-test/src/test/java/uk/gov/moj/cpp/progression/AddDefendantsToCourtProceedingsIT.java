package uk.gov.moj.cpp.progression;

import com.google.common.io.Resources;
import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.Duration;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.AddDefendantsToCourtProceedings;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.helper.QueueUtil;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.helper.DefaultRequests.PROGRESSION_QUERY_PROSECUTION_CASE_JSON;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getProsecutioncasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getCommandUri;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getQueryUri;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearing;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.assertProsecutionCase;

@Ignore
public class AddDefendantsToCourtProceedingsIT {

    // This test is ignored and should be refactored.
    private static final Logger LOGGER = LoggerFactory.getLogger(AddDefendantsToCourtProceedingsIT.class);

    private static final String PROGRESSION_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS_JSON = "application/vnd.progression.add-defendants-to-court-proceedings+json";

    static final String PUBLIC_PROGRESSION_DEFENDANTS_ADDED_TO_COURT_PROCEEDINGS = "public.progression.defendants-added-to-court-proceedings";

    private static final MessageConsumer messageConsumerClientPublic =  publicEvents.createConsumer(PUBLIC_PROGRESSION_DEFENDANTS_ADDED_TO_COURT_PROCEEDINGS);
    private static final MessageProducer messageProducerClientPublic = publicEvents.createProducer();

    @Before
    public void setUp() {
        createMockEndpoints();
    }

    @AfterClass
    public static void tearDown() throws JMSException {
        messageConsumerClientPublic.close();
        messageProducerClientPublic.close();
    }

    @Test
    public void shouldInvokeDefentantsAddedToCaseAndListHearingRequestEvents() throws Exception {

        final String caseId = UUID.randomUUID().toString();
        final String defendantId = UUID.randomUUID().toString();
        final String offenceId = UUID.randomUUID().toString();
        final String defendantId2 = UUID.randomUUID().toString();

        //Create prosecution case
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        verifyPostListCourtHearing(caseId, defendantId);
        String response = getProsecutioncasesProgressionFor(caseId);
        JsonObject prosecutioncasesJsonObject = getJsonObject(response);
        assertProsecutionCase(prosecutioncasesJsonObject.getJsonObject("prosecutionCase"), caseId, defendantId);

        //Create payload for
        AddDefendantsToCourtProceedings addDefendantsToCourtProceedings = buildAddDefendantsToCourtProceedings(
                true, caseId, defendantId, defendantId2, offenceId);
        String addDefendantsToCourtProceedingsJson = Utilities.JsonUtil.toJsonString(addDefendantsToCourtProceedings);

        //Post command progression.add-defendants-to-court-proceedings
        postCommand(getCommandUri("/adddefendantstocourtproceedings"),
                                                            PROGRESSION_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS_JSON,
                                                            addDefendantsToCourtProceedingsJson);

        //Verify the defendants and check the duplicate is not added
        verifyDefendantsAddedInViewStore(caseId, defendantId2);
    }

    @Test
    public void shouldInvokeDefentantsNotAddedToCaseAndListHearingRequestEvents() throws Exception {

        final String caseId = UUID.randomUUID().toString();
        final String defendantId = UUID.randomUUID().toString();
        final String offenceId = UUID.randomUUID().toString();
        final String defendantId2 = UUID.randomUUID().toString();

        //Create prosecution case
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        String response = getProsecutioncasesProgressionFor(caseId);
        JsonObject prosecutioncasesJsonObject = getJsonObject(response);
        assertProsecutionCase(prosecutioncasesJsonObject.getJsonObject("prosecutionCase"), caseId, defendantId);

        //Create payload for
        AddDefendantsToCourtProceedings addDefendantsToCourtProceedings = buildAddDefendantsToCourtProceedings(
                false, caseId, defendantId, defendantId2, offenceId);
        String addDefendantsToCourtProceedingsJson = Utilities.JsonUtil.toJsonString(addDefendantsToCourtProceedings);

        //Post command progression.add-defendants-to-court-proceedings
        postCommand(getCommandUri("/adddefendantstocourtproceedings"),
                PROGRESSION_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS_JSON,
                addDefendantsToCourtProceedingsJson);

        //Verify the defendants and check the duplicate is not added
        verifyDefendantsNotAddedInViewStore(caseId, defendantId2);
    }

    @Test
    public void shouldListHearingRequestsInvokePublicMessage() throws Exception {

        final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
        final String PUBLIC_LISTING_HEARING_UPDATED = "public.listing.hearing-updated";

        final String caseId = UUID.randomUUID().toString();
        final String defendantId = UUID.randomUUID().toString();

        final String userId = randomUUID().toString();
        final String courtCentreId = randomUUID().toString();
        final String offenceId = UUID.randomUUID().toString();
        final String defendantId2 = UUID.randomUUID().toString();

        addProsecutionCaseToCrownCourt(caseId, defendantId);

        final JsonObject prosecutionCaseJson = getJsonObject(getProsecutioncasesProgressionFor(caseId));
        assertProsecutionCase(prosecutionCaseJson.getJsonObject("prosecutionCase"), caseId, defendantId);

        //This had failed mnysteriously and so JsonObject is used to get hearingId as follows.
        //final GetCaseAtAGlance caseAtAGlance = new JsonObjectToObjectConverter().convert(prosecutionCaseJson.getJsonObject("caseAtAGlance"), GetCaseAtAGlance.class);

        final Optional<JsonValue> defendantHearing = prosecutionCaseJson.getJsonObject("caseAtAGlance")
                .getJsonArray("defendantHearings")
                .stream().filter(def1 -> ((JsonObject) def1).getString("defendantId").equals(defendantId))
                .findFirst();

        String hearingId = UUID.randomUUID().toString();

        if (defendantHearing.isPresent())  {
            hearingId = ((JsonObject) defendantHearing.get()).getJsonArray("hearingIds").get(0).toString().replaceAll("\"", "");
            final Metadata hearingConfirmedMetadata = createMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId);
            final JsonObject hearingConfirmedJson = getHearingConfirmedJsonObject(caseId, hearingId, defendantId, courtCentreId);
            sendMessage(messageProducerClientPublic, PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, hearingConfirmedMetadata);
        }

        //otherwise it creates new hearing with status as HEARING_INITIALISED
        final Metadata hearingUpdatedMetadata = createMetadata(PUBLIC_LISTING_HEARING_UPDATED, userId);
        final JsonObject hearingUpdatedJson = getHearingUpdatedJsonObject(caseId, hearingId, defendantId, courtCentreId);
        sendMessage(messageProducerClientPublic, PUBLIC_LISTING_HEARING_UPDATED, hearingUpdatedJson, hearingUpdatedMetadata);

        verifyHearingInitialised(caseId, hearingId);

        //Create payload for
        AddDefendantsToCourtProceedings addDefendantsToCourtProceedings = buildAddDefendantsToCourtProceedings(
                true, caseId, defendantId, defendantId2, offenceId);
        String addDefendantsToCourtProceedingsJson = Utilities.JsonUtil.toJsonString(addDefendantsToCourtProceedings);

        //Post command progression.add-defendants-to-court-proceedings
        postCommand(getCommandUri("/adddefendantstocourtproceedings"),
                PROGRESSION_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS_JSON,
                addDefendantsToCourtProceedingsJson);

        //Verify the defendants and check the duplicate is not added
        verifyDefendantsAddedInViewStore(caseId, defendantId2);

        //Verify public.progression.defendants-added-to-court-proceedings message in the public queue
        verifyInMessagingQueueForDefendantsAddedToCourtHearings(caseId, defendantId2);
    }

    private void verifyInMessagingQueueForDefendantsAddedToCourtHearings(String caseId, String defendantId) {
        final Optional<JsonObject> message =
                QueueUtil.retrieveMessageAsJsonObject(messageConsumerClientPublic);
        assertTrue(message.isPresent());
        assertThat(message.get(), isJson(withJsonPath("$.listHearingRequests[0].listDefendantRequests[0].prosecutionCaseId",
                Matchers.hasToString(Matchers.containsString(caseId)))));
        assertThat(message.get(), isJson(withJsonPath("$.listHearingRequests[0].listDefendantRequests[0].defendantId",
                Matchers.hasToString(Matchers.containsString(defendantId)))));
    }

    private static void verifyHearingInitialised(final String caseId, final String hearingId) {
        poll(requestParams(getQueryUri("/prosecutioncases/" + caseId), PROGRESSION_QUERY_PROSECUTION_CASE_JSON)
                .withHeader(USER_ID, UUID.randomUUID()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.caseAtAGlance.hearings[0].id", CoreMatchers.equalTo(hearingId)),
                                withJsonPath("$.caseAtAGlance.hearings[0].hearingListingStatus", CoreMatchers.equalTo("HEARING_INITIALISED"))
                        )));
    }

    private void verifyDefendantsAddedInViewStore(String caseId, String defendantId) {

        LOGGER.info("Vertifying for defendants added to caseId {} and defendant id {}", caseId, defendantId);

        Awaitility.waitAtMost(Duration.TEN_SECONDS).until(() -> {
            final String response = getProsecutioncasesProgressionFor(caseId);
            JsonObject prosecutioncasesJsonObject = getJsonObject(response);

            assertThat(prosecutioncasesJsonObject, notNullValue());

            Optional<JsonValue> defedantJsonObject = prosecutioncasesJsonObject.getJsonObject("prosecutionCase").getJsonArray("defendants")
                    .stream().filter(def1 -> ((JsonObject) def1).getString("id").equals(defendantId))
                    .findFirst();

            assertThat(defedantJsonObject.isPresent(), is(true));

            assertThat(((JsonObject) defedantJsonObject.get()).getString("id"), equalTo(defendantId));

            assertThat(((JsonObject) defedantJsonObject.get()).getString("prosecutionCaseId"), equalTo(caseId));
        });
    }

    private void verifyDefendantsNotAddedInViewStore(String caseId, String defendantId) {

        LOGGER.info("Vertifying for defendants not added to caseId {} and defendant id {}", caseId, defendantId);

        Awaitility.waitAtMost(Duration.TEN_SECONDS).until(() -> {
            String response = getProsecutioncasesProgressionFor(caseId);
            JsonObject prosecutioncasesJsonObject = getJsonObject(response);

            assertThat(prosecutioncasesJsonObject, notNullValue());

            Optional<JsonValue> defedantJsonObject = prosecutioncasesJsonObject.getJsonObject("prosecutionCase").getJsonArray("defendants")
                    .stream().filter(def1 -> ((JsonObject) def1).getString("id").equals(defendantId))
                    .findFirst();

            assertThat(defedantJsonObject.isPresent(), is(false));
        });
    }

    private AddDefendantsToCourtProceedings buildAddDefendantsToCourtProceedings(
            boolean forAdded, String caseId, String defendantId, String defendantId2, String offenceId) {

        final List<Defendant> defendantsList = new ArrayList<>();

        Offence offence = Offence.offence()
                .withId(UUID.fromString(offenceId))
                .withOffenceDefinitionId(UUID.randomUUID())
                .withOffenceCode("TFL123")
                .withOffenceTitle("TFL Ticket Dodger")
                .withWording("TFL ticket dodged")
                .withStartDate(LocalDate.of(2019, 5, 1))
                .withCount(0)
                .build();

        //past duplicate defendant
        Defendant defendant = Defendant.defendant()
                .withId(UUID.fromString(defendantId))
                .withProsecutionCaseId(UUID.fromString(caseId))
                .withOffences(Collections.singletonList(offence))
                .build();
        defendantsList.add(defendant);

        //Add defendant
        Defendant defendant2 = Defendant.defendant()
                .withId(UUID.fromString(defendantId2))
                .withProsecutionCaseId(UUID.fromString(caseId))
                .withOffences(Collections.singletonList(offence))
                .build();

        if(forAdded)
            defendantsList.add(defendant2);

        //Duplicate defendant in current payload
        Defendant defendant3 = Defendant.defendant()
                .withId(UUID.fromString(defendantId2))
                .withProsecutionCaseId(UUID.fromString(caseId))
                .withOffences(Collections.singletonList(offence))
                .build();

        if(forAdded)
            defendantsList.add(defendant3);

        ListDefendantRequest listDefendantRequest2 = ListDefendantRequest.listDefendantRequest()
                .withProsecutionCaseId(UUID.fromString(caseId))
                .withDefendantOffences(Collections.singletonList(UUID.fromString(offenceId)))
                .withDefendantId(defendant2.getId())
                .build();

        HearingType hearingType = HearingType.hearingType().withId(UUID.randomUUID()).withDescription("TO_JAIL").build();
        CourtCentre courtCentre = CourtCentre.courtCentre().withId(UUID.randomUUID()).build();

        ListHearingRequest listHearingRequest = ListHearingRequest.listHearingRequest()
                .withCourtCentre(courtCentre).withHearingType(hearingType)
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withListDefendantRequests(Arrays.asList(listDefendantRequest2))
                .withEarliestStartDateTime(ZonedDateTime.now().plusWeeks(1))
                .withEstimateMinutes(new Integer(20))
                .build();

        return AddDefendantsToCourtProceedings
                .addDefendantsToCourtProceedings()
                .withDefendants(defendantsList)
                .withListHearingRequests(Collections.singletonList(listHearingRequest))
                .build();

    }

    private Metadata createMetadata(final String eventName, String userId) {
        return JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName(eventName)
                .withUserId(userId)
                .build();
    }

    private JsonObject getHearingConfirmedJsonObject(final String caseId, final String hearingId, final String defendantId, final String courtCentreId) {
        return new StringToJsonObjectConverter().convert(
                getPayloadForCreatingRequest("public.listing.hearing-confirmed.json")
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
        );
    }

    private JsonObject getHearingUpdatedJsonObject(final String caseId, final String hearingId, final String defendantId, final String courtCentreId) {
        return new StringToJsonObjectConverter().convert(
                getPayloadForCreatingRequest("public.listing.hearing-updated.json")
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
        );
    }

    private static String getPayloadForCreatingRequest(final String ramlPath) {
        String request = null;
        try {
            request = Resources.toString(
                    Resources.getResource(ramlPath),
                    Charset.defaultCharset()
            );
        } catch (final Exception e) {
            fail("Error consuming file from location " + ramlPath);
        }
        return request;
    }
}
