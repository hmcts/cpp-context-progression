package uk.gov.moj.cpp.progression.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.lang.String.format;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

public class CourtSchedulerServiceStub {

    private static final String COURT_SCHEDULER_ENDPOINT = "/listingcourtscheduler-api/rest/courtscheduler";
    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");

    private static final String PROVISIONAL_BOOKING = "/provisionalBooking";
    public static final String COURTSCHEDULER_GET_PROVISIONAL_BOOKING_TYPE = "application/vnd.courtscheduler.get.provisional.booking+json";
    public static final String STUB_DATA_PROVISIONAL_BOOKED_SLOTS_FOR_EXISTING_BOOKING_ID_JSON = "stub-data/provisionalBookedSlotsForExistingBookingId.json";

    static {
        configureFor(HOST, 8080);
    }

    public static void stubGetProvisionalBookedSlotsForNonExistingBookingId() {
        stubFor(get(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + PROVISIONAL_BOOKING)))
                .withQueryParam("bookingIds", notMatching("null"))
                .withHeader("Accept", containing(COURTSCHEDULER_GET_PROVISIONAL_BOOKING_TYPE))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody("{\"provisionalSlots\": []}")
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static void stubGetProvisionalBookedSlotsForExistingBookingId() {
        stubFor(get(urlPathMatching(format("%s", COURT_SCHEDULER_ENDPOINT + PROVISIONAL_BOOKING)))
                .withQueryParam("bookingIds", notMatching("null"))
                .withHeader("Accept", containing(COURTSCHEDULER_GET_PROVISIONAL_BOOKING_TYPE))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(getPayload(STUB_DATA_PROVISIONAL_BOOKED_SLOTS_FOR_EXISTING_BOOKING_ID_JSON))
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

}
