package uk.gov.moj.cpp.progression.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;

public class AzureScheduleServiceStub {

    private static final String ROTA_SL_ENDPOINT_URL = "/fa-ste-ccm-scsl";
    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");

    private static final String PROVISIONAL_BOOKING = "/provisionalBooking";

    private static final String PROVISIONAL_SCHEDULE_ID = "fbed768b-ee95-4434-87c8-e81cbc8d24c7";
    private static final String PROVISIONAL_SESSION = "AM";
    private static final Integer PROVISIONAL_COURT_ROOM = 178498;
    private static final Integer PROVISIONAL_COURT_HOUSE_ID = 178498;
    private static final Integer PROVISIONAL_ROOM_ID = 178498;
    private static final String PROVISIONAL_OUCODE = "WERDFG";
    private static final String PROVISIONAL_START_TIME = "2020-07-01";
    private static final String PROVISIONAL_SESSION_DATE = "2020-07-01";

    static {
        configureFor(HOST, 8080);
    }

    public static void stubGetProvisionalBookedSlotsForNonExistingBookingId() {
        stubFor(get(urlPathMatching(format("%s", ROTA_SL_ENDPOINT_URL + PROVISIONAL_BOOKING)))
                .withQueryParam("bookingIds", notMatching("null"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody("{\"provisionalSlots\": []}")
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static void stubGetProvisionalBookedSlotsForExistingBookingId() {
        stubFor(get(urlPathMatching(format("%s", ROTA_SL_ENDPOINT_URL + PROVISIONAL_BOOKING)))
                .withQueryParam("bookingIds", notMatching("null"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody("{\"provisionalSlots\": [{" +
                                "\"courtScheduleId\": \"" + PROVISIONAL_SCHEDULE_ID + "\",\n" +
                                "      \"ouCode\": \"" + PROVISIONAL_OUCODE + "\",\n" +
                                "      \"courtSession\": \"" + PROVISIONAL_SESSION + "\",\n" +
                                "      \"duration\":0,\n" +
                                "      \"bookingId\": \"" + PROVISIONAL_SCHEDULE_ID + "\",\n" +
                                "      \"courtRoomNumber\": \"" + PROVISIONAL_COURT_ROOM + "\",\n" +
                                "      \"startTime\": \"" + PROVISIONAL_START_TIME + "\",\n" +
                                "      \"sessionDate\": \"" + PROVISIONAL_SESSION_DATE + "\",\n" +
                                "      \"maxSlots\": 0,\n" +
                                "      \"courtHouseId\": \"" + PROVISIONAL_COURT_HOUSE_ID + "\",\n" +
                                "      \"courtRoomId\": \"" + PROVISIONAL_ROOM_ID + "\"}]}")
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

}
