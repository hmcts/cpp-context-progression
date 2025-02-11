package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.io.Resources.getResource;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.CaseHearingsQueryHelper.pollForHearing;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithDefendantAsAdult;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.createHttpHeaders;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollHearingWithStatus;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.stub.ProbationCaseworkerStub.verifyProbationHearingCommandInvoked;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.rest.RestClient;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.google.common.io.Resources;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ACourtHearingDaysIT extends AbstractIT {

    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private static final String MEDIA_TYPE_CORRECT_HEARING_DAYS_WITHOUT_COURT_CENTRE = "application/vnd.progression.correct-hearing-days-without-court-centre+json";
    private static final String PUBLIC_LISTING_HEARING_DAYS_WITHOUT_COURT_CENTRE_CORRECTED = "public.events.listing.hearing-days-without-court-centre-corrected";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private static final RestClient restClient = new RestClient();
    private static final DateTimeFormatter ZONE_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private String courtCentreId;
    private String courtRoomId;
    private Integer listedDurationMinutes;

    private Integer listingSequence;

    @BeforeEach
    public void setUp() {
        courtCentreId = fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        courtRoomId = randomUUID().toString();
        listedDurationMinutes = 20;
        listingSequence = 0;
    }

    @Test
    public void shouldCorrectHearingDaysWithCourtCentre() throws IOException, JSONException {
        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String reportingRestrictionOrderedDate = LocalDate.now().toString();

        addProsecutionCaseToCrownCourt(caseId, defendantId, reportingRestrictionOrderedDate);
        final String hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject payload = createObjectBuilder()
                .add("hearingDays", createArrayBuilder().add(populateCorrectedHearingDays()))
                .add("id", hearingId).build();

        try (Response response = restClient.postCommand(getWriteUrl("/correct-hearing-days-without-court-centre"),
                MEDIA_TYPE_CORRECT_HEARING_DAYS_WITHOUT_COURT_CENTRE,
                payload.toString(), createHttpHeaders(userId))) {

            assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
        }

        pollForHearing(hearingId,
                withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearing.hearingDays[0].courtCentreId", is(courtCentreId)),
                withJsonPath("$.hearing.hearingDays[0].courtRoomId", is(courtRoomId))
        );
    }

    @Test
    public void shouldCorrectHearingDaysWhenRaisePublicEvent() throws IOException, JSONException {
        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        addProsecutionCaseToCrownCourtWithDefendantAsAdult(caseId, defendantId);
        final String hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, "Lavender Hill Magistrate's Court");

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        pollHearingWithStatus(hearingId, "HEARING_INITIALISED");

        final JsonObject payload = createObjectBuilder()
                .add("hearingDays", createArrayBuilder().add(populateCorrectedHearingDays()))
                .add("id", hearingId).build();

        final JsonEnvelope publicEventCorrectedEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_DAYS_WITHOUT_COURT_CENTRE_CORRECTED, userId), payload);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_DAYS_WITHOUT_COURT_CENTRE_CORRECTED, publicEventCorrectedEnvelope);

        final Matcher[] hearingDaysMatchers = {
                withJsonPath("$.hearing.id", CoreMatchers.is(hearingId)),
                withJsonPath("$.hearing.hearingDays", hasSize(4)),
                withJsonPath("$.hearing.hearingDays[0].courtCentreId", is(courtCentreId)),
                withJsonPath("$.hearing.hearingDays[0].courtRoomId", is(courtRoomId)),
                withJsonPath("$.hearing.hearingDays[0].listedDurationMinutes", is(0)),
                withJsonPath("$.hearing.hearingDays[0].sittingDay", is("2018-09-28T12:13:00.000Z")),
                withJsonPath("$.hearing.hearingDays[0].hasSharedResults", is(true))
        };

        pollForHearing(hearingId, hearingDaysMatchers);

        verifyProbationHearingCommandInvoked(newArrayList(hearingId, courtCentreId, courtRoomId, "2018-09-28T12:13:00.000Z"));
    }

    public static io.restassured.response.Response addProsecutionCaseToCrownCourt(final String caseId, final String defendantId, final String reportingRestrictionOrderedDate) throws IOException, JSONException {
        return addProsecutionCaseToCrownCourt(caseId, defendantId, generateUrn(), reportingRestrictionOrderedDate);
    }

    public static io.restassured.response.Response addProsecutionCaseToCrownCourt(final String caseId, final String defendantId, final String caseUrn, final String reportingRestrictionOrderedDate) throws IOException, JSONException {
        final JSONObject jsonPayload = new JSONObject(createReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId, randomUUID().toString(),
                randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), caseUrn, reportingRestrictionOrderedDate));
        jsonPayload.getJSONObject("courtReferral").remove("courtDocuments");
        return postCommand(getWriteUrl("/refertocourt"),
                "application/vnd.progression.refer-cases-to-court+json",
                jsonPayload.toString());
    }

    private static String createReferProsecutionCaseToCrownCourtJsonBody(final String caseId, final String defendantId, final String materialIdOne,
                                                                         final String materialIdTwo, final String courtDocumentId, final String referralId,
                                                                         final String caseUrn, final String reportingRestrictionOrderedDate) throws IOException {
        return createReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId, materialIdOne,
                materialIdTwo, courtDocumentId, referralId, caseUrn, reportingRestrictionOrderedDate, "progression.command.prosecution-case-refer-to-court.json");
    }

    public static String createReferProsecutionCaseToCrownCourtJsonBody(final String caseId, final String defendantId, final String materialIdOne,
                                                                        final String materialIdTwo, final String courtDocumentId, final String referralId,
                                                                        final String caseUrn, final String reportingRestrictionOrderedDate, final String filePath) throws IOException {
        final URL resource = getResource(filePath);
        return Resources.toString(resource, Charset.defaultCharset())
                .replace("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_REFERENCE", caseUrn)
                .replaceAll("RANDOM_DEFENDANT_ID", defendantId)
                .replace("RANDOM_DOC_ID", courtDocumentId)
                .replace("RANDOM_MATERIAL_ID_ONE", materialIdOne)
                .replace("RANDOM_MATERIAL_ID_TWO", materialIdTwo)
                .replace("RANDOM_REFERRAL_ID", referralId)
                .replace("RR_ORDERED_DATE", reportingRestrictionOrderedDate);
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
                .add("sittingDay", ZONE_DATETIME_FORMATTER.format(ZonedDateTime.now()))
                .add("hasSharedResults", true);

    }
}
