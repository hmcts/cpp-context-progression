package uk.gov.moj.cpp.progression.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.lang.String.format;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;

public class AzureScheduleServiceStub {

    private static final String ROTA_SL_ENDPOINT_URL = "/fa-ste-ccm-scsl";
    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");

    private static final String PROVISIONAL_BOOKING = "/provisionalBooking";

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

}
