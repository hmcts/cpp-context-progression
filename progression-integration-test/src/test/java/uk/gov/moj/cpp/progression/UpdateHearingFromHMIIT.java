package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithDefendantAsAdult;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;


import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.HearingStub;

public class UpdateHearingFromHMIIT extends AbstractIT {

    private static final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents.createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed");
    private static final MessageConsumer messageConsumerProsecutionHearingMovedtoUnAllocated = privateEvents.createPrivateConsumer("progression.event.hearing-moved-to-unallocated");
    private static final MessageProducer messageProducerClientPublic = publicEvents.createPublicProducer();

    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_LISTING_HEARING_DAYS_WITHOUT_COURT_CENTRE_CORRECTED = "public.events.listing.hearing-days-without-court-centre-corrected";
    private static final String PROGRESSION_QUERY_HEARING_JSON = "application/vnd.progression.query.hearing+json";
    private static final DateTimeFormatter ZONE_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private String courtCentreId;
    private String courtRoomId;
    private Integer listedDurationMinutes;

    private Integer listingSequence;

    @Before
    public void setUp() {
        HearingStub.stubInitiateHearing();
        courtCentreId = fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        courtRoomId = randomUUID().toString();
        listedDurationMinutes = 20;
        listingSequence = 0;
    }

    @AfterClass
    public static void tearDown() throws JMSException {
        messageProducerClientPublic.close();
        messageConsumerProsecutionCaseDefendantListingStatusChanged.close();
        messageConsumerProsecutionHearingMovedtoUnAllocated.close();
    }


    @Test
    public void shouldUpdateHearingWhenHmiRemovedCourtRoom() throws IOException, JMSException {

        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String hearingId = createHearing(userId, caseId, defendantId);
        Metadata metadata;

        metadata = metadataBuilder()
                .withId(randomUUID())
                .withName("public.staginghmi.hearing-updated-from-hmi")
                .withUserId(userId)
                .build();

        final JsonObject publicEvent = Json.createObjectBuilder()
                .add("hearingId", hearingId)
                .add("courtCentreId", courtCentreId)
                .add("startDate", LocalDate.now().toString())
                .build();

        sendMessage(messageProducerClientPublic,
                "public.staginghmi.hearing-updated-from-hmi", publicEvent, metadata);


        final Matcher[] unAllocatedMatchers = {
                withJsonPath("$.hearing.id", CoreMatchers.is(hearingId)),
                withJsonPath("$.hearing.hearingDays", hasSize(4)),
                withJsonPath("$.hearing.hearingDays[0].courtCentreId", CoreMatchers.is(courtCentreId)),
                withoutJsonPath("$.hearing.hearingDays[0].courtRoomId"),
                withoutJsonPath("$.hearing.hearingDays[1].courtRoomId"),
                withoutJsonPath("$.hearing.hearingDays[2].courtRoomId"),
                withoutJsonPath("$.hearing.hearingDays[3].courtRoomId"),
                withJsonPath("$.hearing.hearingDays[0].listedDurationMinutes", CoreMatchers.is(0)),
                withJsonPath("$.hearing.hearingDays[0].sittingDay", CoreMatchers.is("2018-09-28T12:13:00.000Z")),
                withJsonPath("$.hearingListingStatus", CoreMatchers.is("SENT_FOR_LISTING")),
                withoutJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].listingNumber")
        };

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON, unAllocatedMatchers);

    }

    @Test
    public void shouldUpdateHearingWhenHmiRemovedHearingDays() throws IOException, JMSException {

        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String hearingId = createHearing(userId, caseId, defendantId);
        Metadata metadata;

        metadata = metadataBuilder()
                .withId(randomUUID())
                .withName("public.staginghmi.hearing-updated-from-hmi")
                .withUserId(userId)
                .build();

        final JsonObject publicEvent = Json.createObjectBuilder()
                .add("hearingId", hearingId)
                .add("courtCentreId", randomUUID().toString())
                .build();

        sendMessage(messageProducerClientPublic,
                "public.staginghmi.hearing-updated-from-hmi", publicEvent, metadata);


        final Matcher[] unAllocatedMatchers = {
                withJsonPath("$.hearing.id", CoreMatchers.is(hearingId)),
                withoutJsonPath("$.hearing.hearingDays"),
                withJsonPath("$.hearingListingStatus", CoreMatchers.is("SENT_FOR_LISTING")),
                withoutJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].listingNumber")
        };

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON, unAllocatedMatchers);

    }

    private String createHearing(final String userId, final String caseId, final String defendantId) throws IOException, JMSException {
        addProsecutionCaseToCrownCourtWithDefendantAsAdult(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceCode", CoreMatchers.is("TTH105HY"))
                )));

        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        final JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        final String hearingId = prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");

        Metadata metadata = JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, "Lavender Hill Magistrate's Court");


        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, metadata);

            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }


        final JsonObject payload = createObjectBuilder()
                .add("hearingDays", createArrayBuilder().add(populateCorrectedHearingDays()))
                .add("id", hearingId).build();

        sendMessage(messageProducerClientPublic, PUBLIC_LISTING_HEARING_DAYS_WITHOUT_COURT_CENTRE_CORRECTED, payload, Envelope.metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_DAYS_WITHOUT_COURT_CENTRE_CORRECTED)
                .withUserId(userId)
                .build());


        final Matcher[] hearingDaysMatchers = {
                withJsonPath("$.hearing.id", CoreMatchers.is(hearingId)),
                withJsonPath("$.hearing.hearingDays", hasSize(4)),
                withJsonPath("$.hearing.hearingDays[0].courtCentreId", CoreMatchers.is(courtCentreId)),
                withJsonPath("$.hearing.hearingDays[0].courtRoomId", Matchers.is(courtRoomId)),
                withJsonPath("$.hearing.hearingDays[0].listedDurationMinutes", CoreMatchers.is(0)),
                withJsonPath("$.hearing.hearingDays[0].sittingDay", CoreMatchers.is("2018-09-28T12:13:00.000Z"))
        };

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON, hearingDaysMatchers);
        return hearingId;
    }

    private JsonObject createHearingAndReturnHearingId(final String prosecutionCaseId, final String defendantId, final String urn) throws IOException {
        addProsecutionCaseToCrownCourt(prosecutionCaseId, defendantId, urn);

        pollProsecutionCasesProgressionFor(prosecutionCaseId, getProsecutionCaseMatchers(prosecutionCaseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.id", is(prosecutionCaseId)))));

        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        return prosecutionCaseDefendantListingStatusChanged;
    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String courtCentreId, final String courtCentreName) {
        return stringToJsonObjectConverter.convert(
                getPayload(path)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("COURT_CENTRE_NAME", courtCentreName)
        );
    }

    private String doVerifyProsecutionCaseDefendantListingStatusChanged(final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged) {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        final JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        return prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
    }

    private JsonObjectBuilder populateCorrectedHearingDays() {
        return createObjectBuilder()
                .add("listedDurationMinutes", listedDurationMinutes)
                .add("listingSequence", listingSequence)
                .add("courtCentreId", courtCentreId)
                .add("courtRoomId", courtRoomId)
                .add("sittingDay", ZONE_DATETIME_FORMATTER.format(ZonedDateTime.now()));
    }
}
