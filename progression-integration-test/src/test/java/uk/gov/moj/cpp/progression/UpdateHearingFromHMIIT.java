package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithDefendantAsAdult;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.stub.HearingStub;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import javax.jms.JMSException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UpdateHearingFromHMIIT extends AbstractIT {


    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_LISTING_HEARING_DAYS_WITHOUT_COURT_CENTRE_CORRECTED = "public.events.listing.hearing-days-without-court-centre-corrected";
    private static final String PROGRESSION_QUERY_HEARING_JSON = "application/vnd.progression.query.hearing+json";
    private static final DateTimeFormatter ZONE_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    public static final String PUBLIC_STAGINGHMI_HEARING_UPDATED_FROM_HMI = "public.staginghmi.hearing-updated-from-hmi";
    private String courtCentreId;
    private String courtRoomId;
    private Integer listedDurationMinutes;

    private Integer listingSequence;

    @BeforeEach
    public void setUp() {
        HearingStub.stubInitiateHearing();
        courtCentreId = fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        courtRoomId = randomUUID().toString();
        listedDurationMinutes = 20;
        listingSequence = 0;
    }


    @Test
    public void shouldUpdateHearingWhenHmiRemovedCourtRoom() throws IOException, JMSException, JSONException {

        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String hearingId = createHearing(userId, caseId, defendantId);

        final JsonObject publicEvent = createObjectBuilder()
                .add("hearingId", hearingId)
                .add("courtCentreId", courtCentreId)
                .add("startDate", LocalDate.now().toString())
                .build();

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_STAGINGHMI_HEARING_UPDATED_FROM_HMI, userId), publicEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_STAGINGHMI_HEARING_UPDATED_FROM_HMI, publicEventEnvelope);

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
    public void shouldUpdateHearingWhenHmiRemovedHearingDays() throws IOException, JMSException, JSONException {

        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String hearingId = createHearing(userId, caseId, defendantId);

        final JsonObject publicEvent = createObjectBuilder()
                .add("hearingId", hearingId)
                .add("courtCentreId", randomUUID().toString())
                .build();

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_STAGINGHMI_HEARING_UPDATED_FROM_HMI, userId), publicEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_STAGINGHMI_HEARING_UPDATED_FROM_HMI, publicEventEnvelope);

        final Matcher[] unAllocatedMatchers = {
                withJsonPath("$.hearing.id", CoreMatchers.is(hearingId)),
                withoutJsonPath("$.hearing.hearingDays"),
                withJsonPath("$.hearingListingStatus", CoreMatchers.is("SENT_FOR_LISTING")),
                withoutJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].listingNumber")
        };

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON, unAllocatedMatchers);
    }

    private String createHearing(final String userId, final String caseId, final String defendantId) throws IOException, JMSException, JSONException {
        addProsecutionCaseToCrownCourtWithDefendantAsAdult(caseId, defendantId);

        final String hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, "Lavender Hill Magistrate's Court");

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        getHearingForDefendant(hearingId, new Matcher[]{withJsonPath("$.hearingListingStatus", is("HEARING_INITIALISED"))});

        final JsonObject payload = createObjectBuilder()
                .add("hearingDays", createArrayBuilder().add(populateCorrectedHearingDays()))
                .add("id", hearingId).build();

        final JsonEnvelope publicEventCorrectedEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_DAYS_WITHOUT_COURT_CENTRE_CORRECTED, userId), payload);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_DAYS_WITHOUT_COURT_CENTRE_CORRECTED, publicEventCorrectedEnvelope);

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

    private JsonObjectBuilder populateCorrectedHearingDays() {
        return createObjectBuilder()
                .add("listedDurationMinutes", listedDurationMinutes)
                .add("listingSequence", listingSequence)
                .add("courtCentreId", courtCentreId)
                .add("courtRoomId", courtRoomId)
                .add("sittingDay", ZONE_DATETIME_FORMATTER.format(ZonedDateTime.now()));
    }
}
