package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupListingCourtListPayloadStub;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupListingQueryStub;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupReferenceDataQueryCourtCenterDataByCourtNameStub;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for GET /courtlistdata (court list and prison court list data as JSON).
 */
public class SearchCourtListDataIT extends AbstractIT {

    private static final String COURT_CENTRE_ID = "f8254db1-1683-483e-afb3-b87fde5a0a26";

    @BeforeAll
    public static void setUpClass() {
        setupListingQueryStub();
        setupReferenceDataQueryCourtCenterDataByCourtNameStub();
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
}
