package uk.gov.moj.cpp.progression.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static org.awaitility.Awaitility.await;

import java.util.List;

import com.github.tomakehurst.wiremock.client.CountMatchingStrategy;
import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;

public class AmpPcrServiceStub {

    private static final String AMP_PCR_API_ENDPOINT_URL = "/AMP/pcrEvent";
    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");

    static {
        configureFor(HOST, 8080);
    }

    public static void stubPostAmpPcrEvent() {
        stubFor(post(urlMatching(AMP_PCR_API_ENDPOINT_URL))
                .willReturn(aResponse()
                        .withStatus(OK.getStatusCode())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));
    }

    public static void verifyAmpPcrEventInvoked(int count) {
        verifyAmpPcrEventInvoked(exactly(count), null);
    }

    private static void verifyAmpPcrEventInvoked(CountMatchingStrategy countStrategy, List<String> expectedValues) {
        await().atMost(30, SECONDS)
                .pollInterval(500, MILLISECONDS)
                .until(() -> {
                    try {
                        RequestPatternBuilder request = postRequestedFor(urlMatching(AMP_PCR_API_ENDPOINT_URL));
                        if (expectedValues != null) {
                            expectedValues.forEach(v -> request.withRequestBody(containing(v)));
                        }
                        verify(countStrategy, request);
                        return true;
                    } catch (VerificationException e) {
                        return false;
                    }
                });
    }
}
