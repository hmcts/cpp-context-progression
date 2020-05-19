package uk.gov.moj.cpp.progression;

import org.hamcrest.Matcher;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.HearingStub;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.extendHearing;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.stub.ListingStub.getHearingIdFromListCourtHearingRequest;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearing;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

public class ExtendHearingIT extends AbstractIT {

    private static final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED_FILE = "public.listing.hearing-confirmed.json";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED_WITH_EXTENDED_HEARING_ID = "public.listing.hearing-confirmed-with-extended-hearing-id.json";
    private static final String PROGRESSION_QUERY_HEARING_JSON = "application/vnd.progression.query.hearing+json";
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtendHearingIT.class);
    private static final MessageProducer messageProducerClientPublic = publicEvents.createProducer();
    private static final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents.createConsumer("progression.event.prosecutionCase-defendant-listing-status-changed");
    private static final MessageConsumer messageConsumerProgressionHearingExtendedEvent = privateEvents.createConsumer("progression.event.hearing-extended");
    private static final MessageConsumer messageConsumerProgressionSummonsDataPreparedEvent = privateEvents.createConsumer("progression.event.summons-data-prepared");


    @BeforeClass
    public static void setUp() {
        HearingStub.stubInitiateHearing();
    }

    @AfterClass
    public static void tearDown() throws JMSException {
        messageProducerClientPublic.close();
        messageConsumerProsecutionCaseDefendantListingStatusChanged.close();
        messageConsumerProgressionHearingExtendedEvent.close();
        messageConsumerProgressionSummonsDataPreparedEvent.close();
    }

    @Test
    public void shouldListCourtHearing() throws Exception {
        final String applicationId = randomUUID().toString();
        final String hearingId = randomUUID().toString();
        extendHearing(applicationId, hearingId, "progression.command.extend-hearing.json");
        verifyPostListCourtHearing(applicationId);
    }

    @Test
    public void shouldExtendHearingForProsecutionCases() throws Exception {
        final List<String> caseIds = new ArrayList<>();
        final List<String> defendantIds = new ArrayList<>();

        // Allocated hearing Id
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String courtCentreId = randomUUID().toString();
        final String userId = randomUUID().toString();
        caseIds.add(caseId);
        defendantIds.add(defendantId);

        doReferCaseToCourtAndVerify(caseId, defendantId);
        final String extendedHearingId = doVerifyProsecutionCaseDefendantListingStatusChanged();
        LOGGER.info("*** Extended Hearing : {}  | caseId : {}  |  defendant id : {}", extendedHearingId, caseId, defendantId );

        doHearingConfirmedAndVerify(extendedHearingId,caseId,defendantId,courtCentreId,userId);

        // UnAllocated hearing Id
        final String caseId1 = randomUUID().toString();
        final String defendantId1 = randomUUID().toString();
        final String courtCentreId1 = randomUUID().toString();
        final String userId1 = randomUUID().toString();
        caseIds.add(caseId1);
        defendantIds.add(defendantId1);

        doReferCaseToCourtAndVerify(caseId1,defendantId1);

        final String existingHearingId = doVerifyProsecutionCaseDefendantListingStatusChanged();
        LOGGER.info("*** Existing Hearing : {}  | caseId : {}  |  defendant id : {}", existingHearingId, caseId1, defendantId1 );

        // Extending hearing
        doHearingConfirmedAndVerify(existingHearingId,caseId1,defendantId1,courtCentreId1,userId1, extendedHearingId);

        doVerifyProgressionHearingExtendedEvent(extendedHearingId, caseId1);
        queryAndVerifyHearingIsExtended(extendedHearingId);

        doVerifyProgressionSummonsDataPreparedEvent(caseIds, defendantIds);
        doVerifyProgressionSummonsDataPreparedEvent(caseIds, defendantIds);

    }


    private void doReferCaseToCourtAndVerify(final String caseId, final String defendantId) throws IOException {

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

    }

    private void doHearingConfirmedAndVerify(String hearingId, String caseId, String defendantId, String courtCentreId, String userId) {

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = stringToJsonObjectConverter.convert(
                getPayload(PUBLIC_LISTING_HEARING_CONFIRMED_FILE)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId));

        sendMessage(messageProducerClientPublic,
                PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, metadata);

    }


    private void doHearingConfirmedAndVerify(String hearingId, String caseId, String defendantId, String courtCentreId, String userId, String extendedHearingId) {

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = stringToJsonObjectConverter.convert(
                getPayload(PUBLIC_LISTING_HEARING_CONFIRMED_WITH_EXTENDED_HEARING_ID)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("EXTENDED_ID", extendedHearingId));

         sendMessage(messageProducerClientPublic,
                PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, metadata);

    }

    private String doVerifyProsecutionCaseDefendantListingStatusChanged(){
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        final JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        final String resultHearingId = prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
        getHearingForDefendant(resultHearingId, new Matcher[0]);
        LOGGER.info("Hearing Id for defendant listing status changed : {} ", resultHearingId);
        return resultHearingId;
 }

  private void doVerifyProgressionHearingExtendedEvent(final String allocatedHearingId, final String caseId){
     final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerProgressionHearingExtendedEvent);
     final JsonObject extendHearingCommand = message.get();

     assertThat(extendHearingCommand.getJsonObject("hearingRequest").getString("id"),  equalTo(allocatedHearingId));
     assertThat(extendHearingCommand.getJsonObject("hearingRequest")
             .getJsonArray("prosecutionCases").getJsonObject(0).getString("id"),  is(caseId));
 }

    private void doVerifyProgressionSummonsDataPreparedEvent(final List<String> caseIds, final List<String> defendantIds){
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerProgressionSummonsDataPreparedEvent);
        final JsonObject extendHearingCommand = message.get();

        assertThat(caseIds, hasItem(extendHearingCommand.getJsonObject("summonsData").getJsonArray("confirmedProsecutionCaseIds")
                .getJsonObject(0).getString("id")));
        assertThat(defendantIds, hasItem(extendHearingCommand.getJsonObject("summonsData").getJsonArray("confirmedProsecutionCaseIds")
                .getJsonObject(0).getJsonArray("confirmedDefendantIds").getString(0)));

        assertThat(caseIds, hasItem(extendHearingCommand.getJsonObject("summonsData").getJsonArray("listDefendantRequests")
                .getJsonObject(0).getString("prosecutionCaseId")));

        assertThat(defendantIds, hasItem(extendHearingCommand.getJsonObject("summonsData").getJsonArray("listDefendantRequests")
                .getJsonObject(0).getJsonObject("referralReason").getString("defendantId")));
    }


    private void queryAndVerifyHearingIsExtended(final String allocatedHearingId){
        final Matcher[] hearingMatchers = {
                withJsonPath("$", notNullValue()),
                withJsonPath("$.hearing.id", is(allocatedHearingId))
        };

       final String dbHearing =  pollForResponse("/hearingSearch/" + allocatedHearingId, PROGRESSION_QUERY_HEARING_JSON, hearingMatchers);
        final JsonObject hearingExtendedJsonObject = stringToJsonObjectConverter.convert(dbHearing);
        assertThat(hearingExtendedJsonObject.getJsonObject("hearing")
                .getJsonArray("prosecutionCases").size(),  is(2));
    }


}
