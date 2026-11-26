package uk.gov.moj.cpp.progression.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.lang.String.format;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.OK;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import java.util.List;
import java.util.stream.Collectors;

import io.restassured.path.json.JsonPath;

public class CourtSchedulerServiceStub {

    private static final String COURT_SCHEDULER_ENDPOINT = "/listingcourtscheduler-api/rest/courtscheduler";
    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");

    private static final String UNCONFIRMED_BOOKING = "/unconfirmedBooking";
    private static final String RELEASE_UNCONFIRMED_BOOKINGS = UNCONFIRMED_BOOKING + "/release";
    private static final String STUB_DATA_UNCONFIRMED_BOOKINGS_ON_ONE_SESSION_JSON = "stub-data/unconfirmedBookingsOnOneSession.json";
    public static final String COURTSCHEDULER_GET_UNCONFIRMED_BOOKING_TYPE = "application/vnd.courtscheduler.get.unconfirmed.booking+json";
    public static final String STUB_DATA_PROVISIONAL_BOOKED_SLOTS_FOR_EXISTING_BOOKING_ID_JSON = "stub-data/provisionalBookedSlotsForExistingBookingId.json";

    static {
        configureFor(HOST, 8080);
    }

    public static void stubGetProvisionalBookedSlotsForNonExistingBookingId() {
        stubFor(get(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + UNCONFIRMED_BOOKING)))
                .withQueryParam("bookingIds", notMatching("null"))
                .withHeader("Accept", containing(COURTSCHEDULER_GET_UNCONFIRMED_BOOKING_TYPE))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody("{\"provisionalSlots\": []}")
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static void stubGetProvisionalBookedSlotsForExistingBookingId() {
        stubFor(get(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + UNCONFIRMED_BOOKING)))
                .withQueryParam("bookingIds", notMatching("null"))
                .withHeader("Accept", containing(COURTSCHEDULER_GET_UNCONFIRMED_BOOKING_TYPE))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(getPayload(STUB_DATA_PROVISIONAL_BOOKED_SLOTS_FOR_EXISTING_BOOKING_ID_JSON))
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    /**
     * Two bookings the clerk reserved on one session, as courtscheduler answers a lookup naming
     * both. The share merges their next hearings into one listing need, leaving one booking surplus.
     */
    public static void stubUnconfirmedBookingsOnOneSession(final String courtScheduleId, final String bookingA, final String bookingB) {
        stubFor(get(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + UNCONFIRMED_BOOKING)))
                .withQueryParam("bookingIds", containing(bookingA))
                .withHeader("Accept", containing(COURTSCHEDULER_GET_UNCONFIRMED_BOOKING_TYPE))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(getPayload(STUB_DATA_UNCONFIRMED_BOOKINGS_ON_ONE_SESSION_JSON)
                                .replaceAll("COURT_SCHEDULE_ID", courtScheduleId)
                                .replaceAll("BOOKING_A", bookingA)
                                .replaceAll("BOOKING_B", bookingB))
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    /** courtscheduler's bulk release of unconfirmed holds, answering 202 with no body as it does. */
    public static void stubReleaseUnconfirmedBookings() {
        stubFor(post(urlPathEqualTo(COURT_SCHEDULER_ENDPOINT + RELEASE_UNCONFIRMED_BOOKINGS))
                .willReturn(aResponse().withStatus(ACCEPTED.getStatusCode())));
    }

    /** The bookingIds of every bulk release sent with reason SURPLUS, flattened. */
    public static List<String> surplusReleasedBookingIds() {
        return findAll(postRequestedFor(urlPathEqualTo(COURT_SCHEDULER_ENDPOINT + RELEASE_UNCONFIRMED_BOOKINGS))
                .withRequestBody(containing("\"reason\":\"SURPLUS\""))).stream()
                .flatMap(request -> JsonPath.from(request.getBodyAsString()).<String>getList("bookingIds").stream())
                .collect(Collectors.toList());
    }
}
