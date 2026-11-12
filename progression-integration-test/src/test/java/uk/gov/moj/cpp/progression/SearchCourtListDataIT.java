package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.intiateCourtProceedingForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupListingCourtListApplicationPayloadStub;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupListingCourtListPayloadStub;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupListingQueryStub;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupReferenceDataQueryCourtCenterDataByCourtNameStub;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;

import javax.json.JsonObject;

import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for GET /courtlistdata (court list and prison court list data as JSON).
 */
public class SearchCourtListDataIT extends AbstractIT {

    private static final String COURT_CENTRE_ID = "f8254db1-1683-483e-afb3-b87fde5a0a26";
    private static final String PNC_ID = "1234567";
    private static final String ASN = "arrest123";
    private static final String PUBLIC_HEARING_RESULTED = "public.hearing.resulted";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PRISON_COURT_LIST_DATA_MEDIA_TYPE = "application/vnd.progression.search.prison.court.list.data+json";

    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @BeforeAll
    public static void setUpClass() {
        setupReferenceDataQueryCourtCenterDataByCourtNameStub();
    }

    /**
     * Some tests override the shared listing court list payload stub with a test-specific payload, so restore the
     * default before every test to keep results independent of test execution order.
     */
    @BeforeEach
    public void resetListingCourtListPayloadStub() {
        setupListingQueryStub();
    }

    @Test
    public void shouldReturnCourtListDataAsJson() {
        final String responseBody = pollForResponse(
                "/courtlistdata?listId=STANDARD&courtCentreId=f8254db1-1683-483e-afb3-b87fde5a0a26&startDate=2022-07-12&endDate=2022-07-12&_=bc9153c0-8278-494e-8f72-d63973bab35f",
                "application/vnd.progression.search.court.list.data+json");

        final JsonObject json = getJsonObject(responseBody);
        assertThat(json.getString("listType"), equalTo("public"));
        assertThat(json.containsKey("courtCentreName"), equalTo(true));
        assertThat(json.containsKey("hearingDates"), equalTo(true));
    }

    @Test
    public void shouldReturnPrisonCourtListDataAsJson() {
        final String responseBody = pollForResponse(
                "/courtlistdata?listId=PRISON&courtCentreId=f8254db1-1683-483e-afb3-b87fde5a0a26&startDate=2022-07-12&endDate=2022-07-12&_=bc9153c0-8278-494e-8f72-d63973bab35f",
                "application/vnd.progression.search.prison.court.list.data+json");

        final JsonObject json = getJsonObject(responseBody);
        assertThat(json.containsKey("listType"), equalTo(true));
        assertThat(json.containsKey("courtCentreName"), equalTo(true));
        assertThat(json.containsKey("hearingDates"), equalTo(true));
    }

    /**
     * The court list skeleton (defendant identity) is supplied by cpp-context-listing, but ASN and pncId are
     * enriched onto each matched defendant from progression's own prosecution-case viewstore. Enrichment is gated on
     * the hearing being present in progression's hearing viewstore, which is created as soon as the hearing is
     * initialised (HEARING_INITIALISED) by the refer-to-court flow - it does not require the hearing to be confirmed.
     * This test seeds a prosecution case (whose defendant carries arrestSummonsNumber=arrest123 and pncId=1234567),
     * points the listing court list payload stub at the seeded hearing/case/defendant, then asserts both fields are
     * present on the enriched defendant in the /courtlistdata response.
     */
    @Test
    public void shouldReturnArrestSummonsNumberAndPncIdForEnrichedDefendant() throws Exception {
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();

        // 1) Seed a prosecution case in progression (refer-to-court defendant has ASN=arrest123, pncId=1234567).
        //    The initiate flow creates the hearing in progression's hearing viewstore (status HEARING_INITIALISED).
        addProsecutionCaseToCrownCourt(caseId, defendantId);

        // 2) Wait for the case to be processed and capture the initiated hearing for the defendant.
        final String hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        // 3) Point the listing court list payload stub at the seeded hearing/case/defendant.
        setupListingCourtListPayloadStub(hearingId, caseId, defendantId);

        // 4) Query court list data, polling until the defendant is enriched with the pncId, then assert ASN + pncId.
        final String responseBody = pollForResponse(
                "/courtlistdata?listId=STANDARD&courtCentreId=" + COURT_CENTRE_ID + "&startDate=2022-07-12&endDate=2022-07-12&_=" + randomUUID(),
                "application/vnd.progression.search.court.list.data+json",
                withJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[0].defendants[?(@.id=='" + defendantId + "')].pncId", hasItem("1234567")));

        final JsonObject defendant = getJsonObject(responseBody)
                .getJsonArray("hearingDates").getJsonObject(0)
                .getJsonArray("courtRooms").getJsonObject(0)
                .getJsonArray("timeslots").getJsonObject(0)
                .getJsonArray("hearings").getJsonObject(0)
                .getJsonArray("defendants").getJsonObject(0);
        assertThat(defendant.getString("asn"), equalTo("arrest123"));
        assertThat(defendant.getString("pncId"), equalTo("1234567"));
    }

    /**
     * LPT-2435: case with a PNC ID is resulted and becomes INACTIVE, then a reopen-case application (MC80524) is
     * raised against it. The application's subject master defendant carries no pncId or ASN (as in production), so
     * the prison court list must fall back to the PNC ID and ASN held on the linked case defendant.
     */
    @Test
    public void shouldReturnPncIdForReopenCaseApplicationHearingOnPrisonCourtListData() throws Exception {
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String applicationId = randomUUID().toString();
        final String userId = randomUUID().toString();

        // 1) Case with a defendant carrying pncId=1234567 and ASN=arrest123, and its initial hearing.
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        final String hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        // 2) Result the case so it becomes INACTIVE (resulted defendant keeps its PNC ID and ASN, as it does in production).
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED, envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED, userId),
                resultedCasePayload(caseId, hearingId, defendantId)));
        pollProsecutionCasesProgressionFor(caseId,
                withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath("$.prosecutionCase.caseStatus", equalTo("INACTIVE")));

        // 3) Reopen-case application whose subject master defendant has no pncId and no ASN.
        final Response initiateResponse = intiateCourtProceedingForApplication(applicationId, caseId, defendantId, defendantId, hearingId,
                "applications/progression.initiate-court-proceedings-for-reopen-case-application-without-pnc.json");
        assertThat(initiateResponse.getStatusCode(), equalTo(HTTP_ACCEPTED));
        pollForApplication(applicationId);

        // 4) Listing confirms the reopen hearing with the application on it.
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId),
                stringToJsonObjectConverter.convert(getPayload("public.listing.hearing-confirmed-case-reopen.json")
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("COURT_CENTRE_ID", COURT_CENTRE_ID)
                        .replaceAll("COURT_CENTRE_NAME", "Lavender Hill Magistrate's Court")
                        .replaceAll("APPLICATION_ID", applicationId)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("DEFENDANT_ID", defendantId))));

        // 5) Prison list from listing references the application hearing; pncId and ASN must be enriched from the linked case.
        setupListingCourtListApplicationPayloadStub(hearingId, applicationId, defendantId);

        final String responseBody = pollForResponse(
                "/courtlistdata?listId=PRISON&courtCentreId=" + COURT_CENTRE_ID + "&startDate=2022-07-12&endDate=2022-07-12&_=" + randomUUID(),
                PRISON_COURT_LIST_DATA_MEDIA_TYPE,
                withJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[0].defendants[0].pncId", equalTo(PNC_ID)),
                withJsonPath("$.hearingDates[0].courtRooms[0].timeslots[0].hearings[0].defendants[0].asn", equalTo(ASN)));

        final JsonObject hearing = getJsonObject(responseBody)
                .getJsonArray("hearingDates").getJsonObject(0)
                .getJsonArray("courtRooms").getJsonObject(0)
                .getJsonArray("timeslots").getJsonObject(0)
                .getJsonArray("hearings").getJsonObject(0);
        assertThat(hearing.getJsonObject("courtApplication").getString("applicationType"), equalTo("Application to reopen case"));
        final JsonObject defendant = hearing.getJsonArray("defendants").getJsonObject(0);
        assertThat(defendant.getString("pncId"), equalTo(PNC_ID));
        assertThat(defendant.getString("asn"), equalTo(ASN));
    }

    private JsonObject resultedCasePayload(final String caseId, final String hearingId, final String defendantId) {
        return stringToJsonObjectConverter.convert(getPayload("public.hearing.resulted-case-updated.json")
                .replace("\"masterDefendantId\": \"DEFENDANT_ID\",", "\"masterDefendantId\": \"DEFENDANT_ID\",\n            \"pncId\": \"" + PNC_ID + "\",")
                .replace("\"arrestSummonsNumber\": \"a\"", "\"arrestSummonsNumber\": \"" + ASN + "\"")
                .replaceAll("CASE_ID", caseId)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replace("COURT_CENTRE_ID", COURT_CENTRE_ID)
                .replace("BAIL_STATUS_ID", "2593cf09-ace0-4b7d-a746-0703a29f33b5")
                .replace("BAIL_STATUS_CODE", "C")
                .replace("BAIL_STATUS_DESCRIPTION", "Remedy"));
    }
}
