package uk.gov.moj.cpp.progression.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static org.awaitility.Awaitility.await;
import static uk.gov.moj.cpp.progression.helper.RestHelper.INITIAL_INTERVAL_IN_MILLISECONDS;
import static uk.gov.moj.cpp.progression.helper.RestHelper.INTERVAL_IN_MILLISECONDS;

import java.time.Duration;
import java.util.List;

import com.github.tomakehurst.wiremock.client.CountMatchingStrategy;
import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.test.utils.core.http.FibonacciPollWithStartAndMax;

public class LaaAPIMServiceStub {
    private static final Logger LOGGER = LoggerFactory.getLogger(LaaAPIMServiceStub.class);
    private static final String LAA_API_ENDPOINT_URL = "/LAA/v1/caseOutcome/conclude";
    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");

    static {
        configureFor(HOST, 8080);
    }

    public static void stubPostLaaAPI() {
        stubFor(post(urlPathMatching(format("%s", LAA_API_ENDPOINT_URL)))
                .withRequestBody(containing("prosecutionConcluded"))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                ));
    }

    public static void verifyLaaProceedingsConcludedCommandInvoked(final int count, final List<String> expectedValues) {
        verifyLaaProceedingsConcludedCommandInvoked(LAA_API_ENDPOINT_URL, exactly(count), expectedValues);
    }

    private static void verifyLaaProceedingsConcludedCommandInvoked(final String commandEndPoint, final CountMatchingStrategy countMatchingStrategy, final List<String> expectedValues) {
        await().atMost(30, SECONDS).pollInterval(new FibonacciPollWithStartAndMax(Duration.ofMillis(INITIAL_INTERVAL_IN_MILLISECONDS), Duration.ofMillis(INTERVAL_IN_MILLISECONDS))).until(() -> {
            final RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlPathMatching(commandEndPoint));
            expectedValues.forEach(
                    expectedValue -> requestPatternBuilder.withRequestBody(containing(expectedValue))
            );
            try {
                verify(countMatchingStrategy, requestPatternBuilder);
            } catch (VerificationException e) {
                LOGGER.error(e.getMessage());
                return false;
            }
            return true;
        });
    }

}
